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
