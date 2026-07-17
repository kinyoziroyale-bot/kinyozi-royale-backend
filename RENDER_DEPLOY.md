# Render Deployment (Docker)

## What was added
- `Dockerfile` — multi-stage build using Eclipse Temurin **JDK 17** (matches `<java.version>17</java.version>` in `pom.xml`) to build, and Temurin **17-jre** for the runtime image.
- `.dockerignore` — keeps the build context small (excludes `target/`, IDE files, git, envs, docs).

## Build strategy
1. **Stage 1 (build):** copies `pom.xml` + Maven wrapper first for dependency caching, then copies `src/` and runs `./mvnw -DskipTests package`. Falls back to installing `maven` if no wrapper is present. Resulting JAR is renamed to `app.jar`.
2. **Stage 2 (runtime):** JRE-only image, non-root `spring` user, single `COPY` of `app.jar`, started via `java -jar /app/app.jar`.

## Port
`application.properties` already contains:
```
server.port=${PORT:8081}
```
No change required. Render injects `PORT` at runtime; the app binds to it automatically. The Dockerfile documents `EXPOSE 8081` for local runs.

## Deploying on Render
1. Push this backend repo to GitHub.
2. In Render → **New → Web Service** → connect the repo.
3. Environment: **Docker** (auto-detected from the `Dockerfile` at repo root). No build/start command needed.
4. Add environment variables under **Environment**:
   - `DB_URL`, `DB_USER`, `DB_PASSWORD` (Supabase Postgres)
   - `JWT_SECRET`, `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`
   - `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_EMAIL_VERIFY`
   - Any mail / admin / CORS vars used by `application-prod.properties`
   - `SPRING_PROFILES_ACTIVE=prod` (if you want the prod profile)
   - Optional: `JAVA_OPTS=-XX:MaxRAMPercentage=75`
5. Deploy. Render will build the image, run it, and route external traffic to the container's `PORT`.

## Local test
```
docker build -t kr-backend .
docker run --rm -p 8081:8081 --env-file .env kr-backend
```

## Notes
- No application code, security, or Spring configuration was modified.
- No Gradle files exist in the project; the Dockerfile is Maven-only by design.
- Runtime image runs as a non-root user for a smaller attack surface.
