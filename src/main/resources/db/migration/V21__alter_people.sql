DROP VIEW specific_balance_view;
ALTER TABLE people DROP CONSTRAINT people_email_key;
ALTER TABLE people ALTER COLUMN address TYPE TEXT;
ALTER TABLE people ALTER COLUMN name TYPE VARCHAR(255);
ALTER TABLE people ALTER COLUMN last_name TYPE VARCHAR(255);
ALTER TABLE people ALTER COLUMN phone TYPE VARCHAR(255);
ALTER TABLE people ALTER COLUMN cell_phone TYPE VARCHAR(255);

ALTER TABLE organization_belonging_information ALTER COLUMN name TYPE VARCHAR(255);
ALTER TABLE organization_belonging_information ALTER COLUMN nit TYPE VARCHAR(255);

ALTER TABLE property_information ALTER COLUMN address TYPE VARCHAR(255);
ALTER TABLE property_information ALTER COLUMN name TYPE VARCHAR(255);
ALTER TABLE property_information ALTER COLUMN lane TYPE VARCHAR(255);






