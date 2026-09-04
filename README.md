# Digital Account Opening Application and Approval Workflow

Backend project mô phỏng quy trình khách hàng hiện hữu đăng ký mở thêm một sản phẩm tài khoản ngân hàng. Hệ thống quản lý hồ sơ mở tài khoản, xác minh CIF/KYC, đánh giá điều kiện bắt buộc và điều phối phê duyệt thủ công khi cần.

## Công nghệ sử dụng

- Java 21
- Spring Boot 3.5.x
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven
- Swagger / OpenAPI
- JUnit
- Mockito

## Chức năng hiện có

### Product

- Xem danh sách sản phẩm đang active.
- Xem chi tiết sản phẩm theo `productCode`.

### Account Application

- Tạo application ở trạng thái `DRAFT`.
- Xem application và cập nhật `productCode` khi còn `DRAFT`.
- Submit `DRAFT → SUBMITTED`.
- Cancel application ở trạng thái được phép.
- Lưu và truy vấn application status history.

### CIF/KYC Integration

- Gọi CIF/KYC Mock Service qua HTTP.
- Chỉ xác minh thành công khi customer tồn tại, `ACTIVE`, KYC `VERIFIED` và KYC chưa hết hạn.
- Lưu KYC snapshot gồm `kycStatus`, `cifVerifiedAt`, `kycExpiryDate`, `reviewRequired` và `reviewReason`.
- Phân biệt business failure của customer với technical hoặc upstream data-contract failure.

### Business Rules

- `PRODUCT_ACTIVE`
- `KYC_VERIFIED`

`eligible=true` chỉ có nghĩa các mandatory business conditions đã thỏa mãn. Giá trị này không quyết định manual review.

### Application Processing

```text
SUBMITTED
   ↓
Mandatory conditions
   ├── FAIL
   │     → HTTP 409
   │     → giữ SUBMITTED
   │     → không ApprovalCase
   │
   └── PASS
         ↓
     reviewRequired?
       ├── false → APPROVED
       └── true  → UNDER_REVIEW → ApprovalCase
```

### Staff Approval

- Mỗi application tối đa có một `ApprovalCase`.
- Case lifecycle: `PENDING → ASSIGNED → APPROVED/REJECTED`.
- Hỗ trợ xem danh sách, xem chi tiết, lọc case, assign staff, approve và reject.
- Chỉ staff đã được assign mới có thể quyết định case.
- Staff approve đánh giá lại mandatory rules; staff không thể override Product/KYC mandatory conditions.
- Staff reject yêu cầu reason không rỗng.
- Application, ApprovalCase và status history được cập nhật trong cùng transaction.

## Luồng nghiệp vụ

```text
Existing Customer
        ↓
Select Product
        ↓
DRAFT
        ↓
SUBMITTED
        ↓
CIF/KYC Check
        ↓
Mandatory Rules
        ↓
   ┌────┴───────────────┐
   │                    │
 Fail                  Pass
   │                    │
SUBMITTED        reviewRequired?
                    /       \
                 false       true
                   ↓           ↓
               APPROVED   UNDER_REVIEW
                              ↓
                         ApprovalCase
                              ↓
                        Staff Decision
                         /           \
                    APPROVED      REJECTED
```

## API hiện có

| Method | Endpoint | Chức năng |
|---|---|---|
| GET | `/api/products` | Xem danh sách sản phẩm active |
| GET | `/api/products/{productCode}` | Xem chi tiết sản phẩm |
| POST | `/api/applications` | Tạo application `DRAFT` |
| GET | `/api/applications/{applicationId}` | Xem chi tiết application |
| PATCH | `/api/applications/{applicationId}` | Cập nhật `productCode` khi `DRAFT` |
| PATCH | `/api/applications/{applicationId}/submit` | Submit application |
| PATCH | `/api/applications/{applicationId}/cancel` | Cancel application ở trạng thái hợp lệ |
| GET | `/api/applications/{applicationId}/history` | Xem status history |
| POST | `/api/applications/{applicationId}/kyc-check` | Kiểm tra CIF/KYC cho application `SUBMITTED` |
| POST | `/api/applications/{applicationId}/evaluate-rules` | Đánh giá mandatory rules, không thay đổi dữ liệu |
| POST | `/api/applications/{applicationId}/process` | Process application theo KYC snapshot, rules và review signal |
| GET | `/api/approval-cases` | Danh sách case; lọc tùy chọn theo `status`, `assignedTo` |
| GET | `/api/approval-cases/{caseId}` | Xem chi tiết approval case |
| PATCH | `/api/approval-cases/{caseId}/assign` | Assign case `PENDING` cho staff |
| POST | `/api/approval-cases/{caseId}/approve` | Staff được assign approve case |
| POST | `/api/approval-cases/{caseId}/reject` | Staff được assign reject case với reason |
| GET | `/api/health` | Kiểm tra trạng thái cơ bản của ứng dụng |
| GET | `/actuator/health` | Spring Boot Actuator health |
| GET | `/v3/api-docs` | OpenAPI specification |
| GET | `/swagger-ui.html` | Swagger UI |

## Application Status

`ApplicationStatus` hiện có:

- `DRAFT`: application mới tạo, có thể cập nhật hoặc submit.
- `SUBMITTED`: application đã gửi; có thể KYC check, evaluate rules hoặc process.
- `UNDER_REVIEW`: application đang được staff xử lý qua ApprovalCase.
- `APPROVED`: application được duyệt.
- `REJECTED`: application bị staff từ chối.
- `CANCELLED`: application bị hủy.
- `FAILED`: trạng thái lỗi đã được định nghĩa trong enum.

Các transition đang được sử dụng gồm `DRAFT → SUBMITTED`, `DRAFT/SUBMITTED → CANCELLED`, `SUBMITTED → APPROVED`, `SUBMITTED → UNDER_REVIEW`, và `UNDER_REVIEW → APPROVED/REJECTED`. Mandatory failure khi process giữ application ở `SUBMITTED`.

## Database

| Bảng | Trách nhiệm |
|---|---|
| `products` | Lưu sản phẩm tài khoản. |
| `account_applications` | Lưu application, KYC snapshot và manual-review snapshot. |
| `application_status_history` | Lưu lịch sử chuyển trạng thái của application. |
| `approval_cases` | Lưu manual-review case, assignment và quyết định của staff. |

`approval_cases.application_id` có ràng buộc `UNIQUE`, nên mỗi application tối đa có một ApprovalCase.

Manual-review snapshot nằm tại `account_applications`:

- `review_required`
- `review_reason`

CHECK constraint của V7 bảo đảm:

- `review_required = true` yêu cầu `review_reason` khác null.
- `review_required = false` yêu cầu `review_reason` null.
- `review_required = null` yêu cầu `review_reason` null, để tương thích record cũ chưa có snapshot.

Project không có bảng `customers`; customer master data thuộc CIF/KYC Mock Service và application chỉ lưu `customerId` để tham chiếu.

## Flyway Migrations

- V1: products.
- V2: sample products.
- V3: account applications.
- V4: application status history.
- V5: KYC expiry snapshot.
- V6: approval cases.
- V7: manual-review snapshot.

## CIF/KYC Mock Service

Source code: [Mikokoomi/cif-kyc-mock-service](https://github.com/Mikokoomi/cif-kyc-mock-service)

Mock service chạy tại port `8081` và cung cấp:

```http
GET /api/customers/{customerId}
```

Main service đọc `reviewRequired/reviewReason` từ response để tách manual review khỏi mandatory rule failure.

## Cách chạy

Yêu cầu: Java 21 và PostgreSQL với database `account_opening`.

### Khởi động CIF/KYC Mock Service

```powershell
git clone https://github.com/Mikokoomi/cif-kyc-mock-service.git
Set-Location .\cif-kyc-mock-service
.\mvnw.cmd spring-boot:run
```

### Khởi động Account Opening Service

Copy cấu hình mẫu local:

```powershell
Copy-Item src\main\resources\application-local.example.yml src\main\resources\application-local.yml
```

Thiết lập biến môi trường trong PowerShell hiện tại. Không ghi password thật vào source code hoặc file cấu hình.

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/account_opening"
$env:DB_USERNAME="your_database_user"
$securePassword = Read-Host "Nhập mật khẩu PostgreSQL" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
Remove-Variable securePassword
```

Đảm bảo base URL của mock service là `http://localhost:8081` trong `application-local.yml`, sau đó chạy:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Testing

Chạy regression suite:

```powershell
.\mvnw.cmd clean test
```

Current regression suite: 136 tests passing.

## Phạm vi hiện tại

Implemented: existing customer account opening, product validation, account application, CIF/KYC integration, mandatory business rules, processing decision, manual review, ApprovalCase, staff approve/reject và status history.

Currently not implemented: Core Banking account creation, retry/idempotency, integration tracking, notification và audit.

## Quyết định kỹ thuật

- Customer master data ở CIF/KYC Mock Service, không duplicate trong main database.
- Application dùng UUID để định danh độc lập với sequence database.
- Enum được lưu dạng string để dữ liệu dễ đọc và không phụ thuộc thứ tự enum.
- Status history liên kết `ManyToOne` LAZY với application để chỉ tải application khi cần.
- Application state change và history được ghi trong cùng transaction để tránh dữ liệu không nhất quán.

## Documentation

Kiến trúc, phạm vi, workflow, API và trạng thái triển khai được duy trì tại `docs/README.md`.
