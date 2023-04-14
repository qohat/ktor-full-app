CREATE TABLE IF NOT EXISTS bill_returns_observations (
   id BIGSERIAL,
   bill_return_id UUID NOT NULL,
   observation TEXT,
   user_id UUID NOT NULL,
   created_at TIMESTAMP,
   PRIMARY KEY (id),
    CONSTRAINT fk_bill_returns_id
        FOREIGN KEY(bill_return_id)
            REFERENCES bill_returns(id),
    CONSTRAINT fk_users
        FOREIGN KEY(user_id)
            REFERENCES users(id)
);