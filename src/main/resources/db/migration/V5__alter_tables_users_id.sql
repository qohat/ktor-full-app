ALTER TABLE people_companies ADD COLUMN created_by_id UUID NOT NULL;
ALTER TABLE people_companies ADD COLUMN assigned_to_id UUID NOT NULL;
ALTER TABLE people_companies ADD CONSTRAINT fk_created_by_id FOREIGN KEY(created_by_id) REFERENCES users(id);
ALTER TABLE people_companies ADD CONSTRAINT fk_assigned_to_id FOREIGN KEY(assigned_to_id) REFERENCES users(id);

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
