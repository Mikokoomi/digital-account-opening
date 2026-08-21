# Tuần 3 - Ngày 2: Core Business Rules

## 1. Mục tiêu Ngày 2

Ngày 2 hoàn thiện bằng chứng KYC đã pass trên `AccountApplication` và implement hai rule có đủ dữ liệu:

- `PRODUCT_ACTIVE`.
- `KYC_VERIFIED`.

Chưa tạo endpoint evaluate-rules, chưa làm approval workflow và chưa thay đổi database.

## 2. Vấn đề KYC persistence trước khi sửa

Trước Ngày 2, luồng KYC là:

```text
KYC PASS
→ HTTP response PASS
→ database chưa biết application đã PASS
→ kycStatus = null
→ cifVerifiedAt = null
```

`AccountApplication` đã có sẵn hai field `kycStatus` kiểu `String` và `cifVerifiedAt` kiểu `OffsetDateTime`, nhưng `ApplicationService.verifyCifKyc` chưa cập nhật chúng.

## 3. Cách sửa KYC persistence

Luồng sau khi sửa:

```text
AccountApplication
→ CifKycVerificationService.verify(customerId)
→ verification PASS
→ kycStatus = VERIFIED
→ cifVerifiedAt = OffsetDateTime.now(clock)
→ save application
→ trả response
```

`verifyCifKyc` dùng `@Transactional` vì method giờ thay đổi dữ liệu. Timestamp lấy từ bean `Clock` hiện có, giúp unit test dùng thời gian cố định.

Nếu verification fail do customer blocked, KYC chưa verified, KYC expired, customer không tồn tại hoặc integration error, exception xảy ra trước bước cập nhật/save. Application không bị đánh dấu VERIFIED và application status không đổi.

KYC PASS không tạo status history vì KYC là kết quả kiểm tra, không phải chuyển `ApplicationStatus`.

## 4. PRODUCT_ACTIVE

`ProductActiveRule` implement `ApplicationRule` và được Spring quản lý bằng `@Component`.

Nguồn dữ liệu:

```text
AccountApplication.productCode
→ ProductRepository.findByProductCode(productCode)
→ Product.active
```

Kết quả:

- `active == true`: PASS, message `Product is active`.
- `active == false` hoặc `null`: FAIL, message `Product is inactive`.
- Product không tồn tại: ném `ProductNotFoundException`, nhất quán với service hiện tại.

Product không tồn tại được xem là lỗi domain/data reference, không phải một kết quả eligibility bình thường. Foreign key hiện tại cũng ngăn application hợp lệ tham chiếu product không tồn tại.

## 5. KYC_VERIFIED

`KycVerifiedRule` chỉ đọc bằng chứng đã persist trên application:

```text
kycStatus == VERIFIED
AND
cifVerifiedAt != null
→ PASS
```

Các trường hợp còn lại đều FAIL. Nếu `kycStatus=VERIFIED` nhưng timestamp null, dữ liệu được xem là chưa đủ bằng chứng và rule fail.

Rule này không gọi `CifKycClient`, không gọi `CifKycVerificationService` và không kiểm tra lại customer status, upstream KYC status hoặc expiry date. Các trách nhiệm đó vẫn nằm trong CIF/KYC verification.

## 6. AGE_REQUIREMENT

**DEFERRED**.

`Product` có `minAge` và CIF/KYC upstream có `dateOfBirth`, nhưng DOB đã verify chưa được lưu hoặc truyền vào rule evaluation. Implement ngay sẽ phải gọi upstream lần nữa, duplicate integration logic hoặc thêm DOB vào database khi chưa chốt thiết kế.

Ngày 2 không thêm field DOB và không tạo migration.

## 7. DUPLICATE_PRODUCT

**DEFERRED**.

Project chưa có bảng/repository tài khoản mà customer thực sự sở hữu và chưa tích hợp Core Banking ownership data. `account_applications` chỉ là hồ sơ đăng ký, không phải bằng chứng customer đã có tài khoản cùng product.

Vì vậy không hard-code customer và không dùng application cũ để giả lập ownership.

## 8. Tests

### KYC persistence

- KYC PASS persist `kycStatus=VERIFIED`.
- KYC PASS persist `cifVerifiedAt` theo fixed `Clock`.
- KYC business failure không save application hoặc đánh dấu VERIFIED.
- CIF/KYC integration error không save application hoặc đánh dấu VERIFIED.
- Các test null upstream field cũ tiếp tục được giữ nguyên.

### ProductActiveRule

- Active product → PASS.
- Inactive product → FAIL.
- Product không tồn tại → `ProductNotFoundException`.

### KycVerifiedRule

- `VERIFIED` và timestamp khác null → PASS.
- Status khác `VERIFIED` → FAIL.
- `VERIFIED` nhưng timestamp null → FAIL.
- Status null → FAIL.

### ApplicationRuleEvaluationService

- PRODUCT_ACTIVE PASS + KYC_VERIFIED PASS → eligible true.
- PRODUCT_ACTIVE PASS + KYC_VERIFIED FAIL → eligible false, failed rule là KYC_VERIFIED.

## 9. Phạm vi chưa làm

- Endpoint `POST /api/applications/{id}/evaluate-rules`.
- `AGE_REQUIREMENT` khi chưa có verified DOB trong evaluation input.
- `DUPLICATE_PRODUCT` khi chưa có ownership data.
- Auto approve, approval case hoặc thay đổi workflow status.
- Core Banking integration.
