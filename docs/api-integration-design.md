# API & Integration Design

## 1. API Conventions và Response Format

Base URL `http://localhost:8080`; application path uses UUID `{applicationId}`. Success uses `ApiResponse`; errors use:

```json
{"success": false, "message": "...", "errorCode": "...", "timestamp": "..."}
```

## 2. Product and Application APIs

| Endpoint | Purpose |
|---|---|
| `GET /api/products`, `GET /api/products/{productCode}` | Active products. |
| `POST /api/applications`, `GET/PATCH /api/applications/{applicationId}` | Create/read/update DRAFT application. |
| `PATCH /api/applications/{id}/submit`, `/cancel` | Submit/cancel. |
| `GET /api/applications/{id}/history` | Status history. |
| `POST /api/applications/{id}/kyc-check` | SUBMITTED KYC validation/persistence. |
| `POST /api/applications/{id}/evaluate-rules` | SUBMITTED read-only evaluation. |
| `POST /api/applications/{id}/process` | SUBMITTED current-KYC process: APPROVED/UNDER_REVIEW. |
| `GET /api/health`, `/actuator/health`, `/swagger-ui.html`, `/v3/api-docs` | Operational/OpenAPI endpoints. |

Approval APIs are PLANNED.

## 3. CIF/KYC Mock Integration

IMPLEMENTED: call `GET /api/customers/{customerId}`. Require customer ACTIVE, KYC VERIFIED and current expiry; persist success snapshot. Customer absence/inactive/unverified/expired are business errors. Empty/incomplete/non-404 HTTP/timeout/connection failures are technical `503 CIF_KYC_SERVICE_UNAVAILABLE`. Retry/idempotency for KYC are not implemented.

## 4. Core Banking, Retry, Idempotency, Notification

PLANNED: APPROVED → integration_request/idempotency key → Core Banking Mock → bank_account/COMPLETED. Technical temporary failure uses controlled retry; business error/retry exhausted → FAILED. Notification failure is independent of application result.

## 5. Error Catalog

| HTTP | Code | Meaning |
|---|---|---|
| 400 | `VALIDATION_ERROR`, `INVALID_UUID` | Invalid input/path. |
| 404 | `PRODUCT_NOT_FOUND`, `APPLICATION_NOT_FOUND`, `CUSTOMER_NOT_FOUND` | Resource absent. |
| 409 | `PRODUCT_INACTIVE`, application operation guards, KYC/rule/process/workflow guards, `CUSTOMER_NOT_ACTIVE`, `KYC_NOT_VERIFIED`, `KYC_EXPIRED` | Business/state conflict. |
| 503 | `CIF_KYC_SERVICE_UNAVAILABLE` | Technical integration/data-contract failure. |
