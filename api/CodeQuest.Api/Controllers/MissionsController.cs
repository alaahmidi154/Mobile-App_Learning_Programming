using System.Security.Claims;
using CodeQuest.Api.Models;
using CodeQuest.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CodeQuest.Api.Controllers;

public record SubmitRequest(string Code);

[ApiController]
[Authorize]
[Route("api/missions")]
public class MissionsController : ControllerBase
{
    private const int MaxCodeLength = 20_000;

    private record TestRow(string Entree, string Sortie, bool Visible);

    private readonly AppDbContext _db;
    private readonly ISandboxRunner _sandbox;
    private readonly ILogger<MissionsController> _log;

    public MissionsController(AppDbContext db, ISandboxRunner sandbox, ILogger<MissionsController> log)
    {
        _db = db;
        _sandbox = sandbox;
        _log = log;
    }

    [HttpGet]
    public async Task<IActionResult> GetAll()
    {
        var userId = int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);

        var reussies = await _db.Set<Soumission>()
            .Where(s => s.IdUtilisateur == userId && s.Verdict == "réussi")
            .Select(s => s.IdMission)
            .Distinct()
            .ToListAsync();

        var withTests = await _db.Set<Test>().Select(t => t.IdMission).Distinct().ToListAsync();
        var isAdmin = User.IsInRole("admin");

        var missions = (await _db.Set<Mission>().ToListAsync())
            // a mission nobody can be graded on is not shown to players (admins still see it)
            .Where(m => isAdmin || withTests.Contains(m.Id))
            // roadmap order = Débutant, then Intermédiaire, then Avancé, then creation order
            .OrderBy(m => LevelRank(m.Difficulte))
            .ThenBy(m => m.Id)
            .ToList();

        return Ok(missions.Select(m => new
        {
            id = m.Id,
            titre = m.Titre,
            difficulte = m.Difficulte,
            xp = m.Xp,
            reussie = reussies.Contains(m.Id)
        }));
    }

    private static int LevelRank(string difficulte) => difficulte switch
    {
        "Débutant" => 1,
        "Intermédiaire" => 2,
        "Avancé" => 3,
        _ => 9
    };

    [HttpGet("{id:int}")]
    public async Task<IActionResult> GetById(int id)
    {
        var m = await _db.Set<Mission>().FirstOrDefaultAsync(x => x.Id == id);
        if (m == null) return NotFound(new { message = "Mission introuvable." });

        // Only the VISIBLE tests are shown to the player (as examples).
        var exemples = await _db.Set<Test>()
            .Where(t => t.IdMission == id && t.Visible)
            .OrderBy(t => t.Id)
            .Select(t => new { entree = t.Entree, sortieAttendue = t.SortieAttendue })
            .ToListAsync();

        return Ok(new
        {
            id = m.Id,
            titre = m.Titre,
            enonce = m.Enonce,
            codeDepart = m.CodeDepart,
            xp = m.Xp,
            difficulte = m.Difficulte,
            indice = m.Indice,
            exemples
        });
    }

    /// <summary>"Tester": runs the visible tests only. Nothing is saved, no XP.</summary>
    [HttpPost("{id:int}/test")]
    public Task<IActionResult> TestCode(int id, SubmitRequest req) => Grade(id, req, save: false);

    /// <summary>"Soumettre": runs ALL tests, saves the submission, gives XP once.</summary>
    [HttpPost("{id:int}/submit")]
    public Task<IActionResult> Submit(int id, SubmitRequest req) => Grade(id, req, save: true);

    private async Task<IActionResult> Grade(int id, SubmitRequest req, bool save)
    {
        var userId = int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);

        var mission = await _db.Set<Mission>().FirstOrDefaultAsync(m => m.Id == id);
        if (mission == null) return NotFound(new { message = "Mission introuvable." });
        if (string.IsNullOrWhiteSpace(req.Code))
            return BadRequest(new { message = "Code vide." });
        if (req.Code.Length > MaxCodeLength)
            return BadRequest(new { message = "Code trop long." });

        var query = _db.Set<Test>().Where(t => t.IdMission == id);
        if (!save) query = query.Where(t => t.Visible);

        var rows = await query.OrderBy(t => t.Id)
            .Select(t => new TestRow(t.Entree, t.SortieAttendue, t.Visible))
            .ToListAsync();

        if (rows.Count == 0)
            return Conflict(new { message = "Cette mission n'a pas encore de tests." });

        SandboxResult result;
        try
        {
            result = await _sandbox.RunAsync(req.Code,
                rows.Select(r => new SandboxTest(r.Entree, r.Sortie)).ToList(),
                HttpContext.RequestAborted);
        }
        catch (SandboxUnavailableException ex)
        {
            // Docker closed / runner broken: not the student's fault, nothing is saved.
            _log.LogError(ex, "Sandbox indisponible pour la mission {MissionId}", id);
            return StatusCode(StatusCodes.Status503ServiceUnavailable,
                new { message = "Service de correction indisponible. Réessayez dans un instant." });
        }

        var verdict = result.Verdict;
        var xpGagnes = 0;
        int xpTotal;

        var user = await _db.Set<Utilisateur>().FirstAsync(u => u.Id == userId);

        if (save)
        {
            var dejaReussie = await _db.Set<Soumission>().AnyAsync(s =>
                s.IdUtilisateur == userId && s.IdMission == id && s.Verdict == "réussi");

            if (verdict == "réussi" && !dejaReussie && user.Role != "admin")
            {
                xpGagnes = mission.Xp;
                user.XpTotal += xpGagnes;
                user.NiveauJoueur = user.XpTotal / 100 + 1;
            }

            _db.Set<Soumission>().Add(new Soumission
            {
                IdUtilisateur = userId,
                IdMission = id,
                Code = req.Code,
                Verdict = verdict,
                TempsExecution = result.TempsExecutionMs,
                Date = DateTime.UtcNow
            });
            await _db.SaveChangesAsync();
        }
        xpTotal = user.XpTotal;

        string message;
        if (verdict == "réussi")
        {
            message = !save ? "Les exemples passent. Soumets pour valider la mission !"
                : xpGagnes > 0 ? "Bravo !"
                : user.Role == "admin" ? "Mode admin : mission réussie, pas de XP."
                : "Déjà réussie, pas de XP supplémentaire.";
        }
        else message = result.Message;

        // Only VISIBLE tests are detailed; a hidden test never reveals its data.
        object? details = null;
        if (verdict == "échoué" && result.TestIndex is int i && i >= 0 && i < rows.Count)
        {
            var t = rows[i];
            details = t.Visible
                ? (object)new { visible = true, entree = t.Entree, attendu = t.Sortie, obtenu = result.Obtenu }
                : new { visible = false, entree = (string?)null, attendu = (string?)null, obtenu = (string?)null };
        }

        return Ok(new
        {
            verdict,
            tempsExecutionMs = result.TempsExecutionMs,
            xpGagnes,
            xpTotal,
            message,
            ligne = result.Ligne,
            apercu = !save,
            details
        });
    }
}
