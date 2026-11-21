#!/usr/bin/env python3
"""
Sandbox runner: executes untrusted Python code in fresh, isolated Docker
containers (one per test case) and returns one of 4 verdicts.

Usage (what the ASP.NET Core API calls):
    python runner.py --code-file student.py --tests tests.json

tests.json format:
    [ {"entree": "2\\n3\\n", "sortieAttendue": "5"}, ... ]

Output (stdout, ONE json line, pure ASCII so Windows code pages cannot corrupt it):
    {"verdict": "r\\u00e9ussi", "tempsExecutionMs": 140, "message": "Bravo !"}
    ("\\u00e9" is just the JSON way to write "e acute": any JSON parser decodes it.)

Verdicts: réussi | échoué | erreur | temps dépassé

Exit codes:
    0  the code was graded (whatever the verdict)
    2  the sandbox itself failed (Docker closed, image missing, runner bug).
       stdout is then {"error": "..."} and the API must answer "service unavailable"
       instead of saving a verdict.
"""
import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
import threading
import time
import uuid
from concurrent.futures import ThreadPoolExecutor

IMAGE = "py-grader"
TIME_LIMIT_S = 5               # per test case
MAX_OUTPUT_BYTES = 64 * 1024   # stdout / stderr cap per run
MAX_PARALLEL = 4               # test cases graded at the same time
EXIT_INFRA = 2

REUSSI = "réussi"
ECHOUE = "échoué"
ERREUR = "erreur"
TEMPS = "temps dépassé"


class SandboxUnavailable(Exception):
    """Docker / the runner failed: this says nothing about the student's code."""


def normalize(text: str) -> str:
    """Ignore trailing spaces/newlines and Windows line endings."""
    lines = text.replace("\r\n", "\n").strip().split("\n")
    return "\n".join(line.rstrip() for line in lines)


def preflight() -> None:
    """Fail fast (and clearly) if Docker is closed or the image is missing.

    Done once per submission, so a student can never fake an 'infrastructure'
    error by choosing a special exit code in their own program.
    """
    try:
        r = subprocess.run(["docker", "image", "inspect", IMAGE],
                           capture_output=True, text=True, timeout=20)
    except subprocess.TimeoutExpired:
        raise SandboxUnavailable("Docker ne repond pas.")
    except OSError:
        raise SandboxUnavailable("Docker n'est pas installe ou introuvable dans le PATH.")
    if r.returncode != 0:
        lines = r.stderr.strip().splitlines()
        detail = lines[-1] if lines else "?"
        raise SandboxUnavailable(
            f"Docker indisponible ou image '{IMAGE}' absente ({detail}). "
            f"Lancez Docker Desktop puis: docker build -t {IMAGE} sandbox")


def run_one(code_dir: str, stdin_text: str):
    """Run main.py once in a container.

    Returns (kind, stdout, stderr, elapsed_ms, returncode) where kind is
    'ok' | 'error' | 'timeout' | 'overflow'.
    """
    name = "grader-" + uuid.uuid4().hex[:12]
    cmd = [
        "docker", "run", "--rm", "-i",
        "--name", name,
        "--network", "none",
        "--memory=128m", "--memory-swap=128m",
        "--cpus=0.5",
        "--pids-limit=64",
        "--read-only",
        "--tmpfs", "/tmp:rw,noexec,nosuid,size=8m",
        "--cap-drop=ALL",
        "--security-opt", "no-new-privileges",
        "-v", f"{code_dir}:/app:ro",
        IMAGE,
    ]
    start = time.monotonic()
    try:
        proc = subprocess.Popen(cmd, stdin=subprocess.PIPE,
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    except OSError as e:
        raise SandboxUnavailable(f"Impossible de lancer Docker: {e}")

    out_buf, err_buf = bytearray(), bytearray()
    overflow = threading.Event()

    def pump(stream, buf):
        # Keep at most MAX_OUTPUT_BYTES in memory, so a `while True: print(...)`
        # can never fill the API's memory. Keep READING (and discarding) after
        # the cap: if nobody reads, the pipe fills up and the container freezes,
        # which makes stopping it slow.
        try:
            while True:
                chunk = stream.read1(8192)
                if not chunk:
                    return
                room = MAX_OUTPUT_BYTES - len(buf)
                if room > 0:
                    buf.extend(chunk[:room])
                if len(chunk) > room:
                    overflow.set()
        except (OSError, ValueError):
            return

    threads = [
        threading.Thread(target=pump, args=(proc.stdout, out_buf), daemon=True),
        threading.Thread(target=pump, args=(proc.stderr, err_buf), daemon=True),
    ]
    for t in threads:
        t.start()

    try:
        proc.stdin.write(stdin_text.encode("utf-8"))
        proc.stdin.close()
    except (BrokenPipeError, OSError):
        pass  # the program exited without reading its input: not an error

    kind = None
    deadline = start + TIME_LIMIT_S
    while kind is None:
        try:
            proc.wait(timeout=0.02)
            break
        except subprocess.TimeoutExpired:
            pass
        if overflow.is_set():
            kind = "overflow"
        elif time.monotonic() >= deadline:
            kind = "timeout"

    if kind is not None:
        # Killing the docker CLI does not stop the container: kill it by name
        # (fast SIGKILL; --rm then removes it), then stop the CLI itself.
        try:
            subprocess.run(["docker", "kill", name],
                           capture_output=True, timeout=10)
        except (subprocess.TimeoutExpired, OSError):
            pass
        try:
            proc.kill()
        except OSError:
            pass
        try:
            proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            pass
        # Safety net: make sure nothing is left behind (no-op if --rm already did it).
        try:
            subprocess.run(["docker", "rm", "-f", name],
                           capture_output=True, timeout=10)
        except (subprocess.TimeoutExpired, OSError):
            pass

    for t in threads:
        t.join(timeout=5)

    ms = int((time.monotonic() - start) * 1000)
    out = out_buf.decode("utf-8", errors="replace")
    err = err_buf.decode("utf-8", errors="replace")
    rc = proc.returncode

    if kind is None and overflow.is_set():
        kind = "overflow"
    if kind is None:
        kind = "ok" if rc == 0 else "error"
    return kind, out, err, ms, rc


def error_message(err: str, rc) -> str:
    lines = [ln for ln in err.strip().splitlines() if ln.strip()]
    if lines:
        return lines[-1][:300]
    if rc == 137:
        return "Memoire insuffisante (limite 128 Mo)."
    return "Erreur d'execution."


def grade(code: str, tests: list) -> dict:
    if not tests:
        raise SandboxUnavailable("Aucun test fourni.")
    preflight()

    workdir = tempfile.mkdtemp(prefix="submission-")
    try:
        os.chmod(workdir, 0o755)
        path = os.path.join(workdir, "main.py")
        with open(path, "w", encoding="utf-8") as f:
            f.write(code)
        os.chmod(path, 0o644)

        # Each test gets its own container; they run in parallel to keep the
        # answer fast. Expected outputs are NEVER sent into the container.
        workers = max(1, min(MAX_PARALLEL, len(tests)))
        with ThreadPoolExecutor(max_workers=workers) as pool:
            runs = list(pool.map(lambda t: run_one(workdir, t.get("entree", "")), tests))

        elapsed = max(r[3] for r in runs)

        # The verdict is decided by the FIRST test (in order) that does not pass.
        for t, (kind, out, err, _ms, rc) in zip(tests, runs):
            if kind == "timeout":
                return {"verdict": TEMPS, "tempsExecutionMs": elapsed,
                        "message": f"Limite de {TIME_LIMIT_S}s depassee."}
            if kind == "overflow":
                return {"verdict": ERREUR, "tempsExecutionMs": elapsed,
                        "message": "Sortie trop volumineuse."}
            if kind == "error":
                return {"verdict": ERREUR, "tempsExecutionMs": elapsed,
                        "message": error_message(err, rc)}
            if normalize(out) != normalize(t.get("sortieAttendue", "")):
                return {"verdict": ECHOUE, "tempsExecutionMs": elapsed,
                        "message": "Sortie incorrecte."}
        return {"verdict": REUSSI, "tempsExecutionMs": elapsed, "message": "Bravo !"}
    finally:
        shutil.rmtree(workdir, ignore_errors=True)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--code-file", required=True)
    ap.add_argument("--tests", required=True)
    args = ap.parse_args()

    try:
        with open(args.code_file, encoding="utf-8") as f:
            code = f.read()
        with open(args.tests, encoding="utf-8") as f:
            tests = json.load(f)
        result = grade(code, tests)
    except SandboxUnavailable as e:
        print(json.dumps({"error": str(e)}, ensure_ascii=True))
        return EXIT_INFRA
    except Exception as e:  # runner bug / unreadable input: still "sandbox failed"
        print(json.dumps({"error": f"{type(e).__name__}: {e}"}, ensure_ascii=True))
        return EXIT_INFRA

    print(json.dumps(result, ensure_ascii=True))
    return 0


if __name__ == "__main__":
    sys.exit(main())
