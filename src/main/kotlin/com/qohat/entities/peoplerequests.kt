package com.qohat.entities

import com.qohat.domain.NewPaginationParams
import com.qohat.domain.NewPeopleId
import com.qohat.domain.PeopleRequest
import com.qohat.domain.PeopleRequestExpiration
import com.qohat.domain.PeopleRequestId
import com.qohat.domain.RequestExpiration
import com.qohat.domain.RequestState
import com.qohat.domain.RequestType
import com.qohat.domain.ResponseExpiration
import com.qohat.domain.UserId
import com.qohat.entities.Storages.StorageCity
import com.qohat.entities.Storages.StorageDepartment
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Concat
import org.jetbrains.exposed.sql.FieldSet
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.orWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.BatchUpdateStatement
import org.jetbrains.exposed.sql.stringLiteral
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.UUID


object PeopleRequests: IdTable<UUID>("people_requests") {
    override val id: Column<EntityID<UUID>> = uuid("id").autoGenerate().entityId()
    val people_id = (uuid("people_id") references People.id)
    val number = long("number")
    val type = customEnumeration("type", "RequestType", {value -> RequestType.valueOf(value as String)}, { PGEnum("RequestType", it) })
    val state = customEnumeration("state", "RequestState", {value -> RequestState.valueOf(value as String)}, { PGEnum("RequestState", it) })
    val active = bool("active").default(true)
    val created_at = datetime("created_at")
    val updated_at = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)

    private fun query(requestType: RequestType, userId: UserId?): FieldSet {
        val query = when(requestType) {
            RequestType.BILL_RETURN_REQUEST -> {
                val q =
                join(BillReturns, JoinType.INNER, id, BillReturns.people_request_id)
                .join(People, JoinType.INNER, people_id, People.id)
                .join(Storages, JoinType.INNER, BillReturns.storage_id, Storages.id)
                .join(Products, JoinType.INNER, BillReturns.product_id, Products.id)
                .join(Lists.CropGroup, JoinType.INNER, Products.crop_group_id, Lists.CropGroup[Lists.id])
                .join(Lists.Chain, JoinType.INNER, Lists.CropGroup[Lists.list], Concat("_", stringLiteral("CROP_GROUP"), Lists.Chain[Lists.name]))
                .join(Lists.Activity, JoinType.LEFT, Storages.activity_id, Lists.Activity[Lists.id])
                .join(StorageDepartment, JoinType.INNER, Storages.department, StorageDepartment[Lists.id])
                .join(StorageCity, JoinType.INNER, Storages.city, StorageCity[Lists.id])
                .join(Lists.Locality, JoinType.LEFT, People.locality_id, Lists.Locality[Lists.id])
                .join(Lists.DocumentType, JoinType.INNER, People.document_type, Lists.DocumentType[Lists.id])
                .join(Lists.Gender, JoinType.INNER, People.gender_id, Lists.Gender[Lists.id])
                .join(Lists.PopulationGroup, JoinType.INNER, People.population_group_id, Lists.PopulationGroup[Lists.id])
                .join(Lists.EthnicGroup, JoinType.INNER, People.ethnic_group_id, Lists.EthnicGroup[Lists.id])
                .join(Lists.Disability, JoinType.INNER, People.disability_id, Lists.Disability[Lists.id])
                .join(Lists.Sex, JoinType.INNER, People.sex_id, Lists.Sex[Lists.id])
                .join(PropertyInformation, JoinType.LEFT, People.id, PropertyInformation.people_id)
                .join(Lists.Department, JoinType.LEFT, PropertyInformation.department, Lists.Department[Lists.id])
                .join(Lists.City, JoinType.LEFT, PropertyInformation.city, Lists.City[Lists.id])
                .join(OrganizationBelongingInformation, JoinType.LEFT, People.id, OrganizationBelongingInformation.people_id)
                .join(Lists.OrganizationType, JoinType.LEFT, OrganizationBelongingInformation.type, Lists.OrganizationType[Lists.id])
                .join(PaymentInformation, JoinType.LEFT, People.id, PaymentInformation.people_id)
                .join(Lists.PaymentType, JoinType.LEFT, PaymentInformation.payment_type, Lists.PaymentType[Lists.id])
                .join(Lists.Bank, JoinType.LEFT, PaymentInformation.bank_id, Lists.Bank[Lists.id])
                .join(Lists.AccountBankType, JoinType.LEFT, PaymentInformation.account_bank_type_id, Lists.AccountBankType[Lists.id])
                .join(Lists.BankBranch, JoinType.LEFT, PaymentInformation.branch_id, Lists.BankBranch[Lists.id])

                userId?.let {
                    q.join(Assignments, JoinType.INNER, id, Assignments.people_request_id)
                } ?: q
            }
        }

        val listTablesFields = listOf(
            Lists.Locality,
            Lists.DocumentType,
            Lists.Gender,
            Lists.PopulationGroup,
            Lists.EthnicGroup,
            Lists.Disability,
            Lists.Sex,
            Lists.Department,
            Lists.City,
            Lists.OrganizationType,
            Lists.PaymentType,
            Lists.Bank,
            Lists.AccountBankType,
            Lists.BankBranch,
            People,
            OrganizationBelongingInformation,
            PropertyInformation,
            PaymentInformation,
            Products
        )
        .map { it.fields }
        .flatten()

        val fields = listOf(id,
            BillReturns.id,
            Storages.id,
            Storages.name,
            Storages.document,
            Storages.address,
            Storages.active,
            Storages.created_at,
            Storages.register_number,
            Storages.email,
            Storages.phone,
            Lists.Activity[Lists.name],
            StorageCity[Lists.name],
            StorageDepartment[Lists.name],
            Lists.Chain[Lists.name],
            Lists.CropGroup[Lists.name],
            number,
            type,
            state,
            created_at
        ) + listTablesFields

        return query.slice(fields)
    }

    fun insertQuery(peopleRequest: PeopleRequest, requestType: RequestType): UUID =
        (insert {
            it[people_id] = peopleRequest.peopleId.value
            it[type] = requestType
            it[state] = RequestState.Created
            it[active] = true
            it[created_at] = LocalDateTime.now()
            it[updated_at] = LocalDateTime.now()
        } get id).value

    fun updateStateQuery(peopleRequestId: PeopleRequestId, requestState: RequestState): Int =
        update ({ id.eq(peopleRequestId.value) }) {
            it[state] = requestState
            it[updated_at] = LocalDateTime.now()
        }

    fun deleteQuery(peopleRequestId: PeopleRequestId): Int =
        update ({ id.eq(peopleRequestId.value) }) {
            it[active] = false
            it[updated_at] = LocalDateTime.now()
        }

    fun selectBy(peopleRequestId: PeopleRequestId, requestType: RequestType): ResultRow? =
        query(requestType, null)
            .select(id.eq(peopleRequestId.value))
            .singleOrNull()

    fun selectAllBy(peopleId: NewPeopleId, requestType: RequestType): List<ResultRow> =
        query(requestType, null)
            .select(people_id.eq(peopleId.value))
            .map { it }

    fun selectAllBy(reqState: RequestState, requestType: RequestType, params: NewPaginationParams, userId: UserId? = null): List<ResultRow> {
        val select = query(requestType, userId).selectAll()

        params.text?.let {
            val text = it.value.lowercase()
            select.andWhere {
                concat(
                    separator = " ",
                    listOf(People.name.lowerCase(), People.last_name.lowerCase())
                ) like "%${text}%"
            }.orWhere {
                People.document like "%${text}%"
            }.orWhere {
                Lists.CropGroup[Lists.name].lowerCase() like "%${text}%"
            }.orWhere {
                Products.name.lowerCase() like "%${text}%"
            }.orWhere {
                Lists.Chain[Lists.name].lowerCase() like "%${text}%"
            }.orWhere {
                number.castTo<String>(TextColumnType()) like "%${text}%"
            }
        }

        userId?.let {
            select.andWhere { Assignments.user_id.eq(it.value) }
        }

        return select
            .andWhere { state.eq(reqState) and type.eq(requestType) and active.eq(true) }
            .orderBy(updated_at to SortOrder.DESC)
            .limit(params.pagination.limit.value, params.pagination.offset.value)
            .map { it }
    }

    fun updateStateBatchQuery(peopleRequestIds: List<PeopleRequestId>, requestState: RequestState): Unit =
        BatchUpdateStatement(PeopleRequests).run {
            peopleRequestIds.forEach {
                addBatch(EntityID(it.value, PeopleRequests))
                update({ id.eq(it.value) }) {
                    it[state] = requestState
                }
            }
        }
}

object PeopleRequestExpirations: Table("people_request_expirations") {
    val people_request_id = uuid("people_request_id").references(PeopleRequests.id)
    val request_expiration = date("request_expiration")
    val response_expiration = date("response_expiration").nullable()
    val created_at = datetime("created_at")

    override val primaryKey = PrimaryKey(people_request_id, request_expiration)
    fun insertQuery(peopleRequestId: PeopleRequestId, expiration: PeopleRequestExpiration) =
        insert {
            it[people_request_id] = peopleRequestId.value
            it[request_expiration] = expiration.requestExpiration.value
            it[response_expiration] = expiration.responseExpiration?.value
            it[created_at] = LocalDateTime.now()
        }
    fun updateQuery(peopleRequestId: PeopleRequestId, requestExpiration: RequestExpiration, responseExpiration: ResponseExpiration) =
        update ({ people_request_id.eq(peopleRequestId.value) and request_expiration.eq(requestExpiration.value) }) {
            it[response_expiration] = responseExpiration.value
        }
    fun selectQuery(peopleRequestId: PeopleRequestId): List<ResultRow> =
        select { people_request_id.eq(peopleRequestId.value) }
            .orderBy(created_at to SortOrder.DESC)
            .map { it }
}