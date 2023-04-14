CREATE VIEW payments_approvals AS select companies.document as nit, companies.name as company_name, doc_type.name as doc_type,
people."document", people."name", people.last_name,
people_companies.updated_at as approval_date, arl_level.name as arl_level, payments."value",
payments."id" payment_number, concat(payments.month_applied, '/', payments.year_applied) as month_applied
from payments
inner join people_companies on people_companies."id" = payments.people_companies_id
inner join people on people."id" = people_companies.people_id
inner join lists as doc_type on doc_type."id" = people.document_type
inner join lists as arl_level on arl_level."id" = people_companies.arl_level_id
inner join companies on companies."id" = people_companies.company_id;

CREATE VIEW payment_commission_report AS select companies.document as nit, companies.name as company_name, doc_type.name as doc_type,
people."document", people."name", people.last_name,
payments.created_at as payment_date, arl_level.name as arl_level, payments."value",
payments."id" payment_number, concat(payments.month_applied, '/', payments.year_applied) as month_applied
from payments
inner join people_companies on people_companies."id" = payments.people_companies_id
inner join people on people."id" = people_companies.people_id
inner join lists as doc_type on doc_type."id" = people.document_type
inner join lists as arl_level on arl_level."id" = people_companies.arl_level_id
inner join companies on companies."id" = people_companies.company_id;
