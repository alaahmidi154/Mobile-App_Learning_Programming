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

        var missions = await _db.Set<Mission>().OrderBy(m => m.Id).ToListAsync();

        return Ok(missions.Select(m => new
        {
            id = m.Id,
            titre = m.Titre,
            difficulte = m.Difficulte,
            xp = m.Xp,
            reussie = reussies.Contains(m.Id)
        }));
    }

    [HttpGet("{id:int}")]
    public async Task<IActionResult> GetById(int id)
    {
        var m = await _db.Set<Mission>().FirstOrDefaultAsync(x => x.Id == id);
        if (m == null) return NotFound(new { message = "Mission introuvable." });

        return Ok(new { id = m.Id, titre = m.Titre, enonce = m.Enonce, codeDepart = m.CodeDepart, xp = m.Xp });
    }

    [HttpPost("{id:int}/submit")]
    public async Task<IActionResult> Submit(int id, SubmitRequest req)
    {
        var userId = int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);

        var mission = await _db.Set<Mission>().FirstOrDefaultAsync(m => m.Id == id);
        if (mission == null) return NotFound(new { message = "Mission introuvable." });
        if (string.IsNullOrWhiteSpace(req.Code))
            return BadRequest(new { message = "Code vide." });
        if (req.Code.Length > MaxCodeLength)
            return BadRequest(new { message = "Code trop long." });

        // ALL tests (visible and hidden) go to the sandbox.
        var tests = await _db.Set<Test>()
            .Where(t => t.IdMission == id)
            .OrderBy(t => t.Id)
            .Select(t => new SandboxTest(t.Entree, t.SortieAttendue))
            .ToListAsync();

        if (tests.Count == 0)
            return Conflict(new { message = "Cette mission n'a pas encore de tests." });

        SandboxResult result;
        try
        {
            result = await _sandbox.RunAsync(req.Code, tests, HttpContext.RequestAborted);
        }
        catch (SandboxUnavailableException ex)
        {
            // Docker closed / runner broken: not the student's fault, nothing is saved.
            _log.LogError(ex, "Sandbox indisponible pour la mission {MissionId}", id);
            return StatusCode(StatusCodes.Status503ServiceUnavailable,
                new { message = "Service de correction indisponible. Réessayez dans un instant." });
        }

        var verdict = result.Verdict;
        var tempsMs = result.TempsExecutionMs;

        var dejaReussie = await _db.Set<Soumission>().AnyAsync(s =>
            s.IdUtilisateur == userId && s.IdMission == id && s.Verdict == "réussi");

        var user = await _db.Set<Utilisateur>().FirstAsync(u => u.Id == userId);

        var xpGagnes = 0;
        if (verdict == "réussi" && !dejaReussie)
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
            TempsExecution = tempsMs,
            Date = DateTime.UtcNow
        });
        await _db.SaveChangesAsync();

        var message = verdict == "réussi"
            ? (xpGagnes > 0 ? "Bravo !" : "Déjà réussie, pas de XP supplémentaire.")
            : result.Message;

        return Ok(new
        {
            verdict,
            tempsExecutionMs = tempsMs,
            xpGagnes,
            xpTotal = user.XpTotal,
            message
        });
    }
}
