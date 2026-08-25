# Documentation

Thư mục này là baseline tài liệu chính thức. Nó tách biệt rõ implementation hiện tại trên `main` và thiết kế mục tiêu.

## Nguồn sự thật

Khi có mâu thuẫn, ưu tiên: code production trên branch đang làm việc, automated tests, Flyway/JPA, quyết định đã được triển khai, thiết kế mục tiêu, rồi đến tài liệu cũ.

## System Design

1. [project-design.md](project-design.md) — Scope, actors, assumptions.
2. [workflow-design.md](workflow-design.md) — BPMN, state machine, business rules.
3. [data-design.md](data-design.md) — ERD và data model.
4. [api-integration-design.md](api-integration-design.md) — APIs, integrations, retry/idempotency, errors.

## Project Management

- [Implementation status](../project-management/implementation-status.md)
- [Decision log](../project-management/decision-log.md)

## Quy tắc tài liệu

Mỗi PR thay đổi API, application status, business rule, database schema, workflow, integration hoặc error handling phải cập nhật tài liệu tương ứng trong cùng PR.

## Xác minh local

```powershell
.\mvnw.cmd clean test
```

Khi chạy local cần `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; không commit hoặc ghi password thật vào tài liệu. CIF/KYC base URL lấy từ `integration.cif-kyc.base-url`.
