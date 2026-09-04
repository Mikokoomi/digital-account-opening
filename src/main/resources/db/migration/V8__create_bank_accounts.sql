CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL UNIQUE,
    external_account_id UUID NOT NULL UNIQUE,
    account_number VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_bank_accounts_application
        FOREIGN KEY (application_id)
        REFERENCES account_applications (application_id)
);
