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
| CIF/KYC Mock | External customer/KYC data source. |
| Bank Staff, Core Banking Mock, Notification, Scheduler | PLANNED. |

## 5. Business Assumptions

- Customer master thuộc CIF/KYC; database chính chỉ giữ `customerId`.
- Mỗi application có UUID `applicationId` và một requested product.
- Chỉ DRAFT update/submit; DRAFT hoặc SUBMITTED cancel.
- KYC check và rule evaluation chỉ ở SUBMITTED.
- KYC success lưu snapshot, không đổi status/history; rule evaluation read-only.

## 6. Technical Assumptions và Boundaries

KYC snapshot current cần `VERIFIED`, verification timestamp và expiry không trước hôm nay. Core Banking chỉ nhận approved application trong target design. Retry chỉ technical failure; idempotency tránh tạo account trùng; notification failure không đổi business outcome; audit cần bất biến về mặt nghiệp vụ.
