using CodeQuest.Api.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CodeQuest.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/leaderboard")]
public class LeaderboardController : ControllerBase
{
    private readonly AppDbContext _db;

    public LeaderboardController(AppDbContext db) => _db = db;

    [HttpGet]
    public async Task<IActionResult> Get()
    {
        var users = await _db.Set<Utilisateur>()
            .OrderByDescending(u => u.XpTotal)
            .ThenBy(u => u.Id)
            .Take(50)
            .ToListAsync();

        return Ok(users.Select((u, i) => new
        {
            rang = i + 1,
            nom = u.Nom,
            xpTotal = u.XpTotal,
            niveauJoueur = u.NiveauJoueur
        }));
    }
}
