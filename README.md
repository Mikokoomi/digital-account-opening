# Digital Account Opening Application and Approval Workflow

Đây là project mô phỏng quy trình khách hàng hiện hữu đăng ký mở thêm một tài khoản ngân hàng.

Project tập trung vào phần backend, quản lý hồ sơ mở tài khoản, kiểm tra trạng thái, quy trình duyệt và tích hợp với các service giả lập.

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

## Luồng nghiệp vụ chính

```text
Khách hàng hiện hữu
→ Chọn sản phẩm tài khoản
→ Tạo hồ sơ (`DRAFT`)
→ Submit hồ sơ (`SUBMITTED`)
→ Kiểm tra CIF/KYC
→ Đánh giá mandatory Business Rules
→ Mandatory fail: BLOCK, giữ `SUBMITTED`, không tạo ApprovalCase
→ Mandatory pass + `reviewRequired=false`: `APPROVED`
→ Mandatory pass + `reviewRequired=true`: `UNDER_REVIEW` → ApprovalCase → staff decision
```

Phạm vi hiện tại: Account Application, CIF/KYC integration, Business Rules, Application Workflow và Staff Approval. Core Banking, retry, notification và audit log thuộc các tuần sau.

## Tiến độ hiện tại

### Tuần 1

- Chốt phạm vi và luồng nghiệp vụ chính
- Thiết kế trạng thái hồ sơ, API và database
- Khởi tạo project Spring Boot
- Kết nối PostgreSQL
- Cấu hình Flyway
- Tạo health API
- Tạo API xem danh sách sản phẩm
- Tạo API xem chi tiết sản phẩm

### Tuần 2 — Account Application + CIF/KYC Integration

#### Account Application

- Tạo migration, entity, repository, service và controller cho `account_applications` và `application_status_history`
- Tạo, xem chi tiết và cập nhật `productCode` của hồ sơ `DRAFT`
- Submit hồ sơ từ `DRAFT → SUBMITTED`
- Hủy hồ sơ `DRAFT` hoặc `SUBMITTED` sang `CANCELLED`
- Ghi và truy vấn lịch sử trạng thái theo thời gian tăng dần
- Validation request, kiểm tra sản phẩm tồn tại/đang hoạt động và xử lý lỗi thống nhất

#### CIF/KYC Integration

- Xây dựng CIF/KYC Mock Service độc lập chạy ở port `8081`
- Gọi HTTP bằng Spring `RestClient` thông qua `CifKycClient`
- Kiểm tra customer tồn tại, trạng thái `ACTIVE`, KYC `VERIFIED` và ngày hết hạn KYC
- Tạo endpoint `POST /api/applications/{applicationId}/kyc-check`
- Xử lý các lỗi 404, 409 và 503 theo response convention hiện có
- Dùng `Clock` để kiểm thử quy tắc hết hạn ổn định
- Xử lý upstream response thiếu dữ liệu như một integration/data-contract failure
- Hoàn thành **69 automated tests**, không có failure, error hoặc skipped test

### Tuần 3 — Business Rules

- Thiết kế `ApplicationRule`, `RuleResult`, `RuleEvaluationResult` và `ApplicationRuleEvaluationService`.
- Implement `ProductActiveRule` và `KycVerifiedRule` theo thứ tự deterministic.
- Tạo endpoint `POST /api/applications/{applicationId}/evaluate-rules` trả `eligible`, `ruleResults` và `failedRules`.
- KYC check và rule evaluation chỉ được chạy khi hồ sơ ở `SUBMITTED`.
- Persist snapshot KYC gồm `kycStatus`, `cifVerifiedAt` và `kycExpiryDate`.
- `KycVerifiedRule` chỉ pass khi KYC đã verified, có timestamp và chưa hết hạn.
- Hoàn thành **99 automated tests**, không có failure, error hoặc skipped test.

### Tuần 4 — Application Workflow

- Xây dựng `ApplicationWorkflowService` làm đầu mối kiểm soát state transition và ghi `ApplicationStatusHistory`.
- Tạo endpoint process application dựa trên KYC snapshot còn hiệu lực và kết quả business rules.
- Điều phối `SUBMITTED → APPROVED` khi toàn bộ rule pass.
- Điều phối `SUBMITTED → UNDER_REVIEW` khi hồ sơ cần manual review.
- Trả business error cho thao tác process và transition không hợp lệ.
- Bao phủ workflow, processing flow, history và error handling bằng automated tests.

Tại mốc đóng Tuần 4, hệ thống mới điều phối hồ sơ vào `UNDER_REVIEW`; phần xử lý tiếp theo được triển khai ở Tuần 5 bên dưới.

### Tuần 5 — Staff Approval

- Lưu manual-review snapshot `reviewRequired/reviewReason` từ CIF/KYC bằng migration V7.
- Tách mandatory failure, manual review và auto approval thành ba outcome riêng.
- Chỉ tạo `ApprovalCase` PENDING khi mandatory conditions pass và upstream yêu cầu review hợp lệ.
- Hỗ trợ xem danh sách/chi tiết case và lọc theo status hoặc staff được phân công.
- Thực hiện lifecycle `PENDING → ASSIGNED → APPROVED/REJECTED`.
- Staff approve/reject làm application chuyển `UNDER_REVIEW → APPROVED/REJECTED` qua `ApplicationWorkflowService` và ghi status history.
- Bảo vệ case terminal, đúng staff, application status, duplicate case và mandatory rules tại thời điểm approve; `reviewRequired=true` không cản staff approve.
- Validation request và error response 400/404/409 nhất quán với project.
- Hoàn thành **136 automated tests**, không có failure, error hoặc skipped test.

## Các API hiện có

| Method | Endpoint | Chức năng |
|---|---|---|
| GET | `/api/health` | Kiểm tra trạng thái cơ bản của ứng dụng |
| GET | `/actuator/health` | Kiểm tra health của Spring Boot Actuator |
| GET | `/api/products` | Xem danh sách sản phẩm đang hoạt động |
| GET | `/api/products/{productCode}` | Xem chi tiết một sản phẩm |
| POST | `/api/applications` | Tạo hồ sơ mở tài khoản |
| GET | `/api/applications/{applicationId}` | Xem chi tiết hồ sơ |
| PATCH | `/api/applications/{applicationId}` | Cập nhật `productCode` của hồ sơ `DRAFT` |
| PATCH | `/api/applications/{applicationId}/submit` | Submit hồ sơ từ `DRAFT` sang `SUBMITTED` |
| PATCH | `/api/applications/{applicationId}/cancel` | Hủy hồ sơ `DRAFT` hoặc `SUBMITTED` |
| GET | `/api/applications/{applicationId}/history` | Xem lịch sử thay đổi trạng thái của hồ sơ |
| POST | `/api/applications/{applicationId}/kyc-check` | Kiểm tra CIF/KYC cho hồ sơ `SUBMITTED` |
| POST | `/api/applications/{applicationId}/evaluate-rules` | Đánh giá eligibility cho hồ sơ `SUBMITTED` |
| POST | `/api/applications/{applicationId}/process` | Process hồ sơ dựa trên KYC snapshot và business rules |
| GET | `/api/approval-cases` | Xem danh sách approval case; có thể lọc `status`, `assignedTo` |
| GET | `/api/approval-cases/{caseId}` | Xem chi tiết approval case |
| PATCH | `/api/approval-cases/{caseId}/assign` | Phân công case `PENDING` cho staff |
| POST | `/api/approval-cases/{caseId}/approve` | Staff được phân công duyệt case |
| POST | `/api/approval-cases/{caseId}/reject` | Staff được phân công từ chối case với lý do |
| GET | `/v3/api-docs` | Xem OpenAPI specification |
| GET | `/swagger-ui.html` | Mở Swagger UI |

## Trạng thái hồ sơ

Các trạng thái trong enum `ApplicationStatus`:

- `DRAFT`: Hồ sơ mới tạo, khách hàng vẫn có thể chỉnh sửa
- `SUBMITTED`: Hồ sơ đã được gửi để xử lý
- `UNDER_REVIEW`: Hồ sơ đang được nhân viên kiểm tra
- `APPROVED`: Hồ sơ đã được duyệt
- `REJECTED`: Hồ sơ bị từ chối
- `CANCELLED`: Hồ sơ đã bị hủy
- `FAILED`: Hồ sơ gặp lỗi trong quá trình xử lý

Hiện tại project sử dụng các luồng `DRAFT → SUBMITTED`, `DRAFT → CANCELLED`, `SUBMITTED → CANCELLED`, `SUBMITTED → APPROVED`, `SUBMITTED → UNDER_REVIEW` và staff decision từ `UNDER_REVIEW`. `eligible` chỉ biểu thị mandatory rules pass. Mandatory fail giữ `SUBMITTED`; manual review dựa riêng vào `reviewRequired/reviewReason`. KYC check và rule evaluation chỉ chạy ở `SUBMITTED`; evaluate-rules vẫn read-only.

## Kết quả Tuần 2

Tuần 2 đã hoàn thành Account Application và CIF/KYC Integration:

- Tạo hồ sơ
- Xem chi tiết hồ sơ
- Cập nhật hồ sơ khi còn `DRAFT`
- Submit hồ sơ
- Hủy hồ sơ
- Xem lịch sử trạng thái
- Chạy CIF/KYC Mock Service độc lập
- Kiểm tra customer tồn tại và đang `ACTIVE`
- Kiểm tra KYC `VERIFIED` và chưa hết hạn
- Trả lỗi `CUSTOMER_NOT_FOUND`, `CUSTOMER_NOT_ACTIVE`, `KYC_NOT_VERIFIED`, `KYC_EXPIRED` và `CIF_KYC_SERVICE_UNAVAILABLE`
- 69 automated tests pass

## Database migration

- `V1__create_products_table.sql`: Tạo bảng sản phẩm.
- `V2__insert_sample_products.sql`: Thêm dữ liệu sản phẩm mẫu.
- `V3__create_account_applications.sql`: Tạo bảng hồ sơ mở tài khoản và liên kết sản phẩm.
- `V4__create_application_status_history.sql`: Tạo bảng lưu lịch sử thay đổi trạng thái hồ sơ.
- `V5__add_kyc_expiry_date_to_account_applications.sql`: Thêm snapshot ngày hết hạn KYC.
- `V6__create_approval_cases.sql`: Tạo ApprovalCase, unique application và indexes.
- `V7__add_manual_review_snapshot_to_account_applications.sql`: Thêm `review_required`, `review_reason` và CHECK invariant.

## Các bảng chính hiện tại

`products`

Lưu thông tin các sản phẩm tài khoản ngân hàng.

`account_applications`

Lưu hồ sơ mở tài khoản của khách hàng.

`application_status_history`

Lưu lịch sử thay đổi trạng thái của từng hồ sơ.

`approval_cases`

Lưu case manual review, staff assignment và quyết định approve/reject. Mỗi application tối đa một case.

Project không có bảng `customers` vì thông tin khách hàng thuộc CIF/KYC Mock Service. Hệ thống chính chỉ lưu `customerId` để tham chiếu.

## Cách chạy hai service

Điều kiện cần có:

- Java 21
- PostgreSQL

### CIF/KYC Mock Service

Source code: [Mikokoomi/cif-kyc-mock-service](https://github.com/Mikokoomi/cif-kyc-mock-service)

```powershell
git clone https://github.com/Mikokoomi/cif-kyc-mock-service.git
Set-Location .\cif-kyc-mock-service
.\mvnw.cmd spring-boot:run
```

Nếu đã clone CIF/KYC Mock Service trước đó thì chỉ cần mở terminal tại thư mục service và chạy Maven Wrapper.

Mock Service chạy ở `http://localhost:8081` và cung cấp:

```http
GET /api/customers/{customerId}
```

### Account Opening

Tạo database có tên `account_opening`, sau đó copy file cấu hình local:

```powershell
Copy-Item src\main\resources\application-local.example.yml src\main\resources\application-local.yml
```

Thiết lập biến môi trường trong PowerShell hiện tại:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/account_opening"
$env:DB_USERNAME="your_database_user"
$securePassword = Read-Host "Nhập mật khẩu PostgreSQL" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
Remove-Variable securePassword
```

Đảm bảo file local có URL của mock service nhưng không chứa credential thật:

```yaml
integration:
  cif-kyc:
    base-url: http://localhost:8081
```

Chạy test và khởi động ứng dụng bằng Maven Wrapper:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Sau khi chạy thành công, Swagger UI có thể truy cập tại:

```text
http://localhost:8080/swagger-ui/index.html
```

## Phạm vi project

### Có thực hiện

- Khách hàng hiện hữu mở thêm tài khoản
- Quản lý hồ sơ mở tài khoản
- Kiểm tra sản phẩm
- CIF/KYC Mock Service
- Business Rules: `PRODUCT_ACTIVE` và `KYC_VERIFIED`

Core Banking, retry, notification và audit log thuộc phạm vi project nhưng chưa được triển khai ở giai đoạn hiện tại.

### Không thực hiện

- Đăng ký khách hàng mới
- Xác thực danh tính thật
- Tích hợp hệ thống ngân hàng thật
- Frontend phức tạp
- Chức năng production thực tế

## Một số quyết định kỹ thuật

### Vì sao không có bảng customers?

Thông tin khách hàng thuộc CIF/KYC Mock Service. Project chính chỉ lưu `customerId` để tham chiếu, tránh lưu trùng dữ liệu khách hàng.

### Vì sao dùng UUID?

UUID giúp ID khó đoán hơn, không phụ thuộc vào số tăng dần của database và phù hợp khi sau này hệ thống có nhiều service.

### Vì sao enum lưu dạng string?

Trạng thái được lưu bằng chuỗi để dữ liệu dễ đọc và không bị sai khi thay đổi thứ tự các giá trị trong enum.

### Vì sao history dùng ManyToOne LAZY?

Nhiều bản ghi lịch sử thuộc về một hồ sơ. LAZY giúp chỉ tải thông tin hồ sơ khi thật sự cần, tránh truy vấn dữ liệu không cần thiết.

### Vì sao application và history dùng cùng transaction?

Khi tạo hoặc submit hồ sơ, thay đổi trạng thái và lịch sử phải được lưu cùng nhau. Nếu lưu lịch sử thất bại thì thay đổi hồ sơ cũng phải rollback để dữ liệu không bị thiếu hoặc sai.

## Roadmap cập nhật

- **Tuần 1 — Nghiệp vụ và khởi tạo:** tìm hiểu nghiệp vụ, thiết kế luồng/DB/API, khởi tạo Spring Boot, PostgreSQL, Flyway và Product API cơ bản.
- **Tuần 2 — Account Application + CIF/KYC Integration:** quản lý hồ sơ, migration, trạng thái/lịch sử, CIF/KYC Mock Service, RestClient, verification, endpoint KYC check, error handling và automated tests.
- **Tuần 3 — Business Rules:** hoàn thành architecture rule, `PRODUCT_ACTIVE`, `KYC_VERIFIED`, KYC expiry snapshot và endpoint evaluate eligibility.
- **Tuần 4 — Workflow trạng thái:** hoàn thiện luồng trạng thái và điều phối xử lý hồ sơ.
- **Tuần 5 — Nhân viên xét duyệt:** hoàn thành ApprovalCase, assignment, approve/reject và đồng bộ status history.
- **Tuần 6 — Core Banking Mock Service:** tích hợp service giả lập để tạo tài khoản ngân hàng.
- **Tuần 7 — Reliability:** retry, error handling và idempotency.
- **Tuần 8 — Notification và Audit Log:** gửi thông báo và lưu dấu vết xử lý.

Việc đổi roadmap chỉ thay đổi cách nhóm và đánh số tiến độ, không thay đổi code nghiệp vụ đã hoàn thành.

## Documentation

Kiến trúc, phạm vi, workflow, API và trạng thái triển khai của project được duy trì tại `docs/README.md`.
