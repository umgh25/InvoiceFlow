ALTER TABLE customer ADD COLUMN owner_id BIGINT;

ALTER TABLE customer
    ADD CONSTRAINT fk_customer_owner
    FOREIGN KEY (owner_id) REFERENCES app_user(id) ON DELETE CASCADE;

DO $$
DECLARE
    existing_customer_count BIGINT;
    existing_owner_count BIGINT;
    legacy_owner_id BIGINT;
BEGIN
    SELECT COUNT(*) INTO existing_customer_count FROM customer;

    IF existing_customer_count > 0 THEN
        SELECT COUNT(*), MIN(id)
        INTO existing_owner_count, legacy_owner_id
        FROM app_user;

        IF existing_owner_count <> 1 THEN
            RAISE EXCEPTION
                'Cannot safely assign % existing customers to % users. Resolve ownership before applying V3.',
                existing_customer_count,
                existing_owner_count;
        END IF;

        UPDATE customer SET owner_id = legacy_owner_id WHERE owner_id IS NULL;
    END IF;
END $$;

ALTER TABLE customer ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE customer DROP CONSTRAINT IF EXISTS customer_email_key;

CREATE INDEX idx_customer_owner_id ON customer(owner_id);
CREATE UNIQUE INDEX uq_customer_owner_email_lower ON customer(owner_id, LOWER(email));
