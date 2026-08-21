# Tuần 3 - Ngày 1: Thiết kế Business Rules

## 1. Mục tiêu

Business Rules kiểm tra một hồ sơ có đáp ứng điều kiện mở sản phẩm hay không. Kết quả cuối cùng cần cho biết hồ sơ có đủ điều kiện (`eligible`) hay không, rule nào đã chạy và lý do pass/fail.

Ngày 1 chỉ tạo nền tảng model và service orchestration. Chưa có endpoint evaluate-rules, chưa thay đổi database và chưa implement rule cụ thể.

## 2. Luồng dự kiến

```text
AccountApplication
        ↓
CIF/KYC Verification
        ↓
ApplicationRuleEvaluationService
        ↓
ApplicationRule[]
        ↓
RuleResult[]
        ↓
RuleEvaluationResult
        ↓
Eligible / Not Eligible
```

Hiện endpoint KYC thực tế là:

```http
POST /api/applications/{applicationId}/kyc-check
```

Nó không phải `/verify-kyc`.

## 3. Danh sách rule và dữ liệu hiện có

| Rule | Ý nghĩa | Có đủ dữ liệu hiện tại? | Lý do |
|---|---|---|---|
| `PRODUCT_ACTIVE` | Product của hồ sơ còn hoạt động | READY | `AccountApplication` có `productCode`; `Product` và `ProductRepository` có cờ `active` |
| `KYC_VERIFIED` | Hồ sơ đã có kết quả CIF/KYC pass được xác nhận trước đó | NOT READY | Entity có `kycStatus` và `cifVerifiedAt`, nhưng `verifyCifKyc` hiện chỉ trả response, không lưu chúng |
| `DUPLICATE_PRODUCT` | Customer chưa sở hữu/đã mở cùng product | NOT READY | Chưa có bảng/repository `bank_accounts` hoặc nguồn dữ liệu ownership; `account_applications` chỉ là hồ sơ, không chứng minh tài khoản đã mở |
| `AGE_REQUIREMENT` | Customer đạt tuổi tối thiểu của product | NOT READY | `Product` có `minAge` và upstream response có `dateOfBirth`, nhưng DOB chưa được lưu hoặc đưa vào kết quả verification để rule engine dùng lại |

`Product` chưa có `maxAge`; vì vậy khi implement chỉ nên áp dụng minimum age trừ khi yêu cầu mới bổ sung dữ liệu/quy tắc maximum age.

## 4. Kiến trúc đã tạo

Package:

```text
application/rule/
├── ApplicationRule.java
├── ApplicationRuleCode.java
├── RuleResult.java
├── RuleEvaluationResult.java
└── ApplicationRuleEvaluationService.java
```

### `ApplicationRule`

Interface chung cho từng rule sau này:

```java
RuleResult evaluate(AccountApplication application);
```

Mỗi rule cụ thể sẽ là một class riêng. Không gom tất cả `if/else` vào một method lớn.

### `ApplicationRuleCode`

Enum tập trung các mã rule: `PRODUCT_ACTIVE`, `KYC_VERIFIED`, `DUPLICATE_PRODUCT`, `AGE_REQUIREMENT`.

### `RuleResult`

Kết quả của một rule gồm:

- `ruleCode`: rule nào chạy.
- `passed`: pass hay fail.
- `message`: lý do dễ đọc.

### `RuleEvaluationResult`

Tổng hợp toàn bộ kết quả:

- `eligible`: chỉ `true` khi có ít nhất một rule và tất cả rule đều pass.
- `ruleResults`: toàn bộ kết quả.
- `failedRules`: chỉ các rule fail.

Không có rule được đăng ký sẽ cho `eligible=false`. Đây là lựa chọn an toàn để skeleton không vô tình coi một hồ sơ là đủ điều kiện khi chưa có rule thật.

### `ApplicationRuleEvaluationService`

Spring inject danh sách các `ApplicationRule`, chạy từng rule và tạo `RuleEvaluationResult`. Service chưa được gọi từ `ApplicationService` hoặc controller trong Ngày 1, vì chưa có rule cụ thể và chưa có API evaluate-rules.

## 5. Phân chia trách nhiệm với CIF/KYC

`CifKycVerificationService` chịu trách nhiệm gọi upstream qua `CifKycClient` và kiểm tra customer tồn tại, customer `ACTIVE`, KYC `VERIFIED` và expiry date.

`ApplicationRuleEvaluationService` không gọi lại CIF/KYC và không copy các điều kiện trên. Rule `KYC_VERIFIED` sau này chỉ đọc kết quả verification đã được lưu/xác nhận, thay vì duplicate logic integration.

## 6. Dữ liệu còn thiếu và hướng xử lý sau này

- **KYC_VERIFIED:** cần quyết định thời điểm và cách lưu kết quả KYC thành công, ví dụ cập nhật `kycStatus`/`cifVerifiedAt` hiện có sau một verification pass. Việc này chưa làm trong Ngày 1.
- **DUPLICATE_PRODUCT:** cần nguồn dữ liệu về tài khoản đã mở, có thể là bảng/repository `bank_accounts` hoặc Core Banking Mock Service ở giai đoạn phù hợp. Không dùng customer hard-code hoặc application cũ để giả lập ownership.
- **AGE_REQUIREMENT:** cần truyền DOB đã xác nhận từ CIF/KYC vào input của rule hoặc lưu một snapshot phù hợp. Không gọi lại upstream để tự tính lại KYC trong rule engine.

## 7. Kế hoạch Ngày 2

- Chốt input contract cho evaluation, đặc biệt là kết quả KYC đã xác nhận.
- Implement các rule có dữ liệu đủ và đã được phê duyệt scope, bắt đầu với `PRODUCT_ACTIVE`.
- Bổ sung test cho từng rule thật.
- Chỉ thêm database/migration khi có yêu cầu nghiệp vụ rõ ràng, không tạo trước để đoán.
