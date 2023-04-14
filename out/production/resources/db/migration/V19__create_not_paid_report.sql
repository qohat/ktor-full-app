CREATE TABLE IF NOT EXISTS bill_return_not_paid_report (
bill_return_id UUID UNIQUE,
user_id UUID NOT NULL,
reason_id INT NOT NULL,
created_at timestamp,
PRIMARY KEY(bill_return_id),
CONSTRAINT fk_users
    FOREIGN KEY(user_id)
        REFERENCES users(id),
CONSTRAINT fk_reason
    FOREIGN KEY(reason_id)
        REFERENCES lists(id),
CONSTRAINT fk_bill_return
      FOREIGN KEY(bill_return_id)
          REFERENCES bill_returns(id)
);