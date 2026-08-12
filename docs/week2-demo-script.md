# Kịch bản demo Tuần 2

> Dùng Swagger tại `http://localhost:8080/swagger-ui.html`. Sau mỗi request tạo mới, lưu `applicationId` trả về để dùng cho các bước sau.

## 1. Giới thiệu mục tiêu Tuần 2

- **Mở gì:** README hoặc tài liệu này.
- **Bấm hoặc chạy gì:** Giới thiệu ngắn phần quản lý hồ sơ mở tài khoản.
- **Nói gì:** “Tuần 2 hoàn thiện Account Application và tích hợp với CIF/KYC Mock Service độc lập.”
- **Kết quả mong đợi:** Người xem hiểu luồng quản lý hồ sơ và kiểm tra CIF/KYC qua HTTP.

## 2. Mở Swagger

- **Mở gì:** Trình duyệt.
- **Bấm hoặc chạy gì:** Mở `http://localhost:8080/swagger-ui.html`.
- **Nói gì:** “Swagger hiển thị các API hiện có và cho phép thử trực tiếp.”
- **Kết quả mong đợi:** Swagger UI tải thành công.

## 3. Xem danh sách product

- **Mở gì:** Endpoint `GET /api/products` trong Swagger.
- **Bấm hoặc chạy gì:** Chọn **Try it out**, rồi **Execute**.
- **Nói gì:** “Chỉ product đang active mới xuất hiện và mới có thể được chọn cho hồ sơ.”
- **Kết quả mong đợi:** Nhận HTTP 200 cùng các product active; chọn hai mã product khác nhau cho bước sau.

## 4. Tạo application

- **Mở gì:** Endpoint `POST /api/applications`.
- **Bấm hoặc chạy gì:** Gửi `customerId` nhận diện cho buổi demo và product active đầu tiên.
- **Nói gì:** “Một hồ sơ mới luôn bắt đầu ở trạng thái DRAFT.”
- **Kết quả mong đợi:** HTTP 201, trạng thái `DRAFT`; lưu lại `applicationId` trả về.

## 5. Kiểm tra application trong PostgreSQL

- **Mở gì:** `psql` hoặc pgAdmin, database `account_opening`.
- **Bấm hoặc chạy gì:** Tra cứu theo `application_id` vừa tạo trong `account_applications` và `application_status_history`.
- **Nói gì:** “Bản ghi application và history khởi tạo được lưu cùng nhau.”
- **Kết quả mong đợi:** Có một history `null → DRAFT`.

## 6. Kiểm tra CIF/KYC

- **Mở gì:** Endpoint `POST /api/applications/{applicationId}/kyc-check` và CIF/KYC Mock Service trên port `8081`.
- **Bấm hoặc chạy gì:** Gọi KYC check cho application dùng `customerId=CUS001`, không gửi request body.
- **Nói gì:** “Application Service lấy customerId đã lưu và gọi mock service bằng RestClient. CUS001 đang ACTIVE, KYC VERIFIED và còn hạn.”
- **Kết quả mong đợi:** HTTP 200, `eligible=true`; application vẫn `DRAFT` và không có history mới.

Test thêm các application dùng `CUS002`, `CUS003`, `CUS004` và `CUS999` để nhận lần lượt `KYC_EXPIRED`, `KYC_NOT_VERIFIED`, `CUSTOMER_NOT_ACTIVE` và `CUSTOMER_NOT_FOUND`.

## 7. Update product

- **Mở gì:** Endpoint `PATCH /api/applications/{applicationId}`.
- **Bấm hoặc chạy gì:** Gửi product active thứ hai.
- **Nói gì:** “Khi còn DRAFT, người dùng chỉ được đổi productCode.”
- **Kết quả mong đợi:** HTTP 200, application vẫn là `DRAFT` và productCode đã đổi.

## 8. Chứng minh update không tạo history

- **Mở gì:** Endpoint `GET /api/applications/{applicationId}/history` hoặc PostgreSQL.
- **Bấm hoặc chạy gì:** Lấy history của application vừa update.
- **Nói gì:** “Đổi product không phải là chuyển trạng thái nên không phát sinh history.”
- **Kết quả mong đợi:** Vẫn chỉ có history `null → DRAFT`.

## 9. Submit

- **Mở gì:** Endpoint `PATCH /api/applications/{applicationId}/submit`.
- **Bấm hoặc chạy gì:** Execute, không gửi request body.
- **Nói gì:** “Chỉ DRAFT được submit; thời điểm submittedAt được gán tại đây.”
- **Kết quả mong đợi:** HTTP 200, trạng thái `SUBMITTED` và `submittedAt` khác rỗng.

## 10. Xem history DRAFT → SUBMITTED

- **Mở gì:** Endpoint `GET /api/applications/{applicationId}/history`.
- **Bấm hoặc chạy gì:** Execute.
- **Nói gì:** “History trả theo thời gian tăng dần.”
- **Kết quả mong đợi:** Có `null → DRAFT` rồi `DRAFT → SUBMITTED`.

## 11. Cancel

- **Mở gì:** Endpoint `PATCH /api/applications/{applicationId}/cancel`.
- **Bấm hoặc chạy gì:** Execute, không gửi request body.
- **Nói gì:** “DRAFT hoặc SUBMITTED đều có thể bị hủy.”
- **Kết quả mong đợi:** HTTP 200, trạng thái `CANCELLED`, `cancelledAt` khác rỗng và `submittedAt` vẫn được giữ nếu hồ sơ đã submit.

## 12. Xem history SUBMITTED → CANCELLED

- **Mở gì:** Endpoint `GET /api/applications/{applicationId}/history`.
- **Bấm hoặc chạy gì:** Execute.
- **Nói gì:** “Mỗi chuyển trạng thái hợp lệ đều được lưu history.”
- **Kết quả mong đợi:** Có ba bản ghi theo thứ tự: `null → DRAFT`, `DRAFT → SUBMITTED`, `SUBMITTED → CANCELLED`.

## 13. Test lỗi submit lần hai

- **Mở gì:** Lại endpoint submit của application đã submit hoặc đã cancel.
- **Bấm hoặc chạy gì:** Execute thêm một lần.
- **Nói gì:** “API trả lỗi nghiệp vụ rõ ràng, không trả stack trace nội bộ.”
- **Kết quả mong đợi:** HTTP 409 với `APPLICATION_NOT_SUBMITTABLE`.

## 14. Test service unavailable

- **Mở gì:** Terminal chạy CIF/KYC Mock Service.
- **Bấm hoặc chạy gì:** Dừng mock service bằng `Ctrl+C`, giữ account-opening chạy và gọi lại KYC check.
- **Nói gì:** “Lỗi kết nối integration được chuyển thành response thống nhất, không lộ stack trace nội bộ.”
- **Kết quả mong đợi:** HTTP 503 với `CIF_KYC_SERVICE_UNAVAILABLE`.

## 15. Chạy Maven test

- **Mở gì:** PowerShell ở thư mục project.
- **Bấm hoặc chạy gì:** Chạy `./mvnw.cmd clean test`.
- **Nói gì:** “Bộ test bao phủ service và controller cho các luồng thành công, validation và lỗi trạng thái.”
- **Kết quả mong đợi:** 69 tests pass, `BUILD SUCCESS`.

## 16. Kết luận

- **Mở gì:** README và `docs/week2-review.md`.
- **Bấm hoặc chạy gì:** Tóm tắt các kết quả vừa kiểm tra.
- **Nói gì:** “Tuần 2 đã hoàn thành Account Application + CIF/KYC Integration; Tuần 3 mới sẽ bắt đầu Business Rules.”
- **Kết quả mong đợi:** Người xem có thể đối chiếu API, CIF/KYC rules, lịch sử trạng thái, test và dữ liệu PostgreSQL.
