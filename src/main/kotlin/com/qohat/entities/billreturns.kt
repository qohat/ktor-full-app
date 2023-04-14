package com.qohat.entities

import com.qohat.domain.AttachmentId
import com.qohat.domain.AttachmentState
import com.qohat.domain.AttachmentsValidationEvent
import com.qohat.domain.NewAttachment
import com.qohat.domain.NewPayment
import com.qohat.domain.NotPaidReport
import com.qohat.domain.PeopleRequestId
import com.qohat.domain.SupplyDetail
import com.qohat.domain.requests.BillReturn
import com.qohat.domain.requests.BillReturnId
import com.qohat.domain.requests.BillReturnObservation
import com.qohat.domain.requests.UpdateBillReturn
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Concat
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.BatchUpdateStatement
import org.jetbrains.exposed.sql.stringLiteral
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.UUID

object BillReturns: Table("bill_returns") {
    val id = uuid("id")
    val people_request_id = uuid("people_request_id").references(PeopleRequests.id)
    val product_id = (long("product_id") references Products.id)
    val storage_id = (long("storage_id") references Storages.id)

    override val primaryKey = PrimaryKey(id, people_request_id, product_id)

    private val productsQuery = join(Products, JoinType.INNER, product_id, Products.id)
        .join(Lists.CropGroup, JoinType.INNER, Products.crop_group_id, Lists.CropGroup[Lists.id])
        .join(Lists.Chain, JoinType.INNER, Lists.CropGroup[Lists.list], Concat("_", stringLiteral("CROP_GROUP"), Lists.Chain[Lists.name]))
        .slice(
            Products.id,
            Products.name,
            Lists.Chain[Lists.name],
            Lists.CropGroup[Lists.name],
            Products.percentage,
            Products.maximum_to_subsidize,
            Products.minimum_to_apply,
            Products.active,
            Products.created_at
        )

    fun insertQuery(billReturn: BillReturn): UUID =
        insert {
            it[id] = UUID.randomUUID()
            it[people_request_id] = billReturn.peopleRequestId.value
            it[product_id] = billReturn.product.value
            it[storage_id] = billReturn.storage.value
        } get id

    fun updateQuery(billReturnId: BillReturnId, billReturn: UpdateBillReturn): Int =
        update({ id.eq(billReturnId.value)}) {
            it[product_id] = billReturn.product.value
            it[storage_id] = billReturn.storage.value
        }

    fun selectPeopleRequestIdQuery(billReturnId: BillReturnId): PeopleRequestId? =
        slice(people_request_id)
            .select { id.eq(billReturnId.value) }
            .singleOrNull()?.let {
                PeopleRequestId(it[people_request_id])
            }

    fun selectProduct(billReturnId: BillReturnId): ResultRow? =
        productsQuery
            .select { id.eq(billReturnId.value) }
            .singleOrNull()
}

object BillReturnsSupplies: Table("bill_returns_supplies") {
    val bill_return_id = (uuid("bill_return_id") references BillReturns.id)
    val supply_id = (long("supply_id") references Supplies.id)
    val quantity = long("quantity")
    val bought_date = date("bought_date").nullable()
    val presentation = (integer("presentation") references Lists.id).nullable()
    val unit = (integer("unit") references Lists.id)
    val value = decimal("value", 2, 2)

    override val primaryKey = PrimaryKey(bill_return_id, supply_id)

    fun insertBatchQuery(id: BillReturnId, supplyDetails: List<SupplyDetail>) =
        batchInsert(supplyDetails, ignore = false, shouldReturnGeneratedValues = false) {
            this[bill_return_id] = id.value
            this[supply_id] = it.supply.value
            this[quantity] = it.quantity.value
            this[bought_date] = it.boughtDate?.value
            this[presentation] = it.presentation?.id
            this[unit] = it.measurementUnit.id
            this[value] = it.value.value
        }

    private val query =
            join(Supplies, JoinType.INNER, supply_id, Supplies.id)
            .join(Lists.MeasurementUnit, JoinType.INNER, unit, Lists.MeasurementUnit[Lists.id])
            .join(Lists.Presentation, JoinType.LEFT, presentation, Lists.Presentation[Lists.id])
            .slice(
                Supplies.id,
                Supplies.name,
                Supplies.price,
                value,
                Lists.MeasurementUnit[Lists.name],
                quantity,
                bought_date,
                Lists.Presentation[Lists.name]
            )
    fun selectQuery(billReturnId: BillReturnId): List<ResultRow> =
        query
            .select { bill_return_id.eq(billReturnId.value) }
            .map { it }

    fun deleteQuery(billReturnId: BillReturnId) =
        deleteWhere { bill_return_id.eq(billReturnId.value) }
}

object BillReturnsAttachments: IdTable<UUID>("bill_returns_attachments") {
    override val id: Column<EntityID<UUID>> = uuid("id").autoGenerate().entityId()
    val bill_return_id = (uuid("bill_return_id") references BillReturns.id)
    val file_id = (integer("file_id") references Lists.Files[Lists.id])
    val name = varchar("name", 255)
    val path = varchar("path", 255)
    val state = customEnumeration("state", "AttachmentState", {value -> AttachmentState.valueOf(value as String)}, { PGEnum("AttachmentState", it) })
    val created_at = datetime("created_at")

    override val primaryKey = PrimaryKey(
        id,
        bill_return_id,
        file_id
    )

    private val query = join(Lists.Files, JoinType.INNER, file_id, Lists.Files[Lists.id])
        .slice(
            id,
            name,
            path,
            state,
            Lists.Files[Lists.name]
        )

    fun selectQuery(billReturnId: BillReturnId): List<ResultRow> =
        query
        .select { bill_return_id.eq(billReturnId.value) }
        .map { it }

    fun insertQuery(billReturnId: BillReturnId, attachment: NewAttachment): UUID =
        (insertWithRawStatement({
            it[bill_return_id] = billReturnId.value
            it[file_id] = attachment.fileTypeId.id
            it[name] = attachment.name.value
            it[path] = attachment.path.value
            it[state] = attachment.state
            it[created_at] = LocalDateTime.now()
        }) { "ON CONFLICT (bill_return_id, file_id) DO UPDATE SET state = '${attachment.state.name}', name = '${attachment.name.value}'" } get id).value

    fun updateStateQuery(billReturnId: BillReturnId, newState: AttachmentState, attachment: AttachmentId): Int =
        update({ bill_return_id.eq(billReturnId.value) and id.eq(attachment.value) }) {
            it[state] = newState
        }

    fun updateStateBatchQuery(billReturnId: BillReturnId, newState: AttachmentState, attachments: List<AttachmentId>): Unit =
        BatchUpdateStatement(BillReturnsAttachments).run {
            attachments.forEach {
                addBatch(EntityID(it.value, BillReturnsAttachments))
                update({ bill_return_id.eq(billReturnId.value) and id.eq(it.value) }) {
                    it[state] = newState
                }
            }
        }
}

object BillReturnsValidationAttachmentsEvents: Table("bill_returns_validation_attachments_events") {
    val id = long("id").autoIncrement()
    val bill_returns_attachment_id = (uuid("bill_returns_attachment_id").references(BillReturnsAttachments.id))
    val observation = text("observation").nullable()
    val reason_id = (integer("reason_id") references Lists.id)
    val user_id = (uuid("user_id") references Users.id)
    val state = customEnumeration(
        "state",
        "AttachmentState",
        { value -> AttachmentState.valueOf(value as String) },
        { PGEnum("AttachmentState", it) })
    val created_at = datetime("created_at")

    override val primaryKey = PrimaryKey(id)

    private val query = join(Lists.Reason, JoinType.INNER, reason_id, Lists.Reason[Lists.id])
        .join(Users, JoinType.INNER, user_id, Users.id)
        .join(BillReturnsAttachments, JoinType.INNER, bill_returns_attachment_id, BillReturnsAttachments.id)
        .join(BillReturns, JoinType.INNER, BillReturnsAttachments.bill_return_id, BillReturns.id)
        .join(Lists.Files, JoinType.INNER, BillReturnsAttachments.file_id, Lists.Files[Lists.id])
        .slice(
            observation,
            Lists.Reason[Lists.name],
            Users.name,
            state,
            created_at,
            Lists.Files[Lists.name]
        )

    fun selectQuery(attachmentId: AttachmentId): List<ResultRow> =
        query.select { bill_returns_attachment_id.eq(attachmentId.value) }.map { it }
    fun selectQueryBy(billReturnId: BillReturnId): List<ResultRow> =
        query.select { BillReturns.id.eq(billReturnId.value) }
            .orderBy(created_at to SortOrder.DESC)
            .map { it }
    fun insertQuery(event: AttachmentsValidationEvent): Long =
        insert {
            it[bill_returns_attachment_id] = event.attachmentId.value
            it[observation] = event.observation?.value
            it[reason_id] = event.reason.id
            it[user_id] = event.userId.value
            it[state] = event.state
            it[created_at] = LocalDateTime.now()
        } get id

    fun insertBatch(events: List<AttachmentsValidationEvent>) =
        batchInsert(events, shouldReturnGeneratedValues = false) {
            this[bill_returns_attachment_id] = it.attachmentId.value
            this[observation] = it.observation?.value
            this[reason_id] = it.reason.id
            this[user_id] = it.userId.value
            this[state] = it.state
            this[created_at] = LocalDateTime.now()
        }
}

object BillReturnPayments: Table("bill_return_payments") {
    val bill_return_id = uuid("bill_return_id") references BillReturns.id
    val payment_date = date("payment_date")
    val created_at = datetime("created_at")
    val user_id = uuid("user_id") references Users.id

    override val primaryKey = PrimaryKey(bill_return_id)

    fun insertQuery(payment: NewPayment) =
        insert {
            it[bill_return_id] = payment.billReturnId.value
            it[payment_date] = payment.date.value
            it[user_id] = payment.userId.value
            it[created_at] = LocalDateTime.now()
        }

    fun selectQuery(billReturnId: BillReturnId): ResultRow? =
        select { bill_return_id.eq(billReturnId.value) }.singleOrNull()
}

object BillReturnObservations: LongIdTable("bill_returns_observations") {
    val bill_return_id = uuid("bill_return_id") references BillReturns.id
    val observation = text("observation")
    val user_id = uuid("user_id") references Users.id
    val created_at = datetime("created_at")

    val query = join(Users, JoinType.INNER, user_id, Users.id)
        .slice(
            id,
            bill_return_id,
            observation,
            Users.id,
            Users.name,
            Users.last_name,
            created_at
        )

    fun insertQuery(billReturnObservation: BillReturnObservation): Long =
        (insert {
            it[bill_return_id] = billReturnObservation.billReturnId.value
            it[observation] = billReturnObservation.observation.value
            it[user_id] = billReturnObservation.userId.value
            it[created_at] = LocalDateTime.now()
        } get id).value

    fun selectBy(billReturnId: BillReturnId): List<ResultRow> =
        query.select { bill_return_id.eq(billReturnId.value) }
            .orderBy(created_at to SortOrder.DESC)
            .map { it }
}

object BillReturnNotPaidReport: Table("bill_return_not_paid_report") {
    val bill_return_id = uuid("bill_return_id") references BillReturns.id
    val created_at = datetime("created_at")
    val user_id = uuid("user_id") references Users.id
    val reason_id = (integer("reason_id") references Lists.id)

    override val primaryKey = PrimaryKey(bill_return_id)

    fun insertQuery(report: NotPaidReport) =
        insert {
            it[bill_return_id] = report.billReturnId.value
            it[reason_id] = report.reasonId.value
            it[user_id] = report.userId.value
            it[created_at] = LocalDateTime.now()
        }
}
