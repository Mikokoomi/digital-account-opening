ALTER TABLE account_applications
    ADD COLUMN customer_full_name VARCHAR(200),
    ADD COLUMN customer_date_of_birth DATE,
    ADD COLUMN customer_status VARCHAR(30);
