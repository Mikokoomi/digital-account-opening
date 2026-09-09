# Trạng thái Triển khai

Đánh giá theo code trên branch hiện tại.

## IMPLEMENTED

- Đọc active products và product active theo code.
- Tạo DRAFT application, đọc, đổi product khi DRAFT, submit, cancel, process application và đọc history.
- Tất cả application API dùng UUID `applicationId`; không có `applicationNumber`.
- Lưu history cho create, submit, cancel và mọi process transition.
- CIF/KYC HTTP client: mandatory customer/KYC validation; success lưu KYC cùng `reviewRequired/reviewReason`; invalid contract trả technical 503.
- Rule engine chạy mandatory rules `PRODUCT_ACTIVE`, `KYC_VERIFIED`; `eligible` nghĩa mandatory conditions satisfied; evaluation read-only.
- Decision model: mandatory fail BLOCK và giữ SUBMITTED; mandatory pass + no review auto-approve; pass + review signal route UNDER_REVIEW.
- Staff Approval: một ApprovalCase/application; `PENDING → ASSIGNED → APPROVED/REJECTED`; list/detail/filter, assignment và staff decision APIs.
- Staff decision đồng bộ ApprovalCase + application + status history trong transaction; approve yêu cầu các business rule hiện hành vẫn pass.
- Core Banking HTTP integration và account provisioning `APPROVED → ACCOUNT_CREATING → COMPLETED`.
- Stable Core Banking idempotency key, replay-safe create, bounded retry cho technical failures và lost-response recovery.
- `IntegrationRequest` tracking qua Flyway V9, cumulative attempts, `RETRY_PENDING`, manual retry API và read-only tracking API.
- Reliability failure-state hardening: definitive rejection → `FAILED`; ambiguous response, retry exhaustion hoặc interrupted backoff → `RETRY_PENDING`; application và integration request luôn đồng bộ.
- Local `bank_accounts` projection qua Flyway V8; Core Banking Mock sở hữu account domain.
- PostgreSQL/Flyway V1–V9, health, actuator, Swagger/OpenAPI, structured error response.

## PLANNED

- Scheduled retry, generalized reconciliation, notification, audit.
- Advanced rules cần dữ liệu đáng tin cậy: age/DOB policy, ownership duplicate, customer risk.
- `VALIDATING`.
