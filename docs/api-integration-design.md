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
| `POST /api/applications/{id}/process` | Mandatory fail: 409 và giữ SUBMITTED; pass + no review: APPROVED; pass + review signal: UNDER_REVIEW. |
| `POST /api/applications/{id}/create-account` | APPROVED-only provisioning; success returns COMPLETED and account reference. |
| `POST /api/applications/{id}/retry-account-creation` | Manual retry từ RETRY_PENDING; reuse operation/key và cumulative attempt count. |
| `GET /api/applications/{id}/integration-requests` | Read-only integration tracking. |
| `GET /api/health`, `/actuator/health`, `/swagger-ui.html`, `/v3/api-docs` | Operational/OpenAPI endpoints. |

## 3. Approval APIs

| Endpoint | Purpose |
|---|---|
| `GET /api/approval-cases` | List cases; optional `status` and `assignedTo` filters. |
| `GET /api/approval-cases/{caseId}` | Get case and application summary. |
| `PATCH /api/approval-cases/{caseId}/assign` | Assign a PENDING case using required `staffId`. |
| `POST /api/approval-cases/{caseId}/approve` | Assigned staff approves; current rules must pass. |
| `POST /api/approval-cases/{caseId}/reject` | Assigned staff rejects with required reason. |

Approval lifecycle is `PENDING → ASSIGNED → APPROVED/REJECTED`. Terminal decisions cannot be changed.

## 4. CIF/KYC Mock Integration

IMPLEMENTED: call `GET /api/customers/{customerId}`. Require customer ACTIVE, KYC VERIFIED và current expiry; persist `kycStatus`, `cifVerifiedAt`, `kycExpiryDate`, `reviewRequired`, `reviewReason`.

Contract bổ sung:

```json
{"reviewRequired": true, "reviewReason": "CUSTOMER_PROFILE_REVIEW"}
```

`reviewRequired=false` bắt buộc reason null; true bắt buộc reason có giá trị enum. Thiếu field, combination sai, enum lạ, empty response, HTTP bất thường hoặc connection failure đều là technical/data-contract error `503 CIF_KYC_SERVICE_UNAVAILABLE`. Customer absent/inactive, KYC unverified/expired là business error và không lưu success snapshot.

## 5. Core Banking Mock Integration

IMPLEMENTED: main calls `POST http://localhost:8082/api/accounts` with `Idempotency-Key: CREATE_ACCOUNT:<applicationId>`. First create (201) và idempotent replay (200) đều là success. Core Banking binds one key/application/payload to one account; same-key replay returns the existing result, including legacy row recovery.

Main chỉ automatic retry connection/I/O, HTTP 429 và 5xx, với bounded attempts/backoff có cấu hình. HTTP/backoff chạy ngoài DB transaction. HTTP 4xx (gồm 409 idempotency conflict) là definitive failure: application và integration request cùng chuyển `FAILED`. Response 2xx malformed/incomplete là ambiguous result nên không tự retry nhưng chuyển cả hai sang `RETRY_PENDING` để manual recovery bằng cùng record/key. Retry exhaustion hoặc interrupted backoff cũng lưu `RETRY_PENDING`; interrupt flag được restore. Create command đã hoàn tất là idempotent tại local và không gọi Core Banking lại.

## 6. Error Catalog

| HTTP | Code | Meaning |
|---|---|---|
| 400 | `VALIDATION_ERROR`, `INVALID_UUID` | Invalid input/path. |
| 404 | `PRODUCT_NOT_FOUND`, `APPLICATION_NOT_FOUND`, `CUSTOMER_NOT_FOUND` | Resource absent. |
| 409 | `PRODUCT_INACTIVE`, application operation guards, KYC/rule/process/workflow guards, `CUSTOMER_NOT_ACTIVE`, `KYC_NOT_VERIFIED`, `KYC_EXPIRED` | Business/state conflict. |
| 409 | `APPLICATION_MANDATORY_CONDITIONS_NOT_SATISFIED` | Mandatory rule fail; giữ SUBMITTED và không tạo case. |
| 409 | `MANUAL_REVIEW_DATA_INVALID` | Snapshot/case manual-review không thỏa invariant. |
| 404 | `APPROVAL_CASE_NOT_FOUND` | Approval case absent. |
| 409 | `APPROVAL_CASE_ALREADY_EXISTS`, `APPROVAL_CASE_ASSIGNMENT_NOT_ALLOWED`, `APPROVAL_CASE_DECISION_NOT_ALLOWED`, `APPROVAL_CASE_STAFF_MISMATCH`, `APPROVAL_CASE_BUSINESS_RULES_NOT_SATISFIED`, `APPLICATION_NOT_UNDER_REVIEW` | Approval invariant/state conflict. |
| 503 | `CIF_KYC_SERVICE_UNAVAILABLE` | Technical integration/data-contract failure. |
| 409 | `ACCOUNT_CREATION_NOT_ALLOWED`, `BANK_ACCOUNT_ALREADY_EXISTS` | Provisioning state/duplicate conflict. |
| 503 | `CORE_BANKING_SERVICE_UNAVAILABLE` | Core Banking technical/data-contract failure. |
| 503 | `CORE_BANKING_RESPONSE_INVALID` | Core Banking trả success response không đủ dữ liệu để xác nhận kết quả; application có thể manual retry. |
| 409 | `CORE_BANKING_REQUEST_REJECTED` | Core Banking từ chối request dứt khoát; application và tracking chuyển FAILED. |
| 409 | `ACCOUNT_CREATION_IN_PROGRESS`, `ACCOUNT_CREATION_RETRY_NOT_ALLOWED` | Concurrent command hoặc invalid retry state. |
| 409 | `CORE_BANKING_IDEMPOTENCY_CONFLICT` | Key/application/payload conflict; không retry. |
| 503 | `CORE_BANKING_RESPONSE_INVALID` | Non-retryable upstream request/data-contract failure. |
