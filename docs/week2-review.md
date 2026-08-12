# Rà soát Tuần 2

## Mục tiêu Tuần 2

Hoàn thành Account Application và tích hợp CIF/KYC với mock service độc lập.

## Chức năng đã hoàn thành

- Tạo application.
- Xem chi tiết application.
- Cập nhật product khi application còn `DRAFT`.
- Submit từ `DRAFT` sang `SUBMITTED`.
- Cancel từ `DRAFT` hoặc `SUBMITTED`.
- Xem lịch sử trạng thái.
- Validation và xử lý lỗi theo mã lỗi API.
- CIF/KYC Mock Service chạy độc lập ở port `8081`.
- HTTP integration bằng Spring `RestClient`.
- Kiểm tra customer tồn tại, `ACTIVE`, KYC `VERIFIED` và chưa hết hạn.
- Endpoint `POST /api/applications/{applicationId}/kyc-check`.
- Xử lý lỗi CIF/KYC theo HTTP 404, 409 và 503.
- Automated test cho client, verification service, application service và controller.

## Luồng demo

```text
Create → KYC check → Get detail → Update → Submit → Get history → Cancel → Get history
```

## Business rules

- Application mới luôn có trạng thái `DRAFT`.
- Chỉ `DRAFT` được update.
- Chỉ `DRAFT` được submit.
- Chỉ `DRAFT` hoặc `SUBMITTED` được cancel.
- Update product không tạo history.
- Submit và cancel phải tạo history.
- Product phải tồn tại và active khi create, update và submit.
- Cancel không phụ thuộc product còn active hay không.
- History được lấy theo `changedAt` tăng dần.
- KYC check dùng `customerId` lưu trong application, không nhận customer từ request.
- Customer phải tồn tại, `ACTIVE`, có KYC `VERIFIED` và ngày hết hạn không trước ngày hiện tại.
- KYC check không thay đổi application status và không tạo status history.
- Upstream response thiếu status hoặc ngày hết hạn được coi là integration/data-contract failure, không phải business failure.

## Transaction

- Create lưu application và history ban đầu trong cùng một transaction.
- Submit cập nhật application và lưu history trong cùng một transaction.
- Cancel cập nhật application và lưu history trong cùng một transaction.
- Unit test xác nhận lỗi từ `historyRepository.save(...)` được truyền ra ngoài, không bị nuốt để trả kết quả thành công.

Chưa có integration test rollback thực tế trên PostgreSQL. Vì vậy unit test chỉ chứng minh exception không bị xử lý sai ở service, không chứng minh rollback ở database. Project chưa thêm Testcontainers, H2 hay dependency nặng chỉ cho kiểm tra này.

## Kết quả test

Lệnh `.\mvnw.cmd clean test` hoàn thành với **69 tests**, `Failures: 0`, `Errors: 0`, `Skipped: 0`, `BUILD SUCCESS`.

## Rủi ro kỹ thuật còn theo dõi

- Flyway có cảnh báo PostgreSQL 18.4 mới hơn phiên bản đã được kiểm thử chính thức.
- `@MockBean` vẫn có cảnh báo deprecated trong kiểm thử controller.
- Mockito vẫn có cảnh báo dynamic agent trên các JDK tương lai.
- Timestamp hiện chưa hoàn toàn thống nhất vì `Product` dùng `LocalDateTime`, còn application và history dùng `OffsetDateTime`.
