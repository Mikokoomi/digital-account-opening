CREATE TABLE application_status_history (
    history_id UUID PRIMARY KEY,
    application_id UUID NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by VARCHAR(100),
    reason VARCHAR(500),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_application_status_history_application
        FOREIGN KEY (application_id)
        REFERENCES account_applications (application_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_application_status_history_application_changed_at
    ON application_status_history (application_id, changed_at ASC);

CREATE INDEX idx_application_status_history_to_status
    ON application_status_history (to_status);
