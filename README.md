\# Coding Game App



Gamified mobile app for learning programming.



\## Structure

\- `android/`  Android app (Java, Retrofit)

\- `api/`      ASP.NET Core REST API (C#, EF Core)

\- `sandbox/`  Docker grader (Python runner)

\- `database/` SQL creation and seed scripts

\- `docs/`     UML, Swagger/Postman exports, slides



\## Run locally

\### Database

1\. Run `database/schema.sql`, then `database/seed.sql`

\### API

1\. Copy `api/appsettings.Example.json` to `api/appsettings.json`

2\. Fill in your DB connection string and JWT key

3\. `cd api \&\& dotnet run`

\### Sandbox

1\. `cd sandbox \&\& docker build -t py-grader .`

\### Android

1\. Copy `android/local.properties.example` to `android/local.properties`

2\. Open `android/` in Android Studio

