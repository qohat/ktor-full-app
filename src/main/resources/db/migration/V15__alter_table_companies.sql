DROP VIEW fiduciaria.payments_approvals;
DROP VIEW fiduciaria.payment_commission_report;

ALTER TABLE fiduciaria.companies
ALTER COLUMN document type varchar(17);

CREATE OR REPLACE VIEW fiduciaria.payments_approvals
 AS
 SELECT companies.document AS nit,
    companies.name AS company_name,
    doc_type.name AS doc_type,
    people.document,
    people.name,
    people.last_name,
    people_companies.updated_at AS approval_date,
    arl_level.name AS arl_level,
    payments.value,
    payments.id AS payment_number,
    concat(payments.month_applied, '/', payments.year_applied) AS month_applied
   FROM fiduciaria.payments
     JOIN fiduciaria.people_companies ON people_companies.id = payments.people_companies_id
     JOIN fiduciaria.people ON people.id = people_companies.people_id
     JOIN fiduciaria.lists doc_type ON doc_type.id = people.document_type
     JOIN fiduciaria.lists arl_level ON arl_level.id = people_companies.arl_level_id
     JOIN fiduciaria.companies ON companies.id = people_companies.company_id;

ALTER TABLE fiduciaria.payments_approvals
    OWNER TO postgres;

CREATE OR REPLACE VIEW fiduciaria.payment_commission_report
 AS
 SELECT companies.document AS nit,
    companies.name AS company_name,
    doc_type.name AS doc_type,
    people.document,
    people.name,
    people.last_name,
    payments.created_at AS payment_date,
    arl_level.name AS arl_level,
    payments.value,
    payments.id AS payment_number,
    concat(payments.month_applied, '/', payments.year_applied) AS month_applied
   FROM fiduciaria.payments
     JOIN fiduciaria.people_companies ON people_companies.id = payments.people_companies_id
     JOIN fiduciaria.people ON people.id = people_companies.people_id
     JOIN fiduciaria.lists doc_type ON doc_type.id = people.document_type
     JOIN fiduciaria.lists arl_level ON arl_level.id = people_companies.arl_level_id
     JOIN fiduciaria.companies ON companies.id = people_companies.company_id;

ALTER TABLE fiduciaria.payment_commission_report
    OWNER TO postgres;