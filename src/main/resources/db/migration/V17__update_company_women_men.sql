UPDATE companies
   SET women = othertable.women,
       men = othertable.men
  FROM (
  select c.id, COALESCE(women_tab.women, 0) as women, COALESCE(men_tab.men, 0) as men from companies c
left join (select pc.company_id as company_id, count(company_id) as women
    from people_companies pc
    inner join people p on (p.id = pc.people_id and p.gender_id = 77)
    group by company_id) women_tab on women_tab.company_id = c.id
left join (select pc.company_id as company_id, count(company_id) as men
    from people_companies pc
    inner join people p on (p.id = pc.people_id and p.gender_id = 76)
    group by company_id) men_tab on men_tab.company_id = c.id
  ) othertable
 WHERE othertable.id = companies.id;