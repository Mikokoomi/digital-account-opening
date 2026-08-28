ALTER TABLE account_applications
    ADD COLUMN review_required BOOLEAN,
    ADD COLUMN review_reason VARCHAR(50);

ALTER TABLE account_applications
    ADD CONSTRAINT chk_account_applications_review_snapshot
        CHECK (
            (review_required = TRUE AND review_reason IS NOT NULL)
            OR (review_required = FALSE AND review_reason IS NULL)
            OR (review_required IS NULL AND review_reason IS NULL)
        );
