insert into users (id, name, last_name, email, password, document_type, document, address, role_id, active, permission_chain, created_at, updated_at)
VALUES (
'aea3366f-f1e7-475a-b57a-ef65e71a4aad',
'Admin',
'Admin',
'admin@email.com',
'$2a$12$pF2TVOC7UctKQZjYHbzCtuqCyLek2G1RY0TPqRfuGDZoMYB8rpveW',
1,
'11111111',
'Address',
1,
TRUE,
'RdU:WtU:DltU:UptU',
now(),
now()
);