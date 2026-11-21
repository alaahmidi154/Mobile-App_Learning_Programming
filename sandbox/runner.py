#!/usr/bin/env python3
"""
Sandbox runner: executes untrusted Python code in a fresh, isolated Docker
container and returns one of 4 verdicts.

Usage (what the ASP.NET Core API will call):
    python runner.py --code-file student.py --tests tests.json

tests.json format:
    [ {"entree": "2\n3\n", "sortieAttendue": "5"}, ... ]

Output (stdout, one JSON line):
    {"verdict": "réussi", "tempsExecutionMs": 140, "message": "..."}

Verdicts: réussi | échoué | erreur | temps dépassé
"""
import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
import time
import uuid

IMAGE = "py-grader"
TIME_LIMIT_S = 5
MAX_OUTPUT_CHARS = 100_000  # stop huge-output attacks from flooding the API

REUSSI = "réussi"
ECHOUE = "échoué"
ERREUR = "erreur"
TEMPS = "temps dépassé"


def normalize(text: str) -> str:
    """Ignore trailing spaces/newlines and Windows line endings."""
    lines = text.replace("\r\n", "\n").strip().split("\n")
    return "\n".join(line.rstrip() for line in lines)


def run_one(code_dir: str, stdin_text: str):
    """Run main.py once in a container. Returns (kind, stdout, stderr, ms)."""
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
        proc = subprocess.run(
            cmd, input=stdin_text, capture_output=True, text=True,
            timeout=TIME_LIMIT_S,
        )
    except subprocess.TimeoutExpired:
        # Killing the docker CLI does not stop the container: kill it by name.
        subprocess.run(["docker", "rm", "-f", name],
                       capture_output=True, timeout=15)
        return "timeout", "", "", int((time.monotonic() - start) * 1000)
    ms = int((time.monotonic() - start) * 1000)

    out = proc.stdout[:MAX_OUTPUT_CHARS]
    err = proc.stderr[:MAX_OUTPUT_CHARS]
    # 137 = killed (out of memory), any other non-zero = crash / syntax error
    if proc.returncode != 0:
        return "error", out, err, ms
    return "ok", out, err, ms


def grade(code: str, tests: list) -> dict:
    workdir = tempfile.mkdtemp(prefix="submission-")
    try:
        os.chmod(workdir, 0o755)
        path = os.path.join(workdir, "main.py")
        with open(path, "w", encoding="utf-8") as f:
            f.write(code)
        os.chmod(path, 0o644)

        total_ms = 0
        for t in tests:
            kind, out, err, ms = run_one(workdir, t.get("entree", ""))
            total_ms += ms
            if kind == "timeout":
                return {"verdict": TEMPS, "tempsExecutionMs": total_ms,
                        "message": f"Limite de {TIME_LIMIT_S}s dépassée."}
            if kind == "error":
                last = err.strip().split("\n")[-1] if err.strip() else "Erreur d'exécution."
                return {"verdict": ERREUR, "tempsExecutionMs": total_ms,
                        "message": last}
            if normalize(out) != normalize(t.get("sortieAttendue", "")):
                return {"verdict": ECHOUE, "tempsExecutionMs": total_ms,
                        "message": "Sortie incorrecte."}
        return {"verdict": REUSSI, "tempsExecutionMs": total_ms,
                "message": "Bravo !"}
    finally:
        shutil.rmtree(workdir, ignore_errors=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--code-file", required=True)
    ap.add_argument("--tests", required=True)
    args = ap.parse_args()

    with open(args.code_file, encoding="utf-8") as f:
        code = f.read()
    with open(args.tests, encoding="utf-8") as f:
        tests = json.load(f)

    result = grade(code, tests)
    sys.stdout.reconfigure(encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
