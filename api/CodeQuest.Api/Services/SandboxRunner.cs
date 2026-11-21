using System.ComponentModel;
using System.Diagnostics;
using System.Text;
using System.Text.Json;

namespace CodeQuest.Api.Services;

/// <summary>One test case sent to the sandbox (hidden tests included).</summary>
public record SandboxTest(string Entree, string SortieAttendue);

/// <summary>Verdict returned by sandbox/runner.py.</summary>
public record SandboxResult(string Verdict, int TempsExecutionMs, string Message);

/// <summary>JSON line printed by runner.py.</summary>
public record RunnerOutput(string? Verdict, int TempsExecutionMs, string? Message);

/// <summary>
/// The sandbox itself could not do its job (Docker closed, python missing,
/// runner crashed). This is NOT a verdict about the student's code.
/// </summary>
public class SandboxUnavailableException : Exception
{
    public SandboxUnavailableException(string message, Exception? inner = null)
        : base(message, inner) { }
}

public interface ISandboxRunner
{
    Task<SandboxResult> RunAsync(string code, IReadOnlyList<SandboxTest> tests,
        CancellationToken ct = default);
}

/// <summary>
/// Calls:  python sandbox/runner.py --code-file student.py --tests tests.json
/// and reads the one JSON line it prints.
/// </summary>
public class DockerSandboxRunner : ISandboxRunner
{
    private static readonly HashSet<string> KnownVerdicts =
        new() { "réussi", "échoué", "erreur", "temps dépassé" };

    private static readonly JsonSerializerOptions JsonOpts =
        new() { PropertyNameCaseInsensitive = true };

    // At most 2 submissions graded at the same time (protects the demo laptop).
    private static readonly SemaphoreSlim Gate = new(2);

    private readonly string _python;
    private readonly string _runnerPath;
    private readonly TimeSpan _timeout;
    private readonly ILogger<DockerSandboxRunner> _log;

    public DockerSandboxRunner(IConfiguration config, IHostEnvironment env,
        ILogger<DockerSandboxRunner> log)
    {
        _log = log;
        var section = config.GetSection("Sandbox");

        _python = section["PythonCommand"] ?? "python";

        var configuredPath = section["RunnerPath"];
        _runnerPath = !string.IsNullOrWhiteSpace(configuredPath)
            ? configuredPath
            // default: <repo>/sandbox/runner.py when running from <repo>/api/CodeQuest.Api
            : Path.GetFullPath(Path.Combine(env.ContentRootPath, "..", "..", "sandbox", "runner.py"));

        var seconds = int.TryParse(section["TimeoutSeconds"], out var parsed) && parsed > 0
            ? parsed
            : 30;
        _timeout = TimeSpan.FromSeconds(seconds);
    }

    public async Task<SandboxResult> RunAsync(string code, IReadOnlyList<SandboxTest> tests,
        CancellationToken ct = default)
    {
        if (!File.Exists(_runnerPath))
            throw new SandboxUnavailableException($"runner.py introuvable : {_runnerPath}");

        await Gate.WaitAsync(ct);
        var dir = Path.Combine(Path.GetTempPath(), "codequest-" + Guid.NewGuid().ToString("N"));
        try
        {
            Directory.CreateDirectory(dir);
            var utf8 = new UTF8Encoding(false);
            var codeFile = Path.Combine(dir, "student.py");
            var testsFile = Path.Combine(dir, "tests.json");

            await File.WriteAllTextAsync(codeFile, code, utf8, ct);
            var testsJson = JsonSerializer.Serialize(
                tests.Select(t => new { entree = t.Entree, sortieAttendue = t.SortieAttendue }));
            await File.WriteAllTextAsync(testsFile, testsJson, utf8, ct);

            var psi = new ProcessStartInfo
            {
                FileName = _python,
                WorkingDirectory = Path.GetDirectoryName(_runnerPath)!,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                StandardOutputEncoding = Encoding.UTF8,
                StandardErrorEncoding = Encoding.UTF8,
                UseShellExecute = false,
                CreateNoWindow = true
            };
            psi.ArgumentList.Add(_runnerPath);
            psi.ArgumentList.Add("--code-file");
            psi.ArgumentList.Add(codeFile);
            psi.ArgumentList.Add("--tests");
            psi.ArgumentList.Add(testsFile);
            psi.Environment["PYTHONIOENCODING"] = "utf-8";

            Process proc;
            try
            {
                proc = Process.Start(psi)
                       ?? throw new SandboxUnavailableException("Impossible de lancer le correcteur.");
            }
            catch (Win32Exception ex)
            {
                throw new SandboxUnavailableException(
                    $"Commande introuvable : '{_python}' (voir Sandbox:PythonCommand).", ex);
            }

            using (proc)
            {
                var stdoutTask = proc.StandardOutput.ReadToEndAsync();
                var stderrTask = proc.StandardError.ReadToEndAsync();

                using var cts = CancellationTokenSource.CreateLinkedTokenSource(ct);
                cts.CancelAfter(_timeout);
                try
                {
                    await proc.WaitForExitAsync(cts.Token);
                }
                catch (OperationCanceledException)
                {
                    try { proc.Kill(entireProcessTree: true); } catch { /* already gone */ }
                    ct.ThrowIfCancellationRequested();
                    throw new SandboxUnavailableException("Délai de correction dépassé.");
                }

                var stdout = await stdoutTask;
                var stderr = await stderrTask;

                // runner.py exits with 0 when it graded the code (whatever the verdict),
                // and with a non-zero code when Docker / the runner itself failed.
                if (proc.ExitCode != 0)
                {
                    _log.LogError("runner.py exit code {Code}. stdout: {Out} stderr: {Err}",
                        proc.ExitCode, stdout, stderr);
                    throw new SandboxUnavailableException(
                        $"Le correcteur a échoué (code {proc.ExitCode}).");
                }

                var line = stdout
                    .Split('\n', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
                    .LastOrDefault();

                RunnerOutput? output;
                try
                {
                    output = line == null ? null : JsonSerializer.Deserialize<RunnerOutput>(line, JsonOpts);
                }
                catch (JsonException ex)
                {
                    _log.LogError(ex, "Sortie illisible de runner.py : {Out}", stdout);
                    throw new SandboxUnavailableException("Réponse illisible du correcteur.", ex);
                }

                if (output?.Verdict == null || !KnownVerdicts.Contains(output.Verdict))
                {
                    _log.LogError("Verdict inconnu de runner.py : {Out}", stdout);
                    throw new SandboxUnavailableException("Verdict inconnu renvoyé par le correcteur.");
                }

                return new SandboxResult(output.Verdict, output.TempsExecutionMs, output.Message ?? "");
            }
        }
        finally
        {
            Gate.Release();
            try { Directory.Delete(dir, recursive: true); } catch { /* best effort */ }
        }
    }
}
