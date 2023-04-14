CREATE TABLE IF NOT EXISTS bill_return_payments (
bill_return_id UUID UNIQUE,
payment_date DATE NOT NULL,
user_id UUID NOT NULL,
created_at timestamp,
PRIMARY KEY(bill_return_id),
CONSTRAINT fk_users
    FOREIGN KEY(user_id)
        REFERENCES users(id),
CONSTRAINT fk_bill_return
      FOREIGN KEY(bill_return_id)
          REFERENCES bill_returns(id)
);