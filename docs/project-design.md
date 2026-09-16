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
| Account Opening System | Lưu application, KYC snapshot, rules, history, audit và notification outcome. |
| CIF/KYC Mock | Nguồn customer/KYC và tín hiệu manual review `reviewRequired/reviewReason`. |
| Bank Staff | Nhận, xử lý và quyết định ApprovalCase. |
| Core Banking Mock | Owner của BankAccount và account number; tạo/đọc account qua HTTP. |
| Logging Notification Sender | IMPLEMENTED delivery abstraction, không gửi email/SMS/push thật. |
| Scheduler | OUT OF SCOPE hiện tại. |

## 5. Business Assumptions

- Customer master thuộc CIF/KYC; database chính chỉ giữ application cùng snapshot hẹp của lần verification (`customerId`, full name, date of birth, customer/KYC status và review signal), không quản lý customer master lifecycle.
- Mỗi application có UUID `applicationId` và một requested product.
- Chỉ DRAFT update/submit; DRAFT hoặc SUBMITTED cancel.
- KYC check và rule evaluation chỉ ở SUBMITTED.
- KYC success lưu customer/KYC/manual-review snapshot, không đổi status/history; rule evaluation read-only và không gọi upstream lại.
- Required customer data, customer ACTIVE, không có application cùng customer/product đang xử lý, product ACTIVE, KYC current và ownership phù hợp `allowMultipleAccounts` là mandatory conditions; staff không được override.
- Manual review không phát sinh từ mandatory failure; nó chỉ phát sinh khi upstream trả `reviewRequired=true` kèm `reviewReason` hợp lệ.
- `reviewRequired=false` yêu cầu `reviewReason=null`; `reviewRequired=true` yêu cầu reason khác null.
- Approval và account creation là hai bước riêng. Chỉ application `APPROVED` được provision account.
- Audit business action và status history có responsibility riêng; audit success cùng transaction với mutation tương ứng.
- Notification chỉ dành cho approval/rejection/account opened; delivery failure không thay đổi business outcome.

## 6. Technical Assumptions và Boundaries

KYC snapshot current cần customer profile snapshot hiện diện, customer `ACTIVE`, KYC `VERIFIED`, verification timestamp, expiry không trước hôm nay và manual-review snapshot hợp lệ. Core Banking Mock sở hữu account domain; main chỉ lưu local reference. Một account-creation operation dùng stable idempotency key và một `IntegrationRequest` qua mọi attempt. External HTTP/backoff nằm ngoài DB transaction. Chỉ lỗi network, 429 và 5xx được retry; hết lượt chuyển `RETRY_PENDING`. Notification dùng local AFTER_COMMIT event và transaction riêng; không có broker, scheduler hay provider thật.

## 7. Policy Audit

| Policy | Current behavior/evidence | Decision | Implemented behavior | Reason |
|---|---|---|---|---|
| Duplicate application | `ApplicationStatus` phân biệt processing và terminal; application có `customerId`, `productCode`, UUID. | IMPLEMENTED | Application khác cùng customer/product ở `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `ACCOUNT_CREATING` hoặc `RETRY_PENDING` làm hard rule fail. DRAFT và terminal không block. | Chặn hai workflow song song nhưng không nhầm current application hoặc lịch sử đã kết thúc. |
| Existing account same product | `products.allow_multiple_accounts` có từ V1, sample V2 cấu hình theo product; `bank_accounts` liên kết application/customer/product. | IMPLEMENTED | Nếu product không cho multiple và đã có local BankAccount của customer/product khác thì hard rule fail; product cho multiple thì pass. | Policy đã được model/schema biểu diễn rõ theo từng product. |
| Manual review trigger | CIF/KYC contract lưu `reviewRequired/reviewReason`; `/process` dùng snapshot này. `Product.requiresManualReview` chưa tham gia decision flow. | CONFIRMED | Chỉ `reviewRequired=true` với reason hợp lệ route `UNDER_REVIEW`; mandatory fail vẫn BLOCK. | Đây là trigger end-to-end đang được code/test/document xác nhận; không tự nối thêm trigger khác. |

`MinimumAge`, customer risk và ý nghĩa tương lai của `Product.requiresManualReview` vẫn **PENDING BUSINESS CONFIRMATION**; không được suy diễn thành rule/trigger.
