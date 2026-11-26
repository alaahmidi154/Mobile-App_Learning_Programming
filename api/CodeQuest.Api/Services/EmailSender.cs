using System.Net;
using System.Net.Mail;
using System.Text;

namespace CodeQuest.Api.Services;

public interface IEmailSender
{
    /// <summary>
    /// Sends an e-mail. Returns false when SMTP is not configured: the message is then only
    /// written to the API console, which is enough to test the app on a development PC.
    /// </summary>
    Task<bool> SendAsync(string to, string subject, string body, CancellationToken ct = default);
}

public class SmtpEmailSender : IEmailSender
{
    private readonly IConfiguration _config;
    private readonly ILogger<SmtpEmailSender> _log;

    public SmtpEmailSender(IConfiguration config, ILogger<SmtpEmailSender> log)
    {
        _config = config;
        _log = log;
    }

    public async Task<bool> SendAsync(string to, string subject, string body, CancellationToken ct = default)
    {
        var host = _config["Email:Host"];
        var user = _config["Email:User"];
        var password = _config["Email:Password"];

        if (string.IsNullOrWhiteSpace(host) || string.IsNullOrWhiteSpace(user)
            || string.IsNullOrWhiteSpace(password))
        {
            _log.LogWarning("SMTP non configuré (section Email de appsettings.json). " +
                            "E-mail pour {To} | {Subject}\n{Body}", to, subject, body);
            return false;
        }

        var port = int.TryParse(_config["Email:Port"], out var p) ? p : 587;
        var from = string.IsNullOrWhiteSpace(_config["Email:From"]) ? user : _config["Email:From"]!;

        using var client = new SmtpClient(host, port)
        {
            EnableSsl = true,
            Credentials = new NetworkCredential(user, password)
        };
        using var message = new MailMessage(from, to, subject, body)
        {
            SubjectEncoding = Encoding.UTF8,
            BodyEncoding = Encoding.UTF8
        };
        await client.SendMailAsync(message, ct);
        return true;
    }
}
