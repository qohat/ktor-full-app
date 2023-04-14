CREATE VIEW specific_balance_view AS
WITH percentage_to_subsidize AS (
  SELECT value FROM config_values WHERE name = 'percentage_to_subsidize'
),
total_supplies_amounts AS (
  SELECT bill_return_id, SUM(total) as total FROM (
      SELECT brs.bill_return_id,
        CASE
            WHEN brs.value > sp.price THEN SUM(sp.price)
            ELSE SUM(brs.value)
        END as total
      FROM bill_returns_supplies brs
      JOIN bill_returns br ON brs.bill_return_id = br.id
      JOIN supplies sp on brs.supply_id = sp.id
      GROUP BY brs.bill_return_id, brs.value, sp.price
  ) as not_grouped_supplies GROUP BY bill_return_id
)
SELECT br.id,
pd.id as product_id,
pd.name as product,
concat(p.name, ' ', p.last_name) as people_name, p.document,
pType.name as payment_type, bId.name as branch, bkId.name as bank,
accId.name as account_type, pi.account_number,
SUM(tsa.total) as total_supplies_amount,
(pts.value * SUM(tsa.total)) as prospect_value_to_subsidize,
CASE
    WHEN (pts.value * SUM(tsa.total)) > pd.maximum_to_subsidize THEN pd.maximum_to_subsidize
    ELSE (pts.value * SUM(tsa.total))
END as value_to_subsidize,
pr.state
FROM bill_returns br
JOIN total_supplies_amounts tsa ON br.id = tsa.bill_return_id
JOIN products pd on pd.id = br.product_id
JOIN people_requests pr on pr.id = br.people_request_id
JOIN people p on p.id = pr.people_id
JOIN payment_information pi on p.id = pi.people_id
JOIN lists pType on pType.id = pi.payment_type
LEFT JOIN lists bId on bId.id = pi.branch_id
LEFT JOIN lists bkId on bkId.id = pi.bank_id
LEFT JOIN lists accId on accId.id = pi.account_bank_type_id
CROSS JOIN percentage_to_subsidize pts
WHERE pr.state IN ('NonPaid', 'Paid', 'Completed')
GROUP BY br.id, pts.value,
tsa.total, p.name, p.last_name, p.document, pd.maximum_to_subsidize, pd.name, pd.id,
pType.name, bId.name, bkId.name, accId.name, pi.account_number, pr.state
ORDER BY br.id;


CREATE VIEW global_balance_view AS
WITH partial_budget_cte AS (
  SELECT value FROM config_values WHERE name = 'partial_budget'
),
specific_balance AS (
    WITH percentage_to_subsidize AS (
      SELECT value FROM config_values WHERE name = 'percentage_to_subsidize'
    ),
    total_supplies_amounts AS (
      SELECT brs.bill_return_id,
        CASE
            WHEN brs.value > sp.price THEN SUM(sp.price)
            ELSE SUM(brs.value)
        END as total
      FROM bill_returns_supplies brs
      JOIN bill_returns br ON brs.bill_return_id = br.id
      JOIN supplies sp on brs.supply_id = sp.id
      GROUP BY brs.bill_return_id, brs.value, sp.price
    )
    SELECT br.id,
    pd.id as product_id,
    pd.name as product,
    SUM(tsa.total) as total_supplies_amount,
    (pts.value * SUM(tsa.total)) as prospect_value_to_subsidize,
    CASE
        WHEN (pts.value * SUM(tsa.total)) > pd.maximum_to_subsidize THEN pd.maximum_to_subsidize
        ELSE (pts.value * SUM(tsa.total))
    END as value_to_subsidize
    FROM bill_returns br
    JOIN total_supplies_amounts tsa ON br.id = tsa.bill_return_id
    JOIN products pd on pd.id = br.product_id
    CROSS JOIN percentage_to_subsidize pts
    GROUP BY br.id, pts.value,
    tsa.total, pd.maximum_to_subsidize, pd.name, pd.id
)
SELECT depts.name as department, COUNT(DISTINCT sb.id) as requests, ch.name as chain,
pd.name,
(pd.percentage * pbc.value) as product_budget,
(cv.value * cv0.value * pbc.value) as dept_chain_budget,
SUM(sb.value_to_subsidize) as total_value_to_subsidize,
((cv.value * cv0.value * pbc.value) - SUM(sb.value_to_subsidize)) as available_chain_budget,
((pd.percentage * pbc.value) - SUM(sb.value_to_subsidize)) as available_product_budget
from bill_returns br
JOIN specific_balance sb on br.id = sb.id
JOIN products pd on pd.id = sb.product_id
JOIN lists cG on cG.id = pd.crop_group_id
JOIN lists ch on cG.list = CONCAT('CROP_GROUP_', ch.name)
JOIN people_requests pr on pr.id = br.people_request_id
JOIN people p on p.id = pr.people_id
JOIN property_information pi on p.id = pi.people_id
JOIN lists depts on depts.id = pi.department_id
JOIN config_values as cv on cv.name = CONCAT('perc_', depts.name)
JOIN config_values as cv0 on cv0.name = CONCAT('budget_', ch.name)
CROSS JOIN partial_budget_cte pbc
WHERE pr.state IN ('NonPaid', 'Paid', 'Completed')
GROUP BY depts.name,
ch.name,
dept_chain_budget,
pd.percentage,
pd.name,
pbc.value;