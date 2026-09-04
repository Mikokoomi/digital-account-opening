# Project Design

## 1. Project Overview và Business Goal

Digital Account Opening Application and Approval Workflow hỗ trợ existing customer đã có CIF đăng ký mở thêm account product. Đây là application workflow, không phải customer-master hoặc banking platform thật.

## 2. Scope

Target scope: existing customer → ACTIVE product → create DRAFT → submit SUBMITTED → CIF/KYC Mock → product validation → business rules → auto approve/manual review/reject → approval case nếu cần → Core Banking Mock → retry/idempotency → notification → audit/status history.

## 3. Out of Scope

Customer onboarding/tạo CIF, authentication banking production, KYC OCR/biometric thật, Core Banking thật, payment/transfer/card/interest, Internet/Mobile Banking, AML engine thật và distributed infrastructure production-grade.

## 4. Actors

| Actor | Vai trò |
|---|---|
| Customer | Xem product và sử dụng application API. |
| Account Opening System | Lưu application, KYC snapshot, rules, history. |
| CIF/KYC Mock | Nguồn customer/KYC và tín hiệu manual review `reviewRequired/reviewReason`. |
| Bank Staff | Nhận, xử lý và quyết định ApprovalCase. |
| Core Banking Mock | Owner của BankAccount và account number; tạo/đọc account qua HTTP. |
| Notification, Scheduler | PLANNED. |

## 5. Business Assumptions

- Customer master thuộc CIF/KYC; database chính chỉ giữ `customerId`.
- Mỗi application có UUID `applicationId` và một requested product.
- Chỉ DRAFT update/submit; DRAFT hoặc SUBMITTED cancel.
- KYC check và rule evaluation chỉ ở SUBMITTED.
- KYC success lưu snapshot gồm KYC và tín hiệu manual review, không đổi status/history; rule evaluation read-only.
- Product/KYC là mandatory conditions, staff không được override.
- Manual review không phát sinh từ mandatory failure; nó chỉ phát sinh khi upstream trả `reviewRequired=true` kèm `reviewReason` hợp lệ.
- `reviewRequired=false` yêu cầu `reviewReason=null`; `reviewRequired=true` yêu cầu reason khác null.
- Approval và account creation là hai bước riêng. Chỉ application `APPROVED` được provision account.

## 6. Technical Assumptions và Boundaries

KYC snapshot current cần `VERIFIED`, verification timestamp, expiry không trước hôm nay và manual-review snapshot hợp lệ. Core Banking Mock sở hữu account domain; main chỉ lưu local reference. External HTTP call nằm ngoài DB transaction. Technical Core Banking failure giữ `ACCOUNT_CREATING`; retry/idempotency và integration tracking chưa được implement.
