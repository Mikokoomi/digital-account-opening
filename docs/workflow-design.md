# Workflow Design

## 1. Workflow Overview và Preconditions

Customer là existing customer, đã authenticated và có CIF. Authentication/login và tạo CIF mới không thuộc scope.

## 2. BPMN — End-to-End Business Process

```mermaid
flowchart TD
    A([Existing customer authenticated]) --> B[View ACTIVE products]
    B --> C[Select product]
    C --> D[Create application: DRAFT]
    D --> E[Submit: SUBMITTED]
    E --> F[Validate and call CIF/KYC Mock]
    F --> G{KYC valid?}
    G -- No --> X[Business/technical error]
    G -- Yes --> H[Store/check KYC snapshot]
    H --> J[Mandatory checks: ProductActiveRule + KycVerifiedRule]
    J --> K{Mandatory conditions pass?}
    K -- No --> X1[BLOCK: remain SUBMITTED, no ApprovalCase]
    K -- Yes --> RQ{reviewRequired?}
    RQ -- No --> L[Auto routing: APPROVED — IMPLEMENTED]
    RQ -- Yes --> M[Manual-review routing: UNDER_REVIEW — IMPLEMENTED]
    M --> N1[reviewReason required]
    N1 --> C1[Create ApprovalCase: PENDING]
    C1 --> C2[Assign staff: ASSIGNED]
    C2 --> O{Staff decision — IMPLEMENTED}
    O -- Approve --> L
    O -- Reject --> N[REJECTED]
    L --> P[Core Banking Mock — PLANNED]
    P --> Q{Result}
    Q -- Success --> R[bank_account, COMPLETED — PLANNED]
    Q -- temporary failure --> S[Controlled retry — PLANNED]
    S --> P
    Q -- business error/retry exhausted --> T[FAILED — PLANNED]
```

## 3. Application Lifecycle và State Machine

```mermaid
stateDiagram-v2
    [*] --> DRAFT: create
    DRAFT --> SUBMITTED: submit
    DRAFT --> CANCELLED: cancel
    SUBMITTED --> CANCELLED: cancel
    SUBMITTED --> SUBMITTED: mandatory fail / blocked
    SUBMITTED --> APPROVED: mandatory pass + no review
    SUBMITTED --> UNDER_REVIEW: mandatory pass + reviewRequired
    UNDER_REVIEW --> APPROVED: workflow policy
    UNDER_REVIEW --> REJECTED: workflow policy
```

`ApplicationWorkflowService` enforces application transitions. Public staff operations now expose assignment and UNDER_REVIEW approval/rejection. FAILED has no current flow.

## 4. Allowed State Transitions

Current public operations: create DRAFT, DRAFT→SUBMITTED, DRAFT/SUBMITTED→CANCELLED. Process giữ `SUBMITTED` khi mandatory condition fail; chuyển APPROVED khi mandatory pass và không cần review; chuyển UNDER_REVIEW khi mandatory pass và `reviewRequired=true`. Invalid workflow transitions return `INVALID_APPLICATION_STATUS_TRANSITION`.

## 5. Business Rules

| Rule | Status | Pass condition | Fail |
|---|---|---|---|
| `PRODUCT_ACTIVE` | IMPLEMENTED | Product exists and active. | Inactive/null; missing product is exception. |
| `KYC_VERIFIED` | IMPLEMENTED | VERIFIED, timestamp/expiry exist and expiry current. | Missing/invalid/expired snapshot. |

`eligible` của rule evaluation chỉ có nghĩa mandatory conditions đã thỏa mãn. Nó không còn quyết định manual review. Future enhancements: duplicate product needs ownership data; age needs verified DOB; advanced customer/risk rules cần approved inputs/policy.

## 6. Rule Evaluation Outcomes

Current evaluate endpoint only returns eligibility and remains read-only.

IMPLEMENTED:

- Mandatory rule failure → BLOCK, giữ `SUBMITTED`, không tạo ApprovalCase.
- Mandatory pass + `reviewRequired=false` → `APPROVED`.
- Mandatory pass + `reviewRequired=true` và reason hợp lệ → `UNDER_REVIEW` và tạo ApprovalCase.

Staff approval is IMPLEMENTED: routing to `UNDER_REVIEW` creates one `PENDING` case; assignment moves it to `ASSIGNED`; only assigned staff may approve/reject. Approve reevaluates mandatory rules, nhưng không yêu cầu `reviewRequired=false` vì review signal là lý do case tồn tại. Case và application transition hoàn tất trong một transaction.

## 7. Exception Overview, Audit and Status History

Business KYC failures do not persist a success snapshot. Technical CIF/KYC failures return 503 and are not customer rejection. `ApplicationStatusHistory` is currently written for create, submit, cancel, and process transitions. AuditLog is PLANNED; history/audit are cross-cutting business-event concerns, not end-of-flow tasks.
