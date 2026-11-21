using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using CodeQuest.Api.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;

namespace CodeQuest.Api.Controllers;

public record RegisterRequest(string Nom, string Email, string Password);
public record LoginRequest(string Email, string Password);
public record AuthResponse(string Token, string Nom, int XpTotal, int NiveauJoueur);

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly IConfiguration _config;

    public AuthController(AppDbContext db, IConfiguration config)
    {
        _db = db;
        _config = config;
    }

    [HttpPost("register")]
    public async Task<IActionResult> Register(RegisterRequest req)
    {
        if (string.IsNullOrWhiteSpace(req.Nom) || string.IsNullOrWhiteSpace(req.Email)
            || string.IsNullOrWhiteSpace(req.Password))
            return BadRequest(new { message = "Nom, email et mot de passe obligatoires." });

        var users = _db.Set<Utilisateur>();
        if (await users.AnyAsync(u => u.Email == req.Email))
            return Conflict(new { message = "Cet email existe déjà." });

        var user = new Utilisateur
        {
            Nom = req.Nom,
            Email = req.Email,
            MotDePasseHache = BCrypt.Net.BCrypt.HashPassword(req.Password),
            XpTotal = 0,
            NiveauJoueur = 1,
            Role = "joueur"
        };
        users.Add(user);
        await _db.SaveChangesAsync();

        return Ok(new AuthResponse(CreateToken(user), user.Nom, user.XpTotal, user.NiveauJoueur));
    }

    [HttpPost("login")]
    public async Task<IActionResult> Login(LoginRequest req)
    {
        var user = await _db.Set<Utilisateur>().FirstOrDefaultAsync(u => u.Email == req.Email);
        if (user == null || !BCrypt.Net.BCrypt.Verify(req.Password, user.MotDePasseHache))
            return Unauthorized(new { message = "Email ou mot de passe incorrect." });

        return Ok(new AuthResponse(CreateToken(user), user.Nom, user.XpTotal, user.NiveauJoueur));
    }

    private string CreateToken(Utilisateur user)
    {
        var jwt = _config.GetSection("Jwt");
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwt["Key"]!));
        var claims = new[]
        {
            new Claim(ClaimTypes.NameIdentifier, user.Id.ToString()),
            new Claim(ClaimTypes.Name, user.Nom),
            new Claim(ClaimTypes.Role, user.Role)
        };
        var token = new JwtSecurityToken(
            issuer: jwt["Issuer"],
            audience: jwt["Audience"],
            claims: claims,
            expires: DateTime.UtcNow.AddHours(12),
            signingCredentials: new SigningCredentials(key, SecurityAlgorithms.HmacSha256));
        return new JwtSecurityTokenHandler().WriteToken(token);
    }
}