# Hướng dẫn chạy và test project

Tài liệu này giúp chạy, build và test hai project hiện tại theo cách dễ nhớ. Nội dung dựa trên source code đang có, không phải một kế hoạch giả định.

## 1. Tổng quan hệ thống

```text
PostgreSQL
     ↑
     │
Account Opening :8080
     │
     │ HTTP
     ↓
CIF/KYC Mock Service :8081
```

### Account Opening

Project chính nằm tại `C:\Users\Admin\account-opening`. Nó xử lý:

- Product.
- Account Application.
- Trạng thái và lịch sử trạng thái của hồ sơ.
- Kết nối HTTP đến CIF/KYC Mock Service.
- Kiểm tra customer, trạng thái customer, KYC và ngày hết hạn KYC.

### CIF/KYC Mock Service

Service giả lập hệ thống CIF/KYC bên ngoài nằm tại `C:\Users\Admin\cif-kyc-mock-service`.

- Chạy độc lập ở port `8081`.
- Không dùng chung source code hoặc database với Account Opening.
- Chỉ trả dữ liệu customer qua HTTP; không tự quyết định customer có đủ điều kiện mở tài khoản hay không.

Việc quyết định đủ điều kiện nằm trong `CifKycVerificationService` của Account Opening.

## 2. Những thành phần cần chạy

| Thành phần | Khi nào cần? | Vai trò |
|---|---|---|
| PostgreSQL | Luôn cần khi chạy Account Opening | Lưu product, application và history |
| CIF/KYC Mock Service | Cần khi test KYC check | Trả dữ liệu customer ở port 8081 |
| Account Opening Service | Khi test API chính | Cung cấp API ở port 8080 |

- API Product và Application cần PostgreSQL.
- API KYC check cần cả PostgreSQL lẫn CIF/KYC Mock Service.
- Nếu Mock Service tắt, KYC check phải trả HTTP `503`, không phải customer không tồn tại.

## 3. Mở terminal đúng thư mục

### Project chính

**Command:**

```powershell
cd C:\Users\Admin\account-opening
```

**Dùng để:** di chuyển terminal đến project Account Opening.

**Giải thích:** `cd` là viết tắt của *change directory*, tức là đổi thư mục đang làm việc của terminal.

### CIF/KYC Mock Service

**Command:**

```powershell
cd C:\Users\Admin\cif-kyc-mock-service
```

**Dùng để:** di chuyển terminal đến Mock Service.

**Lưu ý quan trọng:** Maven Wrapper phải được chạy trong đúng thư mục có cả `mvnw.cmd` và `pom.xml`. Nếu chạy ở thư mục khác, bạn có thể gặp lỗi không tìm thấy `mvnw.cmd` hoặc `pom.xml`.

## 4. Maven Wrapper là gì?

Trên Windows, project dùng Maven Wrapper qua lệnh:

```powershell
.\mvnw.cmd
```

**Dùng để:** chạy Maven theo cấu hình Maven Wrapper của chính project.

**Vì sao dùng Wrapper thay vì `mvn`?** Wrapper tải và dùng phiên bản Maven mà project chỉ định. Vì vậy máy không nhất thiết phải cài Maven global trước, và các thành viên trong nhóm dùng phiên bản Maven thống nhất hơn.

Ví dụ:

```powershell
.\mvnw.cmd clean test
```

nghĩa là dùng Maven Wrapper của project để làm sạch output cũ và chạy test.

## 5. Chạy CIF/KYC Mock Service

Mở **Terminal 1**.

**Command:**

```powershell
cd C:\Users\Admin\cif-kyc-mock-service
.\mvnw.cmd spring-boot:run
```

**Dùng để:** khởi động service giả lập CIF/KYC.

**Giải thích:**

- `.\mvnw.cmd`: Maven Wrapper trên Windows.
- `spring-boot:run`: Maven goal khởi động ứng dụng Spring Boot.

**Kết quả mong đợi:** terminal có dòng gần giống:

```text
Tomcat started on port 8081
Started CifKycMockServiceApplication
```

Sau đó Mock Service hoạt động tại:

```text
http://localhost:8081
```

Giữ terminal này mở khi test KYC. Để dừng service, nhấn `Ctrl + C` trong chính terminal đó.

## 6. Test Mock Service trực tiếp

Mock Service có endpoint thực tế sau:

```http
GET http://localhost:8081/api/customers/{customerId}
```

Bạn có thể nhập các URL này trên trình duyệt, Postman hoặc chạy PowerShell:

```powershell
Invoke-RestMethod "http://localhost:8081/api/customers/CUS001" | ConvertTo-Json
Invoke-RestMethod "http://localhost:8081/api/customers/CUS002" | ConvertTo-Json
Invoke-RestMethod "http://localhost:8081/api/customers/CUS003" | ConvertTo-Json
Invoke-RestMethod "http://localhost:8081/api/customers/CUS004" | ConvertTo-Json
curl.exe -i "http://localhost:8081/api/customers/CUS999"
```

| Customer | Dữ liệu trả về | Ý nghĩa khi Account Opening kiểm tra |
|---|---|---|
| `CUS001` | `ACTIVE`, `VERIFIED`, còn hạn | Hợp lệ |
| `CUS002` | `ACTIVE`, `VERIFIED`, hết hạn | KYC hết hạn |
| `CUS003` | `ACTIVE`, `PENDING` | KYC chưa verified |
| `CUS004` | `BLOCKED`, `VERIFIED` | Customer không active |
| `CUS999` | HTTP 404 | Không có customer |

Mock endpoint chỉ trả dữ liệu `customerId`, tên, ngày sinh, customer status, KYC status và KYC expiry date. Nó không tự trả `eligible=true/false`; kết quả đó do Account Opening tính.

## 7. Chạy Account Opening

Trước khi chạy, PostgreSQL phải đang hoạt động và database `account_opening` phải tồn tại. Mở **Terminal 2**.

**Command:**

```powershell
cd C:\Users\Admin\account-opening

$env:DB_URL = "jdbc:postgresql://localhost:5432/account_opening"
$env:DB_USERNAME = "postgres"
$securePassword = Read-Host "Nhap mat khau PostgreSQL" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
Remove-Variable securePassword

.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

**Dùng để:** chạy Account Opening với cấu hình local trên máy.

**Giải thích:**

- `spring-boot:run`: chạy Spring Boot.
- `-Dspring-boot.run.profiles=local`: yêu cầu Spring dùng profile `local`. Hiện `application.yml` cũng đặt profile mặc định là `local`, nhưng ghi rõ profile giúp dễ hiểu khi demo.
- `Read-Host -AsSecureString`: yêu cầu nhập mật khẩu trực tiếp trong terminal và không hiện ký tự trên màn hình.
- `Remove-Variable securePassword`: xóa biến SecureString sau khi đã dùng trong phiên terminal.

**Kết quả mong đợi:** terminal có các dòng gần giống:

```text
HikariPool-1 - Start completed
Tomcat started on port 8080
Started AccountOpeningApplication
```

Service hoạt động tại:

```text
http://localhost:8080
```

Không in `$env:DB_PASSWORD` ra terminal. Không đưa mật khẩu thật vào source code, README hay file cấu hình được commit.

## 8. `application-local.yml` dùng để làm gì?

File local của máy developer là:

```text
src/main/resources/application-local.yml
```

Nó chứa cấu hình chỉ dùng trên máy local. Đặc biệt URL của Mock Service là:

```yaml
integration:
  cif-kyc:
    base-url: http://localhost:8081
```

**Ý nghĩa:** Java code không hard-code địa chỉ CIF/KYC. `CifKycClient` lấy base URL từ configuration, nên khi đổi môi trường chỉ cần đổi cấu hình.

`application-local.yml` được `.gitignore` và không được commit. File mẫu được phép commit là:

```text
src/main/resources/application-local.example.yml
```

Không ghi password thật vào bất kỳ file documentation nào.

## 9. Chạy automated tests của Account Opening

**Command:**

```powershell
cd C:\Users\Admin\account-opening
.\mvnw.cmd clean test
```

**Dùng để:** compile code, compile test và chạy automated tests của Account Opening.

**Giải thích:**

- `clean`: xóa output build cũ trong `target/`, giúp test chạy trên build sạch.
- `test`: compile code/test và chạy test tự động.

**Kết quả mong đợi hiện tại:**

```text
Tests run: 69
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Số test có thể tăng ở các tuần sau; quan trọng là không có failure/error và Maven báo `BUILD SUCCESS`.

## 10. Chạy test của CIF/KYC Mock Service

**Command:**

```powershell
cd C:\Users\Admin\cif-kyc-mock-service
.\mvnw.cmd clean test
```

**Dùng để:** chạy Maven test lifecycle của Mock Service.

**Kết quả mong đợi hiện tại:**

```text
No tests to run.
BUILD SUCCESS
```

Hiện Mock Service chưa có test source. `No tests to run` ở đây không phải lỗi Maven; `BUILD SUCCESS` vẫn nghĩa là code compile và test lifecycle chạy thành công.

## 11. Build JAR

### Account Opening

**Command:**

```powershell
cd C:\Users\Admin\account-opening
.\mvnw.cmd package
```

### CIF/KYC Mock Service

**Command:**

```powershell
cd C:\Users\Admin\cif-kyc-mock-service
.\mvnw.cmd package
```

**Dùng để:** build ứng dụng thành JAR.

**Giải thích:** `package` chạy theo thứ tự cơ bản:

```text
compile → test → đóng gói thành JAR
```

**Kết quả mong đợi:** `BUILD SUCCESS`. File JAR được tạo trong thư mục `target/`. `target/` là output sinh tự động nên không được commit Git.

## 12. Thứ tự chạy khi demo

```text
Bước 1: Start PostgreSQL

Bước 2: Terminal 1
→ chạy CIF/KYC Mock Service ở :8081

Bước 3: Terminal 2
→ chạy Account Opening ở :8080

Bước 4: Test API bằng Postman, Swagger hoặc PowerShell

Bước 5: Dừng từng service bằng Ctrl + C
```

Nên chạy Mock Service trước khi demo KYC để KYC check có thể gọi HTTP ngay. Account Opening vẫn có thể khởi động nếu Mock Service chưa chạy, nhưng lời gọi KYC check sẽ trả `503`.

## 13. Test Health API

**Endpoint:**

```http
GET http://localhost:8080/api/health
```

**Command PowerShell:**

```powershell
Invoke-RestMethod "http://localhost:8080/api/health" | ConvertTo-Json
```

**Dùng để:** xác nhận backend Account Opening đang chạy.

**Kết quả mong đợi:** HTTP `200` với dữ liệu gần giống:

```json
{
  "status": "UP",
  "application": "account-opening"
}
```

Ngoài ra Spring Boot Actuator có endpoint:

```http
GET http://localhost:8080/actuator/health
```

Nó thường trả:

```json
{
  "status": "UP"
}
```

Health API `/api/health` là API đơn giản của project; Actuator health phù hợp để monitoring kiểm tra tình trạng ứng dụng.

## 14. Test Product API

**Endpoint lấy danh sách product đang active:**

```http
GET http://localhost:8080/api/products
```

**Command:**

```powershell
Invoke-RestMethod "http://localhost:8080/api/products" | ConvertTo-Json -Depth 5
```

**Dùng để:** xem product có thể chọn khi tạo application.

**Kết quả mong đợi:** HTTP `200`, response có `success=true` và mảng `data` chứa product.

**Endpoint lấy một product:**

```http
GET http://localhost:8080/api/products/{productCode}
```

Ví dụ:

```powershell
Invoke-RestMethod "http://localhost:8080/api/products/DIGITAL_SAVING" |
    ConvertTo-Json -Depth 5
```

## 15. Tạo Account Application

DTO request thực tế là `CreateApplicationRequest`, chỉ yêu cầu hai field:

```text
customerId
productCode
```

**Endpoint:**

```http
POST http://localhost:8080/api/applications
Content-Type: application/json
```

**Request body thực tế:**

```json
{
  "customerId": "CUS001",
  "productCode": "DIGITAL_SAVING"
}
```

**Ý nghĩa field:**

- `customerId`: mã customer sẽ được dùng khi KYC check.
- `productCode`: mã sản phẩm đang active, ví dụ `DIGITAL_SAVING`.

**Chạy bằng PowerShell:**

```powershell
$createBody = @{
    customerId = "CUS001"
    productCode = "DIGITAL_SAVING"
} | ConvertTo-Json

$created = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/applications" `
    -ContentType "application/json" `
    -Body $createBody

$created | ConvertTo-Json -Depth 5
$applicationId = $created.data.applicationId
```

**Kết quả mong đợi:** HTTP `201`. Response data có các thông tin quan trọng:

- `applicationId`: UUID dùng cho các API sau.
- `customerId`: customer vừa gửi.
- `productCode` và `productName`: product đã chọn.
- `status`: hồ sơ mới là `DRAFT`.

Lưu `$applicationId` để dùng ở bước lấy application và KYC check.

## 16. Lấy Account Application

**Endpoint:**

```http
GET http://localhost:8080/api/applications/{applicationId}
```

**Command:**

```powershell
Invoke-RestMethod "http://localhost:8080/api/applications/$applicationId" |
    ConvertTo-Json -Depth 5
```

**Dùng để:** kiểm tra application, customerId, product và status đang được lưu trong database.

**Kết quả mong đợi:** HTTP `200` khi UUID tồn tại. Nếu application không tồn tại, API trả HTTP `404` với `errorCode=APPLICATION_NOT_FOUND`.

## 17. CIF/KYC check theo application

**Endpoint:**

```http
POST http://localhost:8080/api/applications/{applicationId}/kyc-check
```

Không gửi request body và không gửi `customerId` trong URL. Account Opening dùng `applicationId` để tự tìm customerId đã lưu:

```text
applicationId
    ↓
tìm AccountApplication
    ↓
lấy customerId
    ↓
CifKycVerificationService
    ↓
CifKycClient
    ↓ HTTP
CIF/KYC Mock Service
```

**Command cho application vừa tạo:**

```powershell
Invoke-RestMethod -Method Post `
    -Uri "http://localhost:8080/api/applications/$applicationId/kyc-check" |
    ConvertTo-Json -Depth 5
```

Với customer hợp lệ, response data có `eligible`, `customerStatus`, `kycStatus` và `kycExpiryDate`.

## 18. Test các case CIF/KYC

Tạo một application riêng cho từng customer bằng endpoint ở phần 15, sau đó gọi KYC check với `applicationId` tương ứng.

| Customer | Expected | Giải thích |
|---|---|---|
| `CUS001` | HTTP `200`, `eligible=true` | Customer active, KYC verified và còn hạn |
| `CUS002` | HTTP `409`, `KYC_EXPIRED` | KYC đã hết hạn |
| `CUS003` | HTTP `409`, `KYC_NOT_VERIFIED` | KYC đang `PENDING`, chưa verified |
| `CUS004` | HTTP `409`, `CUSTOMER_NOT_ACTIVE` | Customer đang `BLOCKED` |
| `CUS999` | HTTP `404`, `CUSTOMER_NOT_FOUND` | Mock Service không có customer này |

Với case trả lỗi, dùng `curl.exe -i` để xem cả HTTP status và response body, ví dụ:

```powershell
curl.exe -i -X POST `
    "http://localhost:8080/api/applications/THAY_APPLICATION_ID_CUA_CUS002/kyc-check"
```

Trong Postman, chọn method `POST` ở ô method riêng và chỉ nhập URL vào ô URL. Không nhập `POST http://...` vào ô URL.

## 19. Test CIF/KYC service unavailable

### Bước 1: dừng Mock Service

Trong Terminal 1 đang chạy Mock Service, nhấn:

```text
Ctrl + C
```

### Bước 2: giữ Account Opening chạy

Không dừng Terminal 2.

### Bước 3: gọi lại KYC check

```powershell
curl.exe -i -X POST `
    "http://localhost:8080/api/applications/$applicationId/kyc-check"
```

**Kết quả mong đợi:**

```text
HTTP 503 Service Unavailable
errorCode: CIF_KYC_SERVICE_UNAVAILABLE
```

Đây không phải `CUSTOMER_NOT_FOUND`: `CUSTOMER_NOT_FOUND` nghĩa là gọi được Mock Service nhưng không có customer. HTTP `503` nghĩa là Account Opening không kết nối được đến Mock Service.

Sau khi test, khởi động lại Mock Service theo phần 5 nếu cần test tiếp.

## 20. Application status sau KYC check

Hiện tại KYC check:

```text
KHÔNG đổi ApplicationStatus
KHÔNG tạo status history mới
```

Ví dụ application đang `DRAFT`, sau KYC PASS vẫn là `DRAFT`.

**Vì sao?** `ApplicationStatus` là lifecycle của hồ sơ: `DRAFT`, `SUBMITTED`, `CANCELLED`... Còn `KYC VERIFIED` là kết quả kiểm tra dữ liệu customer. Hai khái niệm khác nhau và chưa được trộn vào nhau ở giai đoạn hiện tại.

Kiểm tra lại application và history:

```powershell
Invoke-RestMethod "http://localhost:8080/api/applications/$applicationId" |
    ConvertTo-Json -Depth 5

Invoke-RestMethod "http://localhost:8080/api/applications/$applicationId/history" |
    ConvertTo-Json -Depth 6
```

Với application mới tạo chỉ KYC check, history vẫn chỉ có bản ghi khởi tạo `null → DRAFT`.

## 21. Một số lỗi thường gặp

| Triệu chứng | Nguyên nhân thường gặp | Cách kiểm tra/xử lý |
|---|---|---|
| Không tìm thấy `mvnw.cmd` hoặc `pom.xml` | Terminal ở sai thư mục | `cd` đến đúng thư mục ở phần 3 rồi chạy lại |
| Port 8080 đang được dùng | Đã có Account Opening instance khác chạy | Chạy `netstat -ano \| findstr :8080`, xem PID, chỉ dừng đúng process Java bạn nhận diện được |
| Port 8081 đang được dùng | Đã có Mock Service instance khác chạy | Chạy `netstat -ano \| findstr :8081`, xem PID, chỉ dừng đúng process Java bạn nhận diện được |
| KYC check trả 503 | Mock Service chưa chạy hoặc base URL sai | Kiểm tra `http://localhost:8081/api/customers/CUS001` và `integration.cif-kyc.base-url` trong local config |
| Account Opening không kết nối PostgreSQL | PostgreSQL/database/config local sai | Kiểm tra PostgreSQL service, database `account_opening`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; không in password ra màn hình |
| Maven báo `BUILD FAILURE` | Có lỗi compile, test, port hoặc config | Đọc lỗi đầu tiên có ý nghĩa phía trên; đừng chỉ nhìn dòng cuối `BUILD FAILURE` |

### Xem process đang dùng port

**Command:**

```powershell
netstat -ano | findstr :8080
netstat -ano | findstr :8081
```

**Dùng để:** tìm PID process đang lắng nghe port.

Sau khi đã xác nhận đúng PID là service cũ cần dừng, có thể dùng:

```powershell
Stop-Process -Id THAY_PID_VAO_DAY
```

Không dừng process nếu chưa chắc đó là process nào.

## 22. Cheat sheet: command thường dùng

| Command | Dùng khi nào? | Kết quả mong đợi |
|---|---|---|
| `cd C:\Users\Admin\account-opening` | Trước khi chạy lệnh của project chính | Terminal ở đúng project có `pom.xml` |
| `cd C:\Users\Admin\cif-kyc-mock-service` | Trước khi chạy lệnh Mock Service | Terminal ở đúng project Mock Service |
| `.\mvnw.cmd spring-boot:run` | Chạy Mock Service | Mock chạy port 8081 |
| `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local` | Chạy Account Opening sau khi đặt DB environment variables | Account Opening chạy port 8080 |
| `.\mvnw.cmd clean test` | Muốn chạy test sạch | `BUILD SUCCESS` |
| `.\mvnw.cmd package` | Muốn build JAR | JAR trong `target/`, `BUILD SUCCESS` |
| `Invoke-RestMethod "http://localhost:8080/api/health"` | Kiểm tra Account Opening | `status=UP` |
| `Invoke-RestMethod "http://localhost:8081/api/customers/CUS001"` | Kiểm tra Mock Service | Customer CUS001 được trả về |
| `netstat -ano \| findstr :8080` | Khi port 8080 bị dùng | Thấy PID đang dùng port |

## 23. Git command cơ bản

Các lệnh này chỉ để xem trạng thái; không làm thay đổi code hay lịch sử Git.

**Command:**

```bash
git status
```

**Dùng để:** xem file nào đang thay đổi, đã stage hay chưa.

**Command:**

```bash
git diff
```

**Dùng để:** xem nội dung thay đổi chưa stage.

**Command:**

```bash
git log -1 --oneline
```

**Dùng để:** xem commit gần nhất.

**Command:**

```bash
git branch --show-current
```

**Dùng để:** xem branch hiện tại.

Không dùng `force push` hoặc `reset --hard` nếu chưa hiểu rõ tác động của chúng.
