CREATE TABLE approval_cases (
    case_id UUID PRIMARY KEY,
    application_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    assigned_to VARCHAR(100),
    review_reason VARCHAR(500),
    decision_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE,
    decided_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_approval_cases_application
        FOREIGN KEY (application_id)
        REFERENCES account_applications (application_id),
    CONSTRAINT uk_approval_cases_application UNIQUE (application_id)
);

CREATE INDEX idx_approval_cases_status ON approval_cases (status);
CREATE INDEX idx_approval_cases_assigned_to ON approval_cases (assigned_to);
CREATE INDEX idx_approval_cases_created_at ON approval_cases (created_at);
