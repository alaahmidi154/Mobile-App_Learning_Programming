using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using CodeQuest.Api.Models;
using CodeQuest.Api.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Caching.Memory;
using Microsoft.IdentityModel.Tokens;

namespace CodeQuest.Api.Controllers;

public record RegisterRequest(string Nom, string Email, string Password);
public record LoginRequest(string Email, string Password);
public record AuthResponse(string Token, string Nom, int XpTotal, int NiveauJoueur, string Role);
public record ForgotPasswordRequest(string Email);
public record ResetPasswordRequest(string Email, string Code, string NewPassword);

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly IConfiguration _config;
    private readonly IMemoryCache _cache;
    private readonly IEmailSender _email;
    private readonly ILogger<AuthController> _log;

    // A reset code lives 10 minutes in memory, allows 5 tries and one new code every 30 s.
    private sealed class ResetEntry
    {
        public string Code = "";
        public DateTime SentAt;
        public int Attempts;
    }

    public AuthController(AppDbContext db, IConfiguration config, IMemoryCache cache,
        IEmailSender email, ILogger<AuthController> log)
    {
        _db = db;
        _config = config;
        _cache = cache;
        _email = email;
        _log = log;
    }

    private static string ResetKey(string email) => "reset:" + email.Trim().ToLowerInvariant();

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

        return Ok(new AuthResponse(CreateToken(user), user.Nom, user.XpTotal, user.NiveauJoueur, user.Role));
    }

    [HttpPost("login")]
    public async Task<IActionResult> Login(LoginRequest req)
    {
        var user = await _db.Set<Utilisateur>().FirstOrDefaultAsync(u => u.Email == req.Email);
        if (user == null || !BCrypt.Net.BCrypt.Verify(req.Password, user.MotDePasseHache))
            return Unauthorized(new { message = "Email ou mot de passe incorrect." });

        return Ok(new AuthResponse(CreateToken(user), user.Nom, user.XpTotal, user.NiveauJoueur, user.Role));
    }

    [HttpPost("forgot-password")]
    public async Task<IActionResult> ForgotPassword(ForgotPasswordRequest req)
    {
        var email = (req.Email ?? "").Trim();
        if (email.Length == 0)
            return BadRequest(new { message = "E-mail obligatoire." });

        // Same answer whether the account exists or not (does not reveal who is registered).
        const string generic = "Si cet e-mail existe, un code de réinitialisation vient d'être envoyé.";

        var user = await _db.Set<Utilisateur>().FirstOrDefaultAsync(u => u.Email == email);
        if (user == null)
            return Ok(new { message = generic });

        var key = ResetKey(email);
        if (_cache.TryGetValue(key, out ResetEntry? previous) && previous != null
            && previous.SentAt > DateTime.UtcNow.AddSeconds(-30))
            return StatusCode(StatusCodes.Status429TooManyRequests,
                new { message = "Patientez quelques secondes avant de redemander un code." });

        var code = RandomNumberGenerator.GetInt32(0, 1_000_000).ToString("D6");
        _cache.Set(key, new ResetEntry { Code = code, SentAt = DateTime.UtcNow },
            TimeSpan.FromMinutes(10));

        try
        {
            await _email.SendAsync(user.Email, "CodeMissions : ton code de réinitialisation",
                $"Bonjour {user.Nom},\n\nTon code de réinitialisation : {code}\n" +
                "Il est valable 10 minutes.\n\nSi tu n'as pas fait cette demande, ignore ce message.",
                HttpContext.RequestAborted);
        }
        catch (Exception ex)
        {
            _log.LogError(ex, "Envoi de l'e-mail de réinitialisation échoué");
            _cache.Remove(key);
            return StatusCode(StatusCodes.Status503ServiceUnavailable,
                new { message = "Impossible d'envoyer l'e-mail pour le moment. Réessayez plus tard." });
        }

        return Ok(new { message = generic });
    }

    [HttpPost("reset-password")]
    public async Task<IActionResult> ResetPassword(ResetPasswordRequest req)
    {
        var email = (req.Email ?? "").Trim();
        var code = (req.Code ?? "").Trim();
        if (email.Length == 0 || code.Length == 0 || string.IsNullOrEmpty(req.NewPassword))
            return BadRequest(new { message = "E-mail, code et nouveau mot de passe obligatoires." });
        if (req.NewPassword.Length < 6)
            return BadRequest(new { message = "Le mot de passe doit contenir au moins 6 caractères." });

        const string invalid = "Code invalide ou expiré.";
        var key = ResetKey(email);
        if (!_cache.TryGetValue(key, out ResetEntry? entry) || entry == null)
            return BadRequest(new { message = invalid });

        entry.Attempts++;
        if (entry.Attempts > 5)
        {
            _cache.Remove(key);   // too many wrong codes: a new code must be requested
            return BadRequest(new { message = invalid });
        }

        var same = CryptographicOperations.FixedTimeEquals(
            Encoding.UTF8.GetBytes(entry.Code), Encoding.UTF8.GetBytes(code));
        if (!same)
            return BadRequest(new { message = invalid });

        var user = await _db.Set<Utilisateur>().FirstOrDefaultAsync(u => u.Email == email);
        if (user == null)
            return BadRequest(new { message = invalid });

        user.MotDePasseHache = BCrypt.Net.BCrypt.HashPassword(req.NewPassword);
        await _db.SaveChangesAsync();
        _cache.Remove(key);

        return Ok(new { message = "Mot de passe modifié. Tu peux te connecter." });
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