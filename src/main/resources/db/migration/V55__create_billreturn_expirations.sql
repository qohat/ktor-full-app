CREATE TABLE IF NOT EXISTS people_request_expirations (
people_request_id UUID,
request_expiration DATE NOT NULL,
response_expiration DATE,
created_at timestamp,
PRIMARY KEY(people_request_id, request_expiration),
CONSTRAINT fk_people_request
      FOREIGN KEY(people_request_id)
          REFERENCES people_requests(id)
);