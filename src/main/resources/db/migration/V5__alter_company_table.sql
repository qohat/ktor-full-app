ALTER TABLE people ADD COLUMN created_by_id UUID;
ALTER TABLE people ADD CONSTRAINT fk_created_by_id FOREIGN KEY(created_by_id) REFERENCES users(id);
