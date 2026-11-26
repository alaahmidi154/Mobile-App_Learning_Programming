using System.Security.Claims;
using System.Text;
using CodeQuest.Api.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using Microsoft.EntityFrameworkCore;

namespace CodeQuest.Api.Controllers;

public record MissionInput(string? Titre, string? Enonce, string? Difficulte, int Xp,
    string? CodeDepart, string? Indice);

public record TestInput(string? Entree, string? SortieAttendue, bool Visible);

public record RoleInput(string? Role);

/// <summary>
/// The JWT says "admin" for up to 12 h, even after the admin was demoted.
/// This filter re-checks the role in the database on every admin call.
/// </summary>
public class RequireDbAdminAttribute : Attribute, IAsyncActionFilter
{
    public async Task OnActionExecutionAsync(ActionExecutingContext context, ActionExecutionDelegate next)
    {
        var db = context.HttpContext.RequestServices.GetRequiredService<AppDbContext>();
        var idText = context.HttpContext.User.FindFirstValue(ClaimTypes.NameIdentifier);
        var ok = int.TryParse(idText, out var id)
                 && await db.Set<Utilisateur>().AnyAsync(u => u.Id == id && u.Role == "admin");
        if (!ok)
        {
            context.Result = new ObjectResult(new { message = "Accès réservé aux administrateurs." })
            { StatusCode = StatusCodes.Status403Forbidden };
            return;
        }
        await next();
    }
}

/// <summary>Everything the admin screens need. Only the role "admin" gets in (JWT role claim).</summary>
[ApiController]
[Authorize(Roles = "admin")]
[RequireDbAdmin]
[Route("api/admin")]
public class AdminController : ControllerBase
{
    private static readonly string[] Difficulties = { "Débutant", "Intermédiaire", "Avancé" };

    private readonly AppDbContext _db;

    public AdminController(AppDbContext db) => _db = db;

    private int CurrentUserId => int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);

    // ------------------------------------------------------------ stats

    [HttpGet("stats")]
    public async Task<IActionResult> Stats()
    {
        var joueurs = await _db.Set<Utilisateur>().CountAsync(u => u.Role == "joueur");
        var missions = await _db.Set<Mission>().CountAsync();
        var soumissions = await _db.Set<Soumission>().CountAsync();
        var reussies = await _db.Set<Soumission>().CountAsync(s => s.Verdict == "réussi");
        return Ok(new { joueurs, missions, soumissions, reussies });
    }

    // ------------------------------------------------------------ missions

    [HttpGet("missions")]
    public async Task<IActionResult> Missions()
    {
        var list = await _db.Set<Mission>()
            .OrderBy(m => m.Id)
            .Select(m => new
            {
                id = m.Id,
                titre = m.Titre,
                enonce = m.Enonce,
                difficulte = m.Difficulte,
                xp = m.Xp,
                codeDepart = m.CodeDepart,
                indice = m.Indice,
                nbTests = m.Tests.Count
            })
            .ToListAsync();
        return Ok(list);
    }

    [HttpPost("missions")]
    public async Task<IActionResult> CreateMission(MissionInput input)
    {
        var error = Validate(input);
        if (error != null) return BadRequest(new { message = error });

        var m = new Mission();
        Apply(m, input);
        _db.Set<Mission>().Add(m);
        await _db.SaveChangesAsync();
        return Ok(new { ok = true, id = m.Id });
    }

    [HttpPut("missions/{id:int}")]
    public async Task<IActionResult> UpdateMission(int id, MissionInput input)
    {
        var m = await _db.Set<Mission>().FirstOrDefaultAsync(x => x.Id == id);
        if (m == null) return NotFound(new { message = "Mission introuvable." });
        var error = Validate(input);
        if (error != null) return BadRequest(new { message = error });

        Apply(m, input);
        await _db.SaveChangesAsync();
        return Ok(new { ok = true, id = m.Id });
    }

    [HttpDelete("missions/{id:int}")]
    public async Task<IActionResult> DeleteMission(int id)
    {
        var m = await _db.Set<Mission>().FirstOrDefaultAsync(x => x.Id == id);
        if (m == null) return NotFound(new { message = "Mission introuvable." });

        // Soumission has no ON DELETE CASCADE: remove the submissions first.
        // (The tests of the mission are removed by the database: ON DELETE CASCADE.)
        var subs = _db.Set<Soumission>().Where(s => s.IdMission == id);
        _db.Set<Soumission>().RemoveRange(subs);
        _db.Set<Mission>().Remove(m);
        await _db.SaveChangesAsync();
        return Ok(new { ok = true });
    }

    private static string? Validate(MissionInput i)
    {
        if (string.IsNullOrWhiteSpace(i.Titre)) return "Le titre est obligatoire.";
        if (i.Titre.Trim().Length > 150) return "Titre trop long (150 caractères max).";
        if (string.IsNullOrWhiteSpace(i.Enonce)) return "L'énoncé est obligatoire.";
        if (Canonical(i.Difficulte) == null) return "Difficulté : Débutant, Intermédiaire ou Avancé.";
        if (i.Xp < 1 || i.Xp > 1000) return "Les XP doivent être entre 1 et 1000.";
        return null;
    }

    private static string? Canonical(string? d)
    {
        if (d == null) return null;
        var n = d.Trim().Normalize(NormalizationForm.FormC);
        return Difficulties.FirstOrDefault(x => x.Normalize(NormalizationForm.FormC) == n);
    }

    private static void Apply(Mission m, MissionInput i)
    {
        m.Titre = i.Titre!.Trim();
        m.Enonce = i.Enonce!.Trim();
        m.Difficulte = Canonical(i.Difficulte)!;
        m.Xp = i.Xp;
        m.CodeDepart = i.CodeDepart ?? "";
        m.Indice = string.IsNullOrWhiteSpace(i.Indice) ? null : i.Indice.Trim();
    }

    // ------------------------------------------------------------ tests

    [HttpGet("missions/{id:int}/tests")]
    public async Task<IActionResult> Tests(int id)
    {
        var list = await _db.Set<Test>()
            .Where(t => t.IdMission == id)
            .OrderBy(t => t.Id)
            .Select(t => new { id = t.Id, entree = t.Entree, sortieAttendue = t.SortieAttendue, visible = t.Visible })
            .ToListAsync();
        return Ok(list);
    }

    [HttpPost("missions/{id:int}/tests")]
    public async Task<IActionResult> AddTest(int id, TestInput input)
    {
        if (!await _db.Set<Mission>().AnyAsync(m => m.Id == id))
            return NotFound(new { message = "Mission introuvable." });
        if (string.IsNullOrWhiteSpace(input.SortieAttendue))
            return BadRequest(new { message = "La sortie attendue est obligatoire." });

        var t = new Test
        {
            IdMission = id,
            Entree = input.Entree ?? "",
            SortieAttendue = input.SortieAttendue,
            Visible = input.Visible
        };
        _db.Set<Test>().Add(t);
        await _db.SaveChangesAsync();
        return Ok(new { ok = true, id = t.Id });
    }

    [HttpDelete("tests/{id:int}")]
    public async Task<IActionResult> DeleteTest(int id)
    {
        var t = await _db.Set<Test>().FirstOrDefaultAsync(x => x.Id == id);
        if (t == null) return NotFound(new { message = "Test introuvable." });
        _db.Set<Test>().Remove(t);
        await _db.SaveChangesAsync();
        return Ok(new { ok = true });
    }

    // ------------------------------------------------------------ players

    [HttpGet("users")]
    public async Task<IActionResult> Users()
    {
        var list = await _db.Set<Utilisateur>()
            .OrderBy(u => u.Id)
            .Take(200)
            .Select(u => new
            {
                id = u.Id,
                nom = u.Nom,
                email = u.Email,
                xpTotal = u.XpTotal,
                niveauJoueur = u.NiveauJoueur,
                role = u.Role
            })
            .ToListAsync();
        return Ok(list);
    }

    [HttpPut("users/{id:int}/role")]
    public async Task<IActionResult> SetRole(int id, RoleInput input)
    {
        if (input.Role != "joueur" && input.Role != "admin")
            return BadRequest(new { message = "Rôle : joueur ou admin." });
        if (id == CurrentUserId)
            return BadRequest(new { message = "Tu ne peux pas changer ton propre rôle." });

        var u = await _db.Set<Utilisateur>().FirstOrDefaultAsync(x => x.Id == id);
        if (u == null) return NotFound(new { message = "Joueur introuvable." });
        u.Role = input.Role;
        await _db.SaveChangesAsync();
        return Ok(new { ok = true });
    }

    [HttpPost("users/{id:int}/reset-progress")]
    public async Task<IActionResult> ResetProgress(int id)
    {
        var u = await _db.Set<Utilisateur>().FirstOrDefaultAsync(x => x.Id == id);
        if (u == null) return NotFound(new { message = "Joueur introuvable." });
        _db.Set<Soumission>().RemoveRange(_db.Set<Soumission>().Where(s => s.IdUtilisateur == id));
        u.XpTotal = 0;
        u.NiveauJoueur = 1;
        await _db.SaveChangesAsync();
        return Ok(new { ok = true });
    }

    [HttpDelete("users/{id:int}")]
    public async Task<IActionResult> DeleteUser(int id)
    {
        if (id == CurrentUserId)
            return BadRequest(new { message = "Tu ne peux pas supprimer ton propre compte." });
        var u = await _db.Set<Utilisateur>().FirstOrDefaultAsync(x => x.Id == id);
        if (u == null) return NotFound(new { message = "Joueur introuvable." });
        _db.Set<Soumission>().RemoveRange(_db.Set<Soumission>().Where(s => s.IdUtilisateur == id));
        _db.Set<Utilisateur>().Remove(u);
        await _db.SaveChangesAsync();
        return Ok(new { ok = true });
    }
}
