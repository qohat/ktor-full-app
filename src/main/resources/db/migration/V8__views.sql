CREATE VIEW people_with_payments_count AS SELECT DISTINCT
    COALESCE((SELECT count(payments."id") FROM payments HAVING count(payments."id") = 1), 0) AS one_payment,
    COALESCE((SELECT count(payments."id") FROM payments HAVING count(payments."id") = 2), 0) AS two_payments,
    COALESCE((SELECT count(payments."id") FROM payments HAVING count(payments."id") = 3), 0) AS three_payments
FROM payments;

CREATE VIEW companies_amount_drawn AS select companies."name", sum(payments.value) as total from payments
inner join people_companies on people_companies.id = payments.people_companies_id
inner join companies on people_companies.company_id = companies."id"
group by companies."name";

CREATE VIEW people_amount_drawn AS select concat(people."name", ' ', people."last_name") as fullname, people.document, sum(payments.value) as total from payments
inner join people_companies on people_companies.id = payments.people_companies_id
inner join people on people_companies.people_id = people."id"
group by fullname, people.document;

CREATE VIEW people_related_to_companies AS select concat(people."name", ' ', people."last_name") as people_name, people.document, companies."name" as company_name from people_companies
inner join people on people_companies.people_id = people."id"
inner join companies on people_companies.company_id = companies."id"
order by people_name ASC;

CREATE view users_doing_process AS select concat(u0."name", ' ', u0."last_name") as created_by, lists.name as ccf, concat(u1."name", ' ', u1."last_name") as assigned_to,
concat(people."name", ' ', people."last_name") as people_name, people.document, companies."name" as company_name from people_companies
inner join users as u0 on people_companies.created_by_id = u0."id"
inner join lists on u0.ccf_id = lists."id"
inner join users as u1 on people_companies.assigned_to_id = u1."id"
inner join people on people_companies.people_id = people."id"
inner join companies on people_companies.company_id = companies."id";

CREATE VIEW payments_made AS select count(payments.id) as payments_made from payments;

