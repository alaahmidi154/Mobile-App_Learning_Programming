@echo off
REM Run from the sandbox folder:  tests\run_all.bat
REM (Docker Desktop must be running and the image built: docker build -t py-grader .)
REM The output is pure ASCII: "réussi" = "reussi" with an e-acute, "échoué" = "echoue", "temps dépassé" = "temps depasse".
REM
REM Expected verdicts, in this order:
REM   correct         -> reussi
REM   wrong           -> echoue
REM   crash           -> erreur
REM   infinite        -> temps depasse
REM   attack_network  -> erreur   (no network)
REM   attack_file     -> erreur   (read-only filesystem)
REM   flood           -> erreur   ("Sortie trop volumineuse")
REM   memory_bomb     -> erreur   (killed at 128 MB)
REM   fork_bomb       -> erreur   (pids limit)
for %%f in (correct wrong crash infinite attack_network attack_file flood memory_bomb fork_bomb) do (
  echo --- %%f
  python runner.py --code-file tests\%%f.py --tests tests\tests.json
)
