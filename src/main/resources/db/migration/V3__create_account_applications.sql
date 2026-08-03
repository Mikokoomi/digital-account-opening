CREATE TABLE account_applications (
    application_id UUID PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    kyc_status VARCHAR(30),
    cif_verified_at TIMESTAMP WITH TIME ZONE,
    reject_reason VARCHAR(500),
    submitted_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_account_applications_product
        FOREIGN KEY (product_code)
        REFERENCES products (product_code)
);

CREATE INDEX idx_account_applications_customer_id
    ON account_applications (customer_id);

CREATE INDEX idx_account_applications_product_code
    ON account_applications (product_code);

CREATE INDEX idx_account_applications_status
    ON account_applications (status);

CREATE INDEX idx_account_applications_created_at
    ON account_applications (created_at);
