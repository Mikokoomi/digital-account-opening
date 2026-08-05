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
→ Tạo hồ sơ mở tài khoản
→ Kiểm tra CIF/KYC
→ Kiểm tra điều kiện sản phẩm
→ Tự động duyệt hoặc chuyển nhân viên xử lý
→ Gọi Core Banking giả lập để tạo tài khoản
→ Gửi thông báo và lưu audit log
```

Hiện tại project mới hoàn thành phần tạo hồ sơ, cập nhật hồ sơ và submit hồ sơ. Các bước CIF/KYC, approval và Core Banking sẽ được thực hiện ở các tuần tiếp theo.

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

### Tuần 2 – Ngày 1

- Tạo bảng `account_applications`
- Tạo bảng `application_status_history`
- Tạo entity `AccountApplication`
- Tạo entity `ApplicationStatusHistory`
- Tạo enum `ApplicationStatus`
- Kiểm tra Flyway migration và Hibernate schema validation

### Tuần 2 – Ngày 2

- Tạo API tạo hồ sơ mở tài khoản
- Tạo API xem chi tiết hồ sơ
- Kiểm tra dữ liệu request
- Kiểm tra sản phẩm tồn tại và đang hoạt động
- Hồ sơ mới có trạng thái `DRAFT`
- Tạo lịch sử trạng thái đầu tiên `null → DRAFT`
- Viết unit test cho service và controller

### Tuần 2 – Ngày 3

- Tạo API cập nhật hồ sơ
- Chỉ cho cập nhật hồ sơ khi trạng thái là `DRAFT`
- Chỉ cho phép thay đổi `productCode`
- Tạo API submit hồ sơ
- Chuyển trạng thái từ `DRAFT → SUBMITTED`
- Gán thời gian `submittedAt`
- Lưu lịch sử trạng thái `DRAFT → SUBMITTED`
- Viết test cho chức năng update và submit

### Tuần 2 – Ngày 4

- Tạo API hủy hồ sơ
- Cho phép hủy hồ sơ ở trạng thái `DRAFT` hoặc `SUBMITTED`
- Chuyển trạng thái sang `CANCELLED`
- Gán thời gian `cancelledAt`
- Lưu lịch sử chuyển trạng thái sang `CANCELLED`
- Tạo API xem lịch sử trạng thái hồ sơ
- Trả lịch sử theo thứ tự thời gian tăng dần
- Viết test cho chức năng cancel và history

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

Hiện tại project sử dụng các luồng `DRAFT → SUBMITTED`, `DRAFT → CANCELLED` và `SUBMITTED → CANCELLED`.

## Database migration

- `V1__create_products_table.sql`: Tạo bảng sản phẩm.
- `V2__insert_sample_products.sql`: Thêm dữ liệu sản phẩm mẫu.
- `V3__create_account_applications.sql`: Tạo bảng hồ sơ mở tài khoản và liên kết sản phẩm.
- `V4__create_application_status_history.sql`: Tạo bảng lưu lịch sử thay đổi trạng thái hồ sơ.

## Các bảng chính hiện tại

`products`

Lưu thông tin các sản phẩm tài khoản ngân hàng.

`account_applications`

Lưu hồ sơ mở tài khoản của khách hàng.

`application_status_history`

Lưu lịch sử thay đổi trạng thái của từng hồ sơ.

Project không có bảng `customers` vì thông tin khách hàng thuộc CIF/KYC Mock Service. Hệ thống chính chỉ lưu `customerId` để tham chiếu.

## Cách chạy project

Điều kiện cần có:

- Java 21
- PostgreSQL

Tạo database có tên `account_opening`, sau đó copy file cấu hình local:

```powershell
Copy-Item src\main\resources\application-local.example.yml src\main\resources\application-local.yml
```

Thiết lập biến môi trường trong PowerShell hiện tại:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/account_opening"
$env:DB_USERNAME="your_database_user"
$env:DB_PASSWORD="your_database_password"
```

Chạy test và khởi động ứng dụng bằng Maven Wrapper:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Sau khi chạy thành công, Swagger UI có thể truy cập tại:

```text
http://localhost:8080/swagger-ui.html
```

## Phạm vi project

### Có thực hiện

- Khách hàng hiện hữu mở thêm tài khoản
- Quản lý hồ sơ mở tài khoản
- Kiểm tra sản phẩm
- CIF/KYC Mock Service
- Approval workflow
- Core Banking Mock Service
- Retry
- Notification
- Audit log

Các phần CIF/KYC, approval, Core Banking, retry, notification và audit log thuộc phạm vi project nhưng chưa được triển khai ở giai đoạn hiện tại.

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

## Bước tiếp theo

Tuần 2 – Ngày 5:

- Rà soát lại toàn bộ chức năng quản lý hồ sơ
- Bổ sung các test còn thiếu
- Kiểm tra Swagger và PostgreSQL
- Chuẩn bị kịch bản demo và báo cáo Tuần 2
