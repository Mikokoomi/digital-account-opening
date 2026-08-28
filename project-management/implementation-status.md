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
- Staff Approval Tuần 5: một ApprovalCase/application; `PENDING → ASSIGNED → APPROVED/REJECTED`; list/detail/filter, assignment và staff decision APIs.
- Staff decision đồng bộ ApprovalCase + application + status history trong transaction; approve yêu cầu các business rule hiện hành vẫn pass.
- PostgreSQL/Flyway V1–V7, health, actuator, Swagger/OpenAPI, structured error response.

## PLANNED

- Core Banking, bank account, retry/idempotency, integration tracking, notification, audit, scheduler.
- Advanced rules cần dữ liệu đáng tin cậy: age/DOB policy, ownership duplicate, customer risk.
- `VALIDATING`, `ACCOUNT_CREATING`, `RETRY_PENDING`, `COMPLETED`.
