package com.qohat.entities

import com.qohat.domain.Document
import com.qohat.domain.Email
import com.qohat.domain.Limit
import com.qohat.domain.NewPaginationParams
import com.qohat.domain.NewPeople
import com.qohat.domain.NewPeopleId
import com.qohat.domain.Offset
import com.qohat.domain.OrganizationBelongingInfo
import com.qohat.domain.PeopleRequestNumber
import com.qohat.domain.ProductId
import com.qohat.domain.UserId
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.orWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.UUID

object People: Table("people") {
    val id = uuid("id")
    val name = varchar("name", 255)
    val last_name = varchar("last_name", 255)
    val document_type = (integer("document_type") references Lists.id)
    val document = varchar("document", 15)
    val issue_document_date = date("issue_document_date").nullable()
    val birthday = date("birthday")
    val gender_id = (integer("gender_id") references Lists.id)
    val address = varchar("address", 255).nullable()
    val locality_id = (integer("locality_id") references Lists.id).nullable()
    val neighborhood = varchar("neighborhood", 100).nullable()
    val phone = varchar("phone", 20).nullable()
    val cell_phone = varchar("cell_phone", 21).nullable()
    val email = varchar("email", 50)
    val population_group_id = (integer("population_group_id") references Lists.id)
    val ethnic_group_id = (integer("ethnic_group_id") references Lists.id)
    val disability_id = (integer("disability_id") references Lists.id)
    val sex_id = (integer("sex_id") references Lists.id)
    val armed_conflict_victim = bool("armed_conflict_victim").default(false).nullable()
    val displaced = bool("displaced").default(false).nullable()
    val terms_acceptance = bool("terms_acceptance").default(true)
    val single_mother = bool("single_mother").default(false).nullable()
    val belongs_organization = bool("belongs_organization").default(false).nullable()
    val active = bool("active").default(true)
    val created_at = datetime("created_at")
    val updated_at = datetime("updated_at")

    val created_by_id = (uuid("created_by_id") references Users.id).nullable()

    override val primaryKey = PrimaryKey(id)

    private val query = join(Lists.Locality, JoinType.LEFT, locality_id, Lists.Locality[Lists.id])
        .join(Lists.DocumentType, JoinType.INNER, document_type, Lists.DocumentType[Lists.id])
        .join(Lists.Gender, JoinType.INNER, gender_id, Lists.Gender[Lists.id])
        .join(Lists.PopulationGroup, JoinType.INNER, population_group_id, Lists.PopulationGroup[Lists.id])
        .join(Lists.EthnicGroup, JoinType.INNER, ethnic_group_id, Lists.EthnicGroup[Lists.id])
        .join(Lists.Disability, JoinType.INNER, disability_id, Lists.Disability[Lists.id])
        .join(Lists.Sex, JoinType.INNER, sex_id, Lists.Sex[Lists.id])
        .join(PropertyInformation, JoinType.LEFT, id, PropertyInformation.people_id)
        .join(Lists.Department, JoinType.LEFT, PropertyInformation.department, Lists.Department[Lists.id])
        .join(Lists.City, JoinType.LEFT, PropertyInformation.city, Lists.City[Lists.id])
        .join(OrganizationBelongingInformation, JoinType.LEFT, id, OrganizationBelongingInformation.people_id)
        .join(Lists.OrganizationType, JoinType.LEFT, OrganizationBelongingInformation.type, Lists.OrganizationType[Lists.id])
        .join(PaymentInformation, JoinType.LEFT, id, PaymentInformation.people_id)
        .join(Lists.PaymentType, JoinType.LEFT, PaymentInformation.payment_type, Lists.PaymentType[Lists.id])
        .join(Lists.Bank, JoinType.LEFT, PaymentInformation.bank_id, Lists.Bank[Lists.id])
        .join(Lists.BankBranch, JoinType.LEFT, PaymentInformation.branch_id, Lists.BankBranch[Lists.id])
        .join(Lists.AccountBankType, JoinType.LEFT, PaymentInformation.account_bank_type_id, Lists.AccountBankType[Lists.id])

    fun insertQuery(people: NewPeople) =
        insert {
            it[id] = UUID.randomUUID()
            it[name] = people.name.value
            it[last_name] = people.lastName.value
            it[document_type] = people.documentType.id
            it[document] = people.document.value
            it[issue_document_date] = people.issueDocumentDate?.value
            it[birthday] = people.birthday.value
            it[gender_id] = people.gender.id
            it[address] = people.address?.value
            it[locality_id] = null
            it[neighborhood] = null
            it[phone] = people.phone?.value
            it[cell_phone] = people.cellPhone?.value
            it[email] = people.email.value
            it[population_group_id] =people.populationGroup.id
            it[ethnic_group_id] = people.ethnicGroup.id
            it[disability_id] = people.disability.id
            it[sex_id] = people.sex.id
            it[armed_conflict_victim] = people.armedConflictVictim?.value
            it[displaced] = people.displaced?.value
            it[terms_acceptance] = people.termsAcceptance.value
            it[single_mother] = people.singleMother?.value
            it[belongs_organization] = people.belongsOrganization?.value
            it[active] = true
            it[created_at] = LocalDateTime.now()
            it[updated_at] = LocalDateTime.now()
            it[created_by_id] = people.createdBy?.value
        } get id

    fun selectByQuery(userEmail: Email): ResultRow? = query
        .select { email.eq(userEmail.value) }
        .singleOrNull()

    fun selectByQuery(newPeopleId: NewPeopleId): ResultRow? = query
        .select { id.eq(newPeopleId.value) }
        .singleOrNull()

    fun selectByQuery(newDocument: Document): ResultRow? = query
        .select { document.eq(newDocument.value) }
        .singleOrNull()

    fun selectAllQuery(params: NewPaginationParams, userId: UserId? = null): List<ResultRow> {
        val select = query.selectAll()

        params.text?.let {
            val text = it.value.lowercase()
            select.andWhere {
                concat(
                    separator = " ",
                    listOf(name.lowerCase(), last_name.lowerCase())
                ) like "%${text}%"
            }.orWhere {
                document like "%${text}%"
            }.orWhere {
                email.lowerCase() like "%${text}%"
            }
        }

        userId?.let {
            select.andWhere { created_by_id.eq(it.value) }
        }

        return select
            .orderBy(updated_at to SortOrder.DESC)
            .limit(params.pagination.limit.value, params.pagination.offset.value)
            .map { it }
    }

    fun updateQuery(newPeopleId: NewPeopleId, people: NewPeople) =
        update({id.eq(newPeopleId.value)}) {
            it[name] = people.name.value
            it[last_name] = people.lastName.value
            it[document_type] = people.documentType.id
            it[document] = people.document.value
            it[issue_document_date] = people.issueDocumentDate?.value
            it[birthday] = people.birthday.value
            it[gender_id] = people.gender.id
            it[address] = people.address?.value
            it[locality_id] = null
            it[neighborhood] = null
            it[phone] = people.phone?.value
            it[cell_phone] = people.cellPhone?.value
            it[email] = people.email.value
            it[population_group_id] =people.populationGroup.id
            it[ethnic_group_id] = people.ethnicGroup.id
            it[disability_id] = people.disability.id
            it[sex_id] = people.sex.id
            it[armed_conflict_victim] = people.armedConflictVictim?.value
            it[displaced] = people.displaced?.value
            it[terms_acceptance] = people.termsAcceptance.value
            it[single_mother] = people.singleMother?.value
            it[belongs_organization] = people.belongsOrganization?.value
            it[updated_at] = LocalDateTime.now()
        }
}
object PropertyInformation: Table("property_information") {
    val people_id = (uuid("people_id") references People.id)
    val address = varchar("address", 255)
    val name = varchar("name", 255)
    val department = (integer("department_id") references Lists.id)
    val city = (integer("city_id") references Lists.id)
    val lane = varchar("lane", 255).nullable()
    val hectares = decimal("hectares", 4, 3)
    override val primaryKey = PrimaryKey(people_id)

    fun insertQuery(peopleId: NewPeopleId, pI: com.qohat.domain.PropertyInformation) =
        insert {
            it[people_id] = peopleId.value
            it[address] = pI.address.value
            it[name] = pI.name.value
            it[department] = pI.department.id
            it[city] = pI.city.id
            it[lane] = pI.lane?.value
            it[hectares] = pI.hectares.value
        }

    fun updateQuery(peopleId: NewPeopleId, pI: com.qohat.domain.PropertyInformation) =
        update({ people_id.eq(peopleId.value) }) {
            it[address] = pI.address.value
            it[name] = pI.name.value
            it[department] = pI.department.id
            it[city] = pI.city.id
            it[lane] = pI.lane?.value
            it[hectares] = pI.hectares.value
        }

}
object OrganizationBelongingInformation: Table("organization_belonging_information") {
    val people_id = (uuid("people_id") references People.id)
    val type = (integer("type") references Lists.id)
    val name = varchar("name", 255)
    val nit = varchar("nit", 17).nullable()
    override val primaryKey = PrimaryKey(people_id)

    fun insertQuery(peopleId: NewPeopleId, oI: OrganizationBelongingInfo) =
        insert {
            it[people_id] = peopleId.value
            it[type] = oI.type.id
            it[name] = oI.name.value
            it[nit] = oI.nit?.value
        }

    fun updateQuery(peopleId: NewPeopleId, oI: OrganizationBelongingInfo) =
        update({ people_id.eq(peopleId.value) }) {
            it[type] = oI.type.id
            it[name] = oI.name.value
            it[nit] = oI.nit?.value
        }
}
object PaymentInformation: Table("payment_information") {
    val people_id = (uuid("people_id") references People.id)
    val payment_type = (integer("payment_type") references Lists.id)
    val bank_id = (integer("bank_id") references Lists.id).nullable()
    val branch_id = (integer("branch_id") references Lists.id).nullable()
    val account_bank_type_id = (integer("account_bank_type_id") references Lists.id).nullable()
    val account_number = varchar("account_number", 16).nullable()
    override val primaryKey = PrimaryKey(people_id)

    fun insertQuery(peopleId: NewPeopleId, pI: com.qohat.domain.PaymentInformation) =
        insert {
            it[people_id] = peopleId.value
            it[payment_type] = pI.paymentType.id
            it[account_bank_type_id] = pI.accountType?.id
            it[bank_id] = pI.bank?.id
            it[branch_id] = pI.branch?.id
            it[account_number] = pI.accountNumber?.value
        }

    fun updateQuery(peopleId: NewPeopleId, pI: com.qohat.domain.PaymentInformation) =
        update({ people_id.eq(peopleId.value) }) {
            it[payment_type] = pI.paymentType.id
            it[account_bank_type_id] = pI.accountType?.id
            it[bank_id] = pI.bank?.id
            it[branch_id] = pI.branch?.id
            it[account_number] = pI.accountNumber?.value
        }
}

@Serializable
data class TempUserProduct(
    val number: PeopleRequestNumber,
    val document: Document,
    val productId: ProductId
)

fun ResultRow.toTempUserProduct() =
    TempUserProduct(
        number = PeopleRequestNumber(this[TempUserProducts.request_number]),
        document = Document(this[TempUserProducts.document]),
        productId = ProductId(this[TempUserProducts.product_id])
    )
object TempUserProducts: Table("temp_user_product") {
    val request_number = long("request_number")
    val document = varchar("document", 100)
    val product_id = long("product_id")

    fun selectQuery(limit: Limit, offset: Offset): List<ResultRow> =
        selectAll()
        .limit(limit.value, offset.value)
        .map { it }

    fun deleteQuery(peopleRequestNumber: PeopleRequestNumber, doc: Document) =
        deleteWhere { request_number.eq(peopleRequestNumber.value) and document.eq(doc.value) }
}