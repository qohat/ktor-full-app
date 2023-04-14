DO $$
BEGIN
   IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'people_request_number_seq') THEN
         EXECUTE 'CREATE SEQUENCE people_request_number_seq START WITH ' || (SELECT MAX(number) from people_requests) + 1 || ' INCREMENT BY 1';
   END IF;
END $$;

UPDATE people_requests SET number = nextval('people_request_number_seq') WHERE number is null;

ALTER TABLE people_requests ALTER COLUMN number SET NOT NULL;
ALTER TABLE people_requests ALTER COLUMN number SET DEFAULT nextval('people_request_number_seq');

