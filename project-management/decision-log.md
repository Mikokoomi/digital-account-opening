# Decision Log

## DEC-001 - Customer master thuộc CIF/KYC

Customer data không duplicate local; application giữ `customerId`, CIF/KYC cung cấp profile.

## DEC-002 - Technical CIF/KYC failure không phải business failure

Unavailable/empty/incomplete/unexpected upstream trả `503 CIF_KYC_SERVICE_UNAVAILABLE`, không phải rejection và không update success snapshot/status.

## DEC-003 - Business rules incremental

Chỉ `PRODUCT_ACTIVE`, `KYC_VERIFIED` được đăng ký vì data đáng tin cậy. Ownership, DOB, risk, customer-data rules là planned.
