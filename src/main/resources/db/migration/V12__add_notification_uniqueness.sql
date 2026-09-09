DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM notifications
        GROUP BY application_id, type
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot enforce notification uniqueness: duplicate application_id/type rows exist';
    END IF;
END $$;

ALTER TABLE notifications
    ADD CONSTRAINT uq_notifications_application_type
    UNIQUE (application_id, type);
