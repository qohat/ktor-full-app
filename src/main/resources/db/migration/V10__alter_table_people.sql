DROP VIEW fiduciaria.people_amount_drawn;
DROP VIEW fiduciaria.people_related_to_companies;
DROP VIEW fiduciaria.users_doing_process;

ALTER TABLE fiduciaria.people
ALTER COLUMN document type varchar(15);

CREATE OR REPLACE VIEW fiduciaria.people_amount_drawn
 AS
 SELECT concat(people.name, ' ', people.last_name) AS fullname,
    people.document,
    sum(payments.value) AS total
   FROM fiduciaria.payments
     JOIN fiduciaria.people_companies ON people_companies.id = payments.people_companies_id
     JOIN fiduciaria.people ON people_companies.people_id = people.id
  GROUP BY (concat(people.name, ' ', people.last_name)), people.document;

ALTER TABLE fiduciaria.people_amount_drawn
    OWNER TO postgres;

CREATE OR REPLACE VIEW fiduciaria.people_related_to_companies
 AS
 SELECT concat(people.name, ' ', people.last_name) AS people_name,
    people.document,
    companies.name AS company_name
   FROM fiduciaria.people_companies
     JOIN fiduciaria.people ON people_companies.people_id = people.id
     JOIN fiduciaria.companies ON people_companies.company_id = companies.id
  ORDER BY (concat(people.name, ' ', people.last_name));

ALTER TABLE fiduciaria.people_related_to_companies
    OWNER TO postgres;

CREATE OR REPLACE VIEW fiduciaria.users_doing_process
 AS
 SELECT concat(u0.name, ' ', u0.last_name) AS created_by,
    lists.name AS ccf,
    concat(u1.name, ' ', u1.last_name) AS assigned_to,
    concat(people.name, ' ', people.last_name) AS people_name,
    people.document,
    companies.name AS company_name
   FROM fiduciaria.people_companies
     JOIN fiduciaria.users u0 ON people_companies.created_by_id = u0.id
     JOIN fiduciaria.lists ON u0.ccf_id = lists.id
     JOIN fiduciaria.users u1 ON people_companies.assigned_to_id = u1.id
     JOIN fiduciaria.people ON people_companies.people_id = people.id
     JOIN fiduciaria.companies ON people_companies.company_id = companies.id;

ALTER TABLE fiduciaria.users_doing_process
    OWNER TO postgres;
