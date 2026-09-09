CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_type VARCHAR(20) NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    action VARCHAR(60) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    application_id UUID,
    result VARCHAR(20) NOT NULL,
    details VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_audit_logs_application FOREIGN KEY (application_id)
        REFERENCES account_applications(application_id)
);

CREATE INDEX idx_audit_logs_application_id ON audit_logs(application_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
