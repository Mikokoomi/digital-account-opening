# Trạng thái Triển khai

Đánh giá theo code trên branch hiện tại.

## IMPLEMENTED

- Đọc active products và product active theo code.
- Tạo DRAFT application, đọc, đổi product khi DRAFT, submit, cancel, process application và đọc history.
- Tất cả application API dùng UUID `applicationId`; không có `applicationNumber`.
- Lưu history cho create, submit, cancel và process transition.
- CIF/KYC HTTP client: kiểm tra customer tồn tại, `ACTIVE`, KYC `VERIFIED`, chưa hết hạn; success lưu `kycStatus`, `cifVerifiedAt`, `kycExpiryDate`.
- Rule engine chạy `PRODUCT_ACTIVE`, `KYC_VERIFIED`; evaluation read-only. Process current KYC snapshot và rule result để chuyển `SUBMITTED → APPROVED` hoặc `SUBMITTED → UNDER_REVIEW`.
- PostgreSQL/Flyway V1–V5, health, actuator, Swagger/OpenAPI, structured error response.

## PLANNED

- Staff review/assignment/approve/reject và approval case.
- Core Banking, bank account, retry/idempotency, integration tracking, notification, audit, scheduler.
- `VALIDATING`, `ACCOUNT_CREATING`, `RETRY_PENDING`, `COMPLETED`.
