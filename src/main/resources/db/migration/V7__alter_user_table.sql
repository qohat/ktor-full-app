ALTER TABLE users ADD COLUMN ccf_id INT;
ALTER TABLE users ADD CONSTRAINT fk_ccf_id FOREIGN KEY(ccf_id) REFERENCES lists(id);