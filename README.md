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

### 1. Database

1. Run `database/schema.sql`.
2. Run `database/seed.sql`.

### 2. API

1. Copy `api/appsettings.Example.json` to `api/appsettings.json`.
2. Fill in your DB connection string and JWT key.
3. Navigate to the API directory and start the application:

   ```bash
   cd api && dotnet run
   ```

### 3. Sandbox

1. Navigate to the sandbox directory and build the Docker image:

   ```bash
   cd sandbox && docker build -t py-grader .
   ```

### 4. Android

1. Copy `android/local.properties.example` to `android/local.properties`.
2. Open `android/` in Android Studio.
