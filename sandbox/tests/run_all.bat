@echo off
REM Run from the sandbox folder:  tests\run_all.bat
REM Expected: réussi, échoué, erreur, temps dépassé, erreur, erreur
for %%f in (correct wrong crash infinite attack_network attack_file) do (
  echo --- %%f
  python runner.py --code-file tests\%%f.py --tests tests\tests.json
)
