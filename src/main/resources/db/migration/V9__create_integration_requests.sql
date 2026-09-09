CREATE TABLE integration_requests (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL,
    integration_type VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_http_status INTEGER,
    external_reference VARCHAR(100),
    last_error_code VARCHAR(100),
    last_error_message VARCHAR(500),
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_integration_requests_application FOREIGN KEY (application_id)
        REFERENCES account_applications (application_id),
    CONSTRAINT uq_integration_request_operation UNIQUE (application_id, integration_type),
    CONSTRAINT uq_integration_request_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_integration_request_attempt_count CHECK (attempt_count >= 0)
);
