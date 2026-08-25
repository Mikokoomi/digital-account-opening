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
    H --> I[Product check]
    I --> J[Evaluate ProductActiveRule + KycVerifiedRule]
    J --> K{Decision model}
    K --> L[Auto Approve: APPROVED — PLANNED decision]
    K --> M[Manual Review: approval case, UNDER_REVIEW — PLANNED]
    K --> N[Reject: REJECTED — PLANNED]
    M --> O{Staff decision — PLANNED}
    O -- Approve --> L
    O -- Reject --> N
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
    SUBMITTED --> APPROVED: process eligible
    SUBMITTED --> UNDER_REVIEW: process ineligible
    UNDER_REVIEW --> APPROVED: workflow policy
    UNDER_REVIEW --> REJECTED: workflow policy
```

`ApplicationWorkflowService` enforces the diagram transitions. Current public process reaches APPROVED/UNDER_REVIEW; staff operations for UNDER_REVIEW approval/rejection are not exposed. FAILED has no current flow.

## 4. Allowed State Transitions

Current public operations: create DRAFT, DRAFT→SUBMITTED, DRAFT/SUBMITTED→CANCELLED, SUBMITTED→APPROVED when eligible, SUBMITTED→UNDER_REVIEW when ineligible. Invalid workflow transitions return `INVALID_APPLICATION_STATUS_TRANSITION`.

## 5. Business Rules

| Rule | Status | Pass condition | Fail |
|---|---|---|---|
| `PRODUCT_ACTIVE` | IMPLEMENTED | Product exists and active. | Inactive/null; missing product is exception. |
| `KYC_VERIFIED` | IMPLEMENTED | VERIFIED, timestamp/expiry exist and expiry current. | Missing/invalid/expired snapshot. |

Future enhancements: duplicate product needs ownership data; age needs verified DOB; customer active/risk/required data/manual review need approved inputs/policy.

## 6. Rule Evaluation Outcomes

Current evaluate endpoint only returns eligibility. Auto approve/manual review from `processApplication` are implemented for eligible/ineligible results; direct reject and approval case are PLANNED.

## 7. Exception Overview, Audit and Status History

Business KYC failures do not persist a success snapshot. Technical CIF/KYC failures return 503 and are not customer rejection. `ApplicationStatusHistory` is currently written for create, submit, cancel, and process transitions. AuditLog is PLANNED; history/audit are cross-cutting business-event concerns, not end-of-flow tasks.
