UPDATE people SET created_by_id = 'aea3366f-f1e7-475a-b57a-ef65e71a4aea' WHERE id IN (
  SELECT id FROM (
    SELECT id, NTILE(8) OVER (ORDER BY id) AS user_num FROM people
  ) AS numbered_rows WHERE user_num = 1
);

UPDATE people SET created_by_id = 'aea3366f-f1e7-475a-b57a-ef65e71a4aeb' WHERE id IN (
  SELECT id FROM (
    SELECT id, NTILE(8) OVER (ORDER BY id) AS user_num FROM people
  ) AS numbered_rows WHERE user_num = 2
);

UPDATE people SET created_by_id = 'aea3366f-f1e7-475a-b57a-ef65e71a4aec' WHERE id IN (
  SELECT id FROM (
    SELECT id, NTILE(8) OVER (ORDER BY id) AS user_num FROM people
  ) AS numbered_rows WHERE user_num = 3
);

UPDATE people SET created_by_id = 'aea3366f-f1e7-475a-b57a-ef65e71a4ade' WHERE id IN (
  SELECT id FROM (
    SELECT id, NTILE(8) OVER (ORDER BY id) AS user_num FROM people
  ) AS numbered_rows WHERE user_num = 4
);

UPDATE people SET created_by_id = 'aea3366f-f1e7-475a-b57a-ef65e71a4aee' WHERE id IN (
  SELECT id FROM (
    SELECT id, NTILE(8) OVER (ORDER BY id) AS user_num FROM people
  ) AS numbered_rows WHERE user_num = 5
);

UPDATE people SET created_by_id = 'aea3366f-f1e7-475a-b57a-ef65e71a4aef' WHERE id IN (
  SELECT id FROM (
    SELECT id, NTILE(8) OVER (ORDER BY id) AS user_num FROM people
  ) AS numbered_rows WHERE user_num = 6
);

UPDATE people SET created_by_id = 'aea3366f-f1e7-475a-b57a-ef65e71a4eaa' WHERE id IN (
  SELECT id FROM (
    SELECT id, NTILE(8) OVER (ORDER BY id) AS user_num FROM people
  ) AS numbered_rows WHERE user_num = 7
);

UPDATE people SET created_by_id = 'aea3366f-f1e7-475a-b57a-ef65e71a4afe' WHERE id IN (
  SELECT id FROM (
    SELECT id, NTILE(8) OVER (ORDER BY id) AS user_num FROM people
  ) AS numbered_rows WHERE user_num = 8
);
