# Digital Account Opening Application and Approval Workflow

This Spring Boot project models a workflow for existing bank customers opening an additional account. It focuses on the backend, application status workflow, mock-service integration points, and audit-related data. Development follows a week-by-week plan.

## Tech stack

- Java 21
- Spring Boot 3.5.x
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven
- Swagger / OpenAPI
- JUnit

## Business flow

```text
Existing customer
→ Select product
→ Create application
→ CIF/KYC verification
→ Business rules
→ Auto approval or manual review
→ Core Banking account creation
→ Notification and audit log
```

Application processing steps after product selection are planned for subsequent weeks.

## Current progress

### Week 1

- Defined project scope and main business flow
- Designed application states, APIs, and database structure
- Initialized the Spring Boot project
- Connected PostgreSQL and configured Flyway
- Added health endpoints
- Implemented product listing and product detail APIs

### Week 2 — Day 1

- Added the `account_applications` table
- Added the `application_status_history` table
- Added `AccountApplication` and `ApplicationStatusHistory` entities
- Added the `ApplicationStatus` enum
- Verified Flyway migrations and Hibernate schema validation

### Week 2 Day 2

- Implemented application creation and detail APIs
- Validated customer and product input, including inactive products
- Recorded the initial `DRAFT` status history in the same transaction as application creation
- Added service and controller tests for the application APIs

## Current endpoints

- `GET /api/health`
- `GET /actuator/health`
- `GET /api/products`
- `GET /api/products/{productCode}`
- `POST /api/applications`
- `GET /api/applications/{applicationId}`
- `GET /v3/api-docs`
- `GET /swagger-ui.html`

## Database migrations

- `V1__create_products_table.sql` creates the product catalogue.
- `V2__insert_sample_products.sql` inserts sample products.
- `V3__create_account_applications.sql` creates account-opening applications and their product reference.
- `V4__create_application_status_history.sql` creates application status history.

## Run locally

Prerequisites:

- Java 21
- PostgreSQL

Create a local database named `account_opening`, then copy the local configuration template:

```powershell
Copy-Item src\main\resources\application-local.example.yml src\main\resources\application-local.yml
```

Set database connection variables in the current PowerShell session:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/account_opening"
$env:DB_USERNAME="your_database_user"
$env:DB_PASSWORD="your_database_password"
```

Run tests and start the application with Maven Wrapper:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

After startup, Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

## Project scope

### In scope

- Existing customers opening an additional account
- Product eligibility
- CIF/KYC mock integration
- Approval workflow
- Core Banking mock integration
- Retry, notification, and audit log

### Out of scope

- New customer onboarding
- Real identity verification
- Production banking integration
- Complex frontend

## Next step

Implement application update and submit operations.
