DELETE FROM lists WHERE name = 'lts' and list = 'MEASUREMENT_UNIT';
DELETE FROM lists WHERE name = 'ml' and list = 'MEASUREMENT_UNIT';
DELETE FROM lists WHERE name = 'cm3' and list = 'MEASUREMENT_UNIT';
DELETE FROM lists WHERE name = 'mg' and list = 'MEASUREMENT_UNIT';

UPDATE lists set name = 'Ton' WHERE name = 'lbs' and list = 'MEASUREMENT_UNIT';
UPDATE lists set name = 'gr' WHERE name = 'oz' and list = 'MEASUREMENT_UNIT';