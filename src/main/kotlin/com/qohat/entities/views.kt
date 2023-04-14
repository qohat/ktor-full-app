import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object PeopleWithPaymentsCount: Table("people_with_payments_count") {
    val one_payment = integer("one_payment").default(0)
    val two_payments = integer("two_payments").default(0)
    val three_payments = integer("three_payments").default(0)
}

object CompaniesAmountDrawn: Table("companies_amount_drawn") {
    val name = varchar("name", 100)
    val total = decimal("total", 2, 2)
}

object PeopleAmountsDrawn: Table("people_amount_drawn") {
    val fullname = varchar("fullname", 100)
    val document = varchar("document", 10)
    val total = decimal("total", 2, 2)
}

object PeopleRelatedToCompanies: Table("people_related_to_companies") {
    val people_name = varchar("people_name", 100)
    val document = varchar("document", 10)
    val company_name = varchar("company_name", 100)
}

object UsersDoingProcess: Table("users_doing_process") {
    val created_by = varchar("created_by", 100)
    val ccf = varchar("ccf", 100)
    val assigned_to = varchar("assigned_to", 100)
    val people_name = varchar("people_name", 100)
    val document = varchar("document", 10)
    val company_name = varchar("company_name", 100)
}

object PaymentsMade: Table("payments_made") {
    val payments_made = integer("payments_made").default(0)
}

object PaymentsApprovals: Table("payments_approvals") {
    val nit = varchar("nit", 10)
    val company_name = varchar("company_name", 100)
    val doc_type = varchar("doc_type", 100)
    val document = varchar("document", 10)
    val name = varchar("name", 50)
    val last_name = varchar("last_name", 50)
    val approval_date = datetime("approval_date")
    val arl_level = varchar("arl_level", 50)
    val value = decimal("value", 2, 2)
    val payment_number = uuid("payment_number")
    val month_applied = varchar("month_applied", 50)
}

object PaymentsCommissionReport: Table("payment_commission_report") {
    val nit = varchar("nit", 10)
    val company_name = varchar("company_name", 100)
    val doc_type = varchar("doc_type", 100)
    val document = varchar("document", 10)
    val name = varchar("name", 50)
    val last_name = varchar("last_name", 50)
    val payment_date = datetime("payment_date")
    val arl_level = varchar("arl_level", 50)
    val value = decimal("value", 2, 2)
    val payment_number = uuid("payment_number")
    val month_applied = varchar("month_applied", 50)
}
