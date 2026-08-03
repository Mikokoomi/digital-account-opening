# Digital Account Opening Application and Approval Workflow

Spring Boot service skeleton for a digital account-opening and approval workflow.

## Prerequisites

- Java 21
- Maven 3.9 or newer
- PostgreSQL (for running the application with the default `local` profile)

## Run locally

1. Create a PostgreSQL database.
2. Set the required database environment variables:

   ```powershell
   $env:DB_URL="jdbc:postgresql://localhost:5432/account_opening"
   $env:DB_USERNAME="your_database_user"
   $env:DB_PASSWORD="your_database_password"
   ```

3. Compile and start the service:

   ```powershell
   mvn compile
   mvn spring-boot:run
   ```

If Maven is not installed globally, this repository includes Maven Wrapper files. On Windows paths containing non-ASCII characters can prevent `mvnw.cmd` from launching correctly; use this equivalent PowerShell command from the project root instead:

```powershell
java "-Dmaven.multiModuleProjectDirectory=$PWD" -classpath ".mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain compile
```

The default active profile is `local`. To select it explicitly, add `--spring.profiles.active=local` to the run command.

## Endpoints

- `GET /api/health` — application health response.
- `GET /actuator/health` — Spring Boot Actuator health endpoint.
- `/swagger-ui.html` — Swagger UI.
- `/v3/api-docs` — OpenAPI definition.

## Configuration

Shared configuration is in `src/main/resources/application.yml`. Copy `src/main/resources/application-local.example.yml` to `src/main/resources/application-local.yml` for local development. Database connection details are required through environment variables; credentials are not stored in source code.

## Database migrations

Flyway is enabled for the local profile. Add future migration scripts under `src/main/resources/db/migration` (for example, `V1__initial_schema.sql`). No business entities or migrations are included in this initial skeleton.
