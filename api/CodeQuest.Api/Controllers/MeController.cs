using System.Security.Claims;
using CodeQuest.Api.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CodeQuest.Api.Controllers;

/// <summary>
/// The app keeps XP / level / role in the phone. This lets it re-read the truth from the
/// database (an admin may have reset the progress or changed the role).
/// </summary>
[ApiController]
[Authorize]
[Route("api/me")]
public class MeController : ControllerBase
{
    private readonly AppDbContext _db;

    public MeController(AppDbContext db) => _db = db;

    [HttpGet]
    public async Task<IActionResult> Get()
    {
        var id = int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        var u = await _db.Set<Utilisateur>().FirstOrDefaultAsync(x => x.Id == id);
        if (u == null) return Unauthorized(new { message = "Compte introuvable." });   // deleted account
        return Ok(new { id = u.Id, nom = u.Nom, xpTotal = u.XpTotal, niveauJoueur = u.NiveauJoueur, role = u.Role });
    }
}
