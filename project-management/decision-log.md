# Decision Log

## DEC-001 - Customer master thuộc CIF/KYC

Customer data không duplicate local; application giữ `customerId`, CIF/KYC cung cấp profile.

## DEC-002 - Technical CIF/KYC failure không phải business failure

Unavailable/empty/incomplete/unexpected upstream trả `503 CIF_KYC_SERVICE_UNAVAILABLE`, không phải rejection và không update success snapshot/status.

## DEC-003 - Business rules incremental

Chỉ `PRODUCT_ACTIVE`, `KYC_VERIFIED` được đăng ký vì data đáng tin cậy. Ownership, DOB, risk, customer-data rules là planned.

## DEC-004 - ApprovalCase và application được quyết định nguyên tử

Mỗi application có tối đa một ApprovalCase bằng service guard và UNIQUE constraint. Tạo case với transition sang `UNDER_REVIEW`, cũng như staff decision với application transition/history, chạy trong cùng transaction.

## DEC-005 - Staff không override mandatory conditions

Trước khi staff approve, hệ thống evaluate lại mandatory rules. Product inactive hoặc KYC snapshot không còn hợp lệ trả conflict và giữ nguyên case/application; `reviewRequired=true` không block approve; staff vẫn có thể reject.

## DEC-006 - Manual review dùng tín hiệu upstream rõ nghĩa

Manual review được quyết định riêng bằng CIF/KYC `reviewRequired/reviewReason`, không suy ra từ mandatory rule failure. Mandatory fail giữ application SUBMITTED và không tạo ApprovalCase. Contract invariant sai là integration/data-contract failure.

## DEC-007 - Core Banking sở hữu BankAccount

Core Banking Mock tạo account và account number. Main chỉ lưu local reference, không lưu balance, transaction hoặc ledger.

## DEC-008 - Approval và provisioning là hai bước riêng

Approval kết thúc ở `APPROVED`. Endpoint create-account riêng bắt đầu provisioning và không làm Core Banking outage rollback quyết định approval.

## DEC-009 - HTTP call nằm ngoài database transaction

Phase A commit `ACCOUNT_CREATING`, phase B gọi Core Banking không có transaction, phase C lưu reference và commit `COMPLETED`. Technical/data-contract failure giữ `ACCOUNT_CREATING` vì external account có thể đã được tạo dù response bị mất.

## DEC-010 - Reliability có giới hạn và quan sát được

Account creation dùng stable key `CREATE_ACCOUNT:<applicationId>`. Một `IntegrationRequest` đại diện logical operation, không phải từng attempt; mọi automatic/manual attempt reuse record và key đó. Core Banking replay cùng key/payload trả existing account.

Chỉ network/I/O, HTTP 429 và 5xx được retry. Business conflict và data-contract error không retry. Hết bounded retry chuyển `RETRY_PENDING`, không dùng `FAILED` vì external result có thể chưa xác định. Manual retry reuse cùng tracking record/key. HTTP và backoff không nằm trong DB transaction. Scheduled retry và generalized reconciliation chưa thuộc current scope.

## DEC-011 - Trạng thái failure của Core Banking phải phản ánh độ chắc chắn

HTTP 4xx từ Core Banking là definitive rejection nên cả application và integration request chuyển `FAILED`. Response success malformed/incomplete là ambiguous vì external account có thể đã được tạo; trường hợp này chuyển cả hai sang `RETRY_PENDING` để manual recovery với cùng idempotency key. Retry exhaustion và interrupted backoff cũng dùng `RETRY_PENDING`; khi interrupt, thread interrupt flag phải được khôi phục trước khi trả lỗi.

## DEC-012 - Audit và status history có responsibility khác nhau

Status history mô tả application lifecycle; audit trả lời actor đã thực hiện business/system action nào lên object nào, lúc nào và kết quả gì. Không dùng hai loại record thay thế nhau.

## DEC-013 - Audit success cùng transaction với business mutation

Audit persistence là compliance/business trace trong cùng database. Khi audit của mutation thất bại, exception không bị swallow và transaction business được rollback.

## DEC-014 - Notification failure tách khỏi business result

Notification request được xử lý AFTER_COMMIT trong transaction riêng. Sender failure tạo notification `FAILED` nhưng không rollback approval, rejection hoặc completed account.

## DEC-015 - Notification delivery hiện là logging abstraction

`NotificationSender` tách delivery khỏi persistence; implementation hiện tại chỉ log type/application ID, không gọi email, SMS hay push provider.

## DEC-016 - Không audit read và automatic retry attempt

GET API không phải business mutation. Từng Core Banking attempt thuộc `IntegrationRequest`; audit chỉ ghi start, retry pending, manual retry, failure và success milestone.

## DEC-017 - Idempotent replay không tạo business event trùng

Repeated create-account sau `COMPLETED` trả local result, không tạo thêm `ACCOUNT_CREATED` audit hoặc `ACCOUNT_OPENED` notification. Notification service còn có service guard theo application/type.
