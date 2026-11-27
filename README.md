# CodeMissions

A gamified mobile app for learning Python. The player follows a roadmap of coding missions, writes code in a real editor, gets the faulty line highlighted and progressive hints, earns XP and climbs a leaderboard. An admin space lets teachers manage missions, tests and users.

## Features

**Player**
- Sign-up, login, password reset by e-mail
- Roadmap of missions grouped by level, locked until the previous level is done
- Code editor: line numbers, Python syntax colors, auto-indent, symbol bar
- **Tester** (visible tests, nothing saved), **Soumettre** (all tests, XP), **Réinitialiser**, **Indice** (3 progressive hints)
- The line that causes an error is highlighted and the error is explained in French
- Success / failure popup with XP animation
- Leaderboard with podium, profile with badges and logout

**Admin**
- Dashboard with statistics
- Create, edit and delete missions, with visible and hidden tests
- Manage users: change role, reset progress, delete
- Sees all levels unlocked, earns no XP, is not ranked

## Architecture

```
Android app  ──HTTP/JSON (JWT)──>  ASP.NET Core API  ──EF Core──>  PostgreSQL
                                          │
                                          └──docker run──>  py-grader (runner.py)
```

| Folder | Technology | Role |
|---|---|---|
| `android/` | Java, Material 3, Volley | Screens, editor, API calls |
| `api/` | ASP.NET Core (net10.0), EF Core, JWT, BCrypt | Business rules, auth, XP, admin |
| `database/` | PostgreSQL | Users, missions, tests, submissions |
| `sandbox/` | Docker, Python | Runs submitted code in isolation and grades it |
| `scripts/` | Windows batch | One-click demo start, firewall rule |
| `docs/` | Markdown, UML | Documentation |

### How a submission works
1. The app sends the code to `POST /api/missions/{id}/submit` with the JWT.
2. The API loads the mission's tests from the database.
3. The API starts a Docker container (`py-grader`) and passes it the code and tests.
4. `runner.py` runs every test and returns the verdict, the error line and the output.
5. The API computes the XP, saves the submission and answers with JSON.
6. The app highlights the faulty line and shows the result popup.

### Main endpoints
| Endpoint | Access | Purpose |
|---|---|---|
| `POST /api/auth/register`, `/login` | public | Create account, get a JWT |
| `GET /api/me` | logged in | Current user, XP, role |
| `GET /api/missions`, `/api/missions/{id}` | logged in | Mission list and details (examples, hint) |
| `POST /api/missions/{id}/test` | logged in | Run visible tests only, nothing saved |
| `POST /api/missions/{id}/submit` | logged in | Run all tests, award XP |
| `GET /api/leaderboard` | logged in | Ranking |
| `/api/admin/...` | admin only | Stats, missions, tests, users |

API documentation is available at `/scalar/v1` when the API is running.

### Security
- Passwords are hashed with BCrypt.
- The JWT carries the role (`joueur` or `admin`); admin routes are protected and the role is re-checked in the database.
- Hidden tests never leave the server.
- Submitted code only runs inside Docker, with time and output limits.

## Run locally

### Database
1. Create a PostgreSQL database named `codequest`.
2. Run `database/schema_seed_pg.sql` (tables, sample missions and hints).
3. If your database already existed, run `database/add_indice.sql` instead to add the hint column.
4. To make a user admin: `UPDATE "Utilisateur" SET "Role"='admin' WHERE "Email"='you@example.com';` then log in again.

### Sandbox
1. Install and start Docker Desktop.
2. `docker build -t py-grader sandbox`

### API
1. Copy `api/CodeQuest.Api/appsettings.Example.json` to `appsettings.json` in the same folder.
2. Fill in the database connection string, the JWT key and the mail settings. Never commit this file.
3. `cd api/CodeQuest.Api` then `dotnet run --urls http://0.0.0.0:5007`

### Android
1. Open `android/` in Android Studio and sync Gradle.
2. On the emulator the app reaches the API at `10.0.2.2:5007` by default.
3. On a real phone, tap **serveur** on the login screen and type the PC's address (same Wi-Fi).

## Quick demo (Windows)
1. Start Docker Desktop.
2. Double-click `scripts\start-demo.bat` (first time: run `scripts\open-firewall.bat` as administrator).
3. Type one of the printed addresses in the app (**serveur**), then log in.

More details in `docs/DEMO.md`.

## Example mission to try (admin)
Title: *Carré d'un nombre*, level Débutant, 40 XP.
Tests: `5 → 25` (visible), `0 → 0` (hidden), `-3 → 9` (hidden).

## Git workflow
- `dev` is the working branch; features are merged into it with pull requests.
- `release/v1` holds the first release; `main` is kept stable.
- Never commit `appsettings.json`, `bin/`, `obj/`, `.gradle/`, `.idea/`.