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

## DEC-010 - Trạng thái failure của Core Banking phải phản ánh độ chắc chắn

HTTP 4xx từ Core Banking là definitive rejection nên cả application và integration request chuyển `FAILED`. Response success malformed/incomplete là ambiguous vì external account có thể đã được tạo; trường hợp này chuyển cả hai sang `RETRY_PENDING` để manual recovery với cùng idempotency key. Retry exhaustion và interrupted backoff cũng dùng `RETRY_PENDING`; khi interrupt, thread interrupt flag phải được khôi phục trước khi trả lỗi.
