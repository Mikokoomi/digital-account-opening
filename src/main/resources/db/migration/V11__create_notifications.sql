CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    subject VARCHAR(200),
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    failure_reason VARCHAR(500),
    CONSTRAINT fk_notifications_application FOREIGN KEY (application_id)
        REFERENCES account_applications(application_id)
);

CREATE INDEX idx_notifications_application_id ON notifications(application_id);
