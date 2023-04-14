package com.qohat.entities

import arrow.core.getOrElse
import at.favre.lib.crypto.bcrypt.BCrypt
import com.qohat.domain.RequestAssignment
import com.qohat.domain.RoleName
import com.qohat.domain.User
import com.qohat.domain.UserId
import com.qohat.entities.Assignments.references
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import java.util.UUID

class PGEnum<T : Enum<T>>(enumTypeName: String, enumValue: T?) : PGobject() {
    init {
        value = enumValue?.name
        type = enumTypeName
    }
}

// SQL
object Lists: Table("lists") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val list = varchar("list", 50)
    val active = bool("active").default(true)

    override val primaryKey = PrimaryKey(id)

    // Aliases
    val CompanyType = Lists.alias("CompanyType")
    val Locality = Lists.alias("Locality")
    val Ccf = Lists.alias("Ccf")
    val CompanySize = Lists.alias("CompanySize")
    val Bank = Lists.alias("Bank")
    val BankBranch = Lists.alias("BankBranch")
    val AccountBankType = Lists.alias("AccountBankType")
    val DocumentType = Lists.alias("DocumentType")
    val Gender = Lists.alias("Gender")
    val PopulationGroup = Lists.alias("PopulationGroup")
    val EthnicGroup = Lists.alias("EthnicGroup")
    val Disability = Lists.alias("Disability")
    val Sex = Lists.alias("Sex")
    val Department = Lists.alias("Department")
    val City = Lists.alias("City")
    val OrganizationType = Lists.alias("OrganizationType")
    val PaymentType = Lists.alias("PaymentType")
    val ContractType = Lists.alias("ContractType")
    val MothRequestApplied = Lists.alias("MothRequestApplied")
    val CurrentMonthApplied = Lists.alias("CurrentMonthApplied")
    val ArlLevel = Lists.alias("ArlLevel")
    val PeopleDocType = Lists.alias("PeopleDocType")
    val RejectPeopleDocReason = Lists.alias("RejectPeopleDocReason")
    val CompanyDocType = Lists.alias("CompanyDocType")
    val RejectCompanyDocReason = Lists.alias("RejectCompanyDocReason")
    val Reason = Lists.alias("Reason")
    val Activity = Lists.alias("Activity")
    val Chain = Lists.alias("Chain")
    val CropGroup = Lists.alias("CropGroup")
    val Files = Lists.alias("Files")
    val MeasurementUnit = Lists.alias("MeasurementUnit")
    val Presentation = Lists.alias("Presentation")
}

object Users: Table("users") {
    val id = uuid("id")
    val name = varchar("name", 50)
    val last_name = varchar("last_name", 50)
    val email = varchar("email", 50)
    val password = varchar("password", 255)
    val document_type = (integer("document_type") references Lists.id)
    val document = varchar("document", 10)
    val address = varchar("address", 100).nullable()
    val role_id = (integer("role_id") references Roles.id)
    val active = bool("active").default(true)
    val recovering_password = bool("recovering_password").default(false)
    val recover_token = uuid("recover_token").nullable()
    val recover_expiration = datetime("recover_expiration").nullable()
    val created_at = datetime("created_at")
    val updated_at = datetime("updated_at")

    val ccf_id = (integer("ccf_id") references Lists.id).nullable()

    override val primaryKey = PrimaryKey(id)

    fun insertBatch(users: List<User>) =
        batchInsert(users, shouldReturnGeneratedValues = false) {
            this[id] = UUID.randomUUID()
            this[name] = it.name
            this[last_name] = it.lastName
            this[email] = it.email
            this[password] = BCrypt.withDefaults().hashToString(12, it.password.toCharArray())
            this[document_type] = it.documentType.id
            this[document] = it.document
            this[address] = it.address.getOrElse { null }
            this[role_id] = it.role.id.value
            this[active] = true
            //TODO Review this
            this[ccf_id] = it.ccf.value
            this[created_at] = LocalDateTime.now()
            this[updated_at] = LocalDateTime.now()
        }
}

object Companies: Table("companies") {
    val id = uuid("id")
    val name = varchar("name", 100)
    val company_type_id = (integer("company_type_id") references Lists.id)
    val document = varchar("document", 17) //809777654-1
    val address = varchar("address", 50)
    val locality_id = (integer("locality_id") references Lists.id)
    val neighborhood = varchar("neighborhood", 100).nullable()
    val phone = varchar("phone", 20).nullable()
    val cell_phone = varchar("cell_phone", 21).nullable()
    val email = varchar("email", 50)
    val employees = integer("employees").default(0)
    val company_size_id = (integer("company_size_id") references Lists.id)
    val economic_activity_code = varchar("economic_activity_code", 10).nullable()
    val ccf_id = (integer("ccf_id") references Lists.id)
    val legal_representative = varchar("legal_representative", 100).nullable()
    val legal_representative_document = varchar("legal_representative_document", 10).nullable()
    val postulation_responsible = varchar("postulation_responsible", 100).nullable()
    val postulation_responsible_phone = varchar("postulation_responsible_phone", 20).nullable()
    val postulation_responsible_position = varchar("postulation_responsible_position", 50).nullable()
    val postulation_responsible_email = varchar("postulation_responsible_email", 50).nullable()
    val bank_id = (integer("bank_id") references Lists.id)
    val account_bank_type_id = (integer("account_bank_type_id") references Lists.id)
    val account_number = varchar("account_number", 16)
    val active = bool("active").default(true)
    val created_at = datetime("created_at")
    val updated_at = datetime("updated_at")

    val created_by_id = (uuid("created_by_id") references Users.id)

    val women = integer("women").default(0)
    val men = integer("men").default(0)

    override val primaryKey = PrimaryKey(id)
}

object CompaniesAttachment: Table("companies_attachments") {
    val id = uuid("id")
    val company_id = (uuid("company_id") references Companies.id)
    val name = varchar("name", 255)
    val path = varchar("path", 255)
    val state = varchar("state", 10).default("InReview")
    val active = bool("active").default(true)
    val company_file_id = (integer("company_file_id") references Lists.id)
    val created_at = datetime("created_at")
    val updated_at = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)

    val CompanyAttachmentCountVal = this.company_id.count().alias("CompanyAttachmentCountVal")

    val CompanyAttachmentCount =
        CompaniesAttachment.slice(this.company_id, CompanyAttachmentCountVal)
            .selectAll()
            .groupBy(this.company_id)
            .alias("CompanyAttachmentCount")
}

object ValidationCompaniesAttachmentsEvents: Table("validation_companies_attachments_events") {

    val id = uuid("id")
    val companies_attachments_id = (uuid("companies_attachments_id") references CompaniesAttachment.id)
    val user_id = (uuid("user_id") references Users.id)
    val observation = text("observation")
    val state = varchar("state", 10)
    val reason_id = (integer("reason_id") references Lists.id)
    val created_at = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
object PeopleCompanies: Table("people_companies") {
    val id = uuid("id")
    val people_id = (uuid("people_id") references People.id)
    val company_id = (uuid("company_id") references Companies.id)
    val contract_type_id = (integer("contract_type_id") references Lists.id)
    val duration = integer("duration").default(0)
    val start_date = date("start_date")
    val end_date = date("end_date")
    val monthly_income = integer("monthly_income")
    val month_request_applied_id = (integer("month_request_applied_id") references Lists.id)
    val current_month_applied = (integer("current_month_applied_id") references Lists.id)
    val arl_level_id = (integer("arl_level_id") references Lists.id)
    val state = varchar("state", 10).default("InReview")
    val active = bool("active").default(true)
    val created_by_id = (uuid("created_by_id") references Users.id)
    val assigned_to_id = (uuid("assigned_to_id") references Users.id)
    val created_at = datetime("created_at")
    val updated_at = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}
object PeopleAttachments: Table("people_attachments") {
    val id = uuid("id")
    val people_companies_id = (uuid("people_companies_id") references PeopleCompanies.id)
    val name = varchar("name", 255)
    val path = varchar("path", 255)
    val state = varchar("state", 10).default("InReview")
    val active = bool("active").default(true)
    val people_file_id = (integer("people_file_id") references Lists.id)
    val created_at = datetime("created_at")
    val updated_at = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)

    val PeopleAttachmentCountVal = people_companies_id.count().alias("PeopleAttachmentCountVal")

    val PeopleAttachmentCount =
        PeopleAttachments.slice(people_companies_id, PeopleAttachmentCountVal)
            .selectAll()
            .groupBy(people_companies_id)
            .alias("PeopleAttachmentCount")
}
object ValidationPeopleAttachmentsEvents: Table("validation_people_attachments_events") {

    val id = uuid("id")
    val people_attachments_id = (uuid("people_attachments_id") references CompaniesAttachment.id)
    val user_id = (uuid("user_id") references Users.id)
    val observation = text("observation")
    val state = varchar("state", 10)
    val reason_id = (integer("reason_id") references Lists.id)
    val created_at = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
object Payments: Table("payments") {
    val id = uuid("id")
    val people_companies_id = (uuid("people_companies_id") references PeopleCompanies.id)
    val base_income = decimal("base_income", precision = 2, scale = 2)
    val value = decimal("value", precision = 2, scale = 2)
    val month_applied = varchar("month_applied", 3)
    val year_applied = integer("year_applied")
    val p_date = date("p_date").nullable()
    val p_number = integer("p_number").nullable()
    val created_at = datetime("created_at")
}
object Assignments: Table("user_assignments") {
    val user_id = (uuid("user_id") references Users.id)
    val people_request_id = uuid("people_request_id").references(PeopleRequests.id)

    private fun query(role: RoleName) = join(Users, JoinType.INNER, user_id, Users.id)
        .join(Roles, JoinType.INNER, Roles.id, Users.role_id)
        .slice(user_id, people_request_id.count())
        .select { Roles.name.lowerCase().eq(role.value) }
        .groupBy(user_id)
        .orderBy(people_request_id.count() to SortOrder.ASC)

    val selectAll = slice(user_id, people_request_id.count())
        .selectAll()
        .groupBy(user_id)
        .orderBy(people_request_id.count() to SortOrder.ASC)

    fun selectBy(role: RoleName) = query(role).map { it }

    fun insertBatch(assignments: List<RequestAssignment>) =
        batchInsert(assignments, shouldReturnGeneratedValues = false) {
            this[user_id] = it.userId.value
            this[people_request_id] = it.peopleRequestId.value
        }

    fun insertQuery(assignment: RequestAssignment) =
        insert {
            it[user_id] = assignment.userId.value
            it[people_request_id] = assignment.peopleRequestId.value
        }
}

// The below code is just a copy-paste that should actually be in the lib
class RunSqlStatement(table: Table, private val statement: String? = null) : InsertStatement<Number>(table) {
    override fun prepareSQL(transaction: Transaction): String {
        return statement?.let {
            super.prepareSQL(transaction) + statement
        } ?: super.prepareSQL(transaction)
    }
}

class RunBatchSqlStatement(table: Table): BatchInsertStatement(table, false)

fun <T : Table> T.insertWithRawStatement(body: T.(RunSqlStatement) -> Unit, statement: () -> String): InsertStatement<Number> =
    RunSqlStatement(this, statement.invoke()).apply {
        body(this)
        TransactionManager.current().exec(this)
    }