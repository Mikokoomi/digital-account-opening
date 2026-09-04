# Data Design

## 1. Data Model Overview và ERD

```mermaid
erDiagram
    PRODUCTS ||--o{ ACCOUNT_APPLICATIONS : requested_by
    ACCOUNT_APPLICATIONS ||--o{ APPLICATION_STATUS_HISTORY : has
    ACCOUNT_APPLICATIONS ||--o| APPROVAL_CASES : requires
    ACCOUNT_APPLICATIONS ||--o| BANK_ACCOUNTS : provisions
    PRODUCTS { bigint id PK
               varchar product_code UK }
    ACCOUNT_APPLICATIONS { uuid application_id PK
                           varchar product_code FK
                           varchar status
                           varchar kyc_status
                           date kyc_expiry_date
                           boolean review_required
                           varchar review_reason }
    APPLICATION_STATUS_HISTORY { uuid history_id PK
                                 uuid application_id FK
                                 varchar from_status
                                 varchar to_status }
    APPROVAL_CASES { uuid case_id PK
                     uuid application_id FK,UK
                     varchar status
                     varchar assigned_to
                     varchar decision_reason }
    BANK_ACCOUNTS { uuid id PK
                    uuid application_id FK,UK
                    uuid external_account_id UK
                    varchar account_number UK }
```

## 2. Entity / Table Definitions

| Table | Definition |
|---|---|
| `products` | Identity ID, unique code, product fields/flags/timestamps; `min_age >= 0`. |
| `account_applications` | UUID ID, customer/product/status, KYC snapshot, `review_required/review_reason`, rejection/submission/cancellation/timestamps. |
| `application_status_history` | UUID ID, application FK, from/to status, actor, reason, time. |
| `approval_cases` | UUID ID, unique application FK, case status, assignment/review/decision data và timestamps. |
| `bank_accounts` | Local projection: unique application/external account/account number, status và opened time. |

## 3. Relationships, Keys, Constraints, Indexes

`bank_accounts.application_id` references application và UNIQUE. `external_account_id` và `account_number` cũng UNIQUE. Main không lưu balance, transaction hoặc ledger.

## 4. Data Lifecycle Notes

Flyway V1–V8/JPA là source of truth; V8 tạo local `bank_accounts`. Core Banking Mock có database riêng và là owner của account domain. `integration_requests`, `notifications`, `audit_logs` vẫn PLANNED.
