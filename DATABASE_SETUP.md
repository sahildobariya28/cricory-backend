# Cricory database setup

The Android application reads Spring Boot APIs. REST requests read only the
`api_snapshots` database table; scraping is performed by scheduled background
jobs.

## Recommended local setup: Docker Desktop

1. Install and start Docker Desktop.
2. From `cricory-backend`, start PostgreSQL and pgAdmin:

   ```powershell
   docker compose up -d
   ```

3. Start Spring Boot with the PostgreSQL profile from the repository root:

   ```powershell
   .\gradlew :cricory-backend:bootRun --args="--spring.profiles.active=postgres"
   ```

Flyway automatically creates and upgrades all tables. Do not manually create
tables in pgAdmin.

## pgAdmin connection

Open `http://localhost:5050` and sign in with:

- Email: `admin@cricory.local`
- Password: `admin_dev_password`

Register a server using:

- Name: `Cricory Local`
- Host: `postgres` when pgAdmin runs through Compose
- Port: `5432`
- Database: `cricory`
- Username: `cricory`
- Password: `cricory_dev_password`

The important table is `public.api_snapshots`. Its rows are `live-matches`,
`upcoming-matches`, `recent-matches`, `news`, `series-list`, and
`players-list`.

## Without Docker

Install PostgreSQL 17 and create database/user `cricory`. Then provide secrets
without committing them:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/cricory"
$env:DB_USERNAME="cricory"
$env:DB_PASSWORD="your-password"
.\gradlew :cricory-backend:bootRun --args="--spring.profiles.active=postgres"
```

## Refresh intervals

- Live matches: 20 seconds
- Recent results: 15 minutes
- Upcoming matches: 30 minutes
- News: 10 minutes
- Series and players: 24 hours

If a refresh fails, the last successful database snapshot remains available.
Changing these values only requires overriding `cricory.sync.*` properties.
