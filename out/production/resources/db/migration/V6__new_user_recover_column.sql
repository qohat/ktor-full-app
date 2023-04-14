ALTER TABLE users
    ADD COLUMN recovering_password BOOLEAN default false,
    ADD COLUMN recover_token uuid,
    ADD COLUMN recover_expiration TIMESTAMP;
