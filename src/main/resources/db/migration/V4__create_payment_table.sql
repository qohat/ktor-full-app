CREATE TABLE IF NOT EXISTS payments (
   id UUID,
   people_companies_id UUID NOT NULL,
   base_income DECIMAL NOT NULL,
   value DECIMAL NOT NULL,
   month_applied VARCHAR(3) NOT NULL,
   year_applied INTEGER NOT NULL,
   created_at TIMESTAMP,
   PRIMARY KEY (id),
   CONSTRAINT fk_people_companies
      FOREIGN KEY(people_companies_id)
          REFERENCES people_companies(id)
);