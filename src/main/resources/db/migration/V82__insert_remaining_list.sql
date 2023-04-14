update lists set name = 'Secretaría de Agricultura Municipal o quien haga sus veces' WHERE name = 'Organización 1' and list = 'ORGANIZATION_TYPE';
update lists set name = 'No reporta' WHERE name = 'Organización 2' and list = 'ORGANIZATION_TYPE';
update lists set name = 'Organización de productores' WHERE name = 'Organización 3' and list = 'ORGANIZATION_TYPE';

insert into lists (name, list, active) VALUES ('Gobernadores/organizaciones de base indígena', 'ORGANIZATION_TYPE', TRUE);
insert into lists (name, list, active) VALUES ('Gremios', 'ORGANIZATION_TYPE', TRUE);
insert into lists (name, list, active) VALUES ('Secretaría de Agricultura Departamental o quien haga sus veces', 'ORGANIZATION_TYPE', TRUE);

insert into lists (name, list, active) VALUES ('Giro Postal', 'PAYMENT_TYPE', TRUE);

INSERT INTO products(name, crop_group_id, percentage, maximum_to_subsidize, minimum_to_apply, active, created_at, updated_at) VALUES
('Plátano', 3, 0.0646, 2543025, 254, TRUE, now(), now());
INSERT INTO products(name, crop_group_id, percentage, maximum_to_subsidize, minimum_to_apply, active, created_at, updated_at) VALUES
('Papa', 3, 0.0589, 1745797, 337, TRUE, now(), now());
INSERT INTO products(name, crop_group_id, percentage, maximum_to_subsidize, minimum_to_apply, active, created_at, updated_at) VALUES
('Arroz', 3, 0.0537, 2212077, 243, TRUE, now(), now());
INSERT INTO products(name, crop_group_id, percentage, maximum_to_subsidize, minimum_to_apply, active, created_at, updated_at) VALUES
('Maíz', 3, 0.0472, 4657564, 101, TRUE, now(), now());
INSERT INTO products(name, crop_group_id, percentage, maximum_to_subsidize, minimum_to_apply, active, created_at, updated_at) VALUES
('Yuca', 3, 0.0411, 580320, 708, TRUE, now(), now());