#  Coding Game App

##  Project Structure

A gamified mobile app for learning programming.

```text
Coding-Game-App/
├── android/       # Android app (Java, Retrofit)
├── api/           # ASP.NET Core REST API (C#, EF Core)
├── sandbox/       # Docker grader (Python runner)
├── database/      # SQL creation and seed scripts
└── docs/          # UML, Swagger/Postman exports, slides
```

##  Run Locally

### 1. Database (PostgreSQL 16)

#### Setup
1. Create a database named `CodingGame`.
2. Run `database/schema_seed_pg.sql`, for example:

   ```bash
   psql -U postgres -d CodingGame -f database/schema_seed_pg.sql
   ```

3. The same script resets the demo data (it drops and recreates the tables).
4. Create the demo accounts through `POST /api/auth/register`.

### 2. API

1. Copy `api/appsettings.Example.json` to `api/appsettings.json`.
2. Fill in your DB connection string and JWT key.
3. Navigate to the API directory and start the application:

   ```bash
   cd api && dotnet run
   ```

### 3. Sandbox (Docker)

**Requirements:** Docker Desktop running, and Python installed.

#### Setup and Testing
1. Navigate to the sandbox folder:
   ```bash
   cd sandbox
   ```

2. Build the Docker image:
   ```bash
   docker build -t py-grader .
   ```

3. Run the tests:
   ```bash
   tests\run_all.bat
   ```

   **Expected verdicts, in order:** `réussi`, `échoué`, `erreur`, `temps dépassé`, `erreur`, `erreur`.

#### API Integration
The API calls the runner using the following command:

```bash
python runner.py --code-file student.py --tests tests.json
```

The runner prints one JSON line:

```json
{"verdict": "réussi", "tempsExecutionMs": 140, "message": "Bravo !"}
```
### 4. Android

1. Copy `android/local.properties.example` to `android/local.properties`.
2. Open `android/` in Android Studio.

