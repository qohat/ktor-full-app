CREATE TABLE IF NOT EXISTS user_assignments (
   user_id UUID NOT NULL,
   people_request_id UUID NOT NULL,
   PRIMARY KEY (user_id, people_request_id),
   CONSTRAINT fk_user_id
        FOREIGN KEY(user_id)
          REFERENCES users(id),
   CONSTRAINT fk_people_request_id
        FOREIGN KEY(people_request_id)
          REFERENCES people_requests(id)
);
