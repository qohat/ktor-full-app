package com.qohat.repo

import com.qohat.domain.*
import com.qohat.entities.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.util.*

interface EventRepo<I, E> {
    suspend fun findAll(id: I): List<E>
    suspend fun save(event: E): UUID
}

class CompaniesValidationEventRepo: EventRepo<CompanyId, ValidationCompaniesAttachmentsEvent> {

    override suspend fun findAll(id: CompanyId): List<ValidationCompaniesAttachmentsEvent> =
        newSuspendedTransaction(Dispatchers.IO) {
            ValidationCompaniesAttachmentsEvents
                .join(CompaniesAttachment, JoinType.INNER, ValidationCompaniesAttachmentsEvents.companies_attachments_id, CompaniesAttachment.id)
                .join(Users, JoinType.INNER, ValidationCompaniesAttachmentsEvents.user_id, Users.id)
                .join(Lists.RejectCompanyDocReason, JoinType.INNER, ValidationCompaniesAttachmentsEvents.reason_id, Lists.RejectCompanyDocReason[Lists.id])
                .join(Lists.CompanyDocType, JoinType.INNER, CompaniesAttachment.company_file_id, Lists.CompanyDocType[Lists.id])
                .select { CompaniesAttachment.company_id.eq(UUID.fromString(id.value)) }
                .orderBy(ValidationCompaniesAttachmentsEvents.created_at to SortOrder.DESC)
                .map { toValidationCompaniesAttachmentsEvent(it) }
        }

    override suspend fun save(event: ValidationCompaniesAttachmentsEvent): UUID =
        newSuspendedTransaction(Dispatchers.IO) {
            ValidationCompaniesAttachmentsEvents.insert {
                it[id] = UUID.randomUUID()
                it[companies_attachments_id] = UUID.fromString(event.companyAttachment.id)
                it[user_id] = UUID.fromString(event.userId)
                it[observation] = event.observation
                it[state] = event.state.toString()
                it[reason_id] = event.reason.id
                it[created_at] = LocalDateTime.now()
            } get ValidationCompaniesAttachmentsEvents.id
        }

    private fun toValidationCompaniesAttachmentsEvent(row: ResultRow): ValidationCompaniesAttachmentsEvent =
        ValidationCompaniesAttachmentsEvent(
            companyAttachment =
            CompanyAttachment(
                id = row[CompaniesAttachment.id].toString(),
                companyId = row[CompaniesAttachment.company_id].toString(),
                name = row[CompaniesAttachment.name],
                path = row[CompaniesAttachment.path],
                state = AttachmentState.valueOf(row[CompaniesAttachment.state]),
                active = row[CompaniesAttachment.active],
                fileType = ValueList(row[Lists.CompanyDocType[Lists.id]], row[Lists.CompanyDocType[Lists.name]], row[Lists.CompanyDocType[Lists.list]], row[Lists.CompanyDocType[Lists.active]]),
                createdAt = row[CompaniesAttachment.created_at],
                updatedAt = row[CompaniesAttachment.updated_at]
            ),
            userId = row[Users.id].toString(),
            userName = "${row[Users.name]} ${row[Users.last_name]}",
            observation = row[ValidationCompaniesAttachmentsEvents.observation],
            state = AttachmentState.valueOf(row[ValidationCompaniesAttachmentsEvents.state]),
            reason = ValueList(row[Lists.RejectCompanyDocReason[Lists.id]], row[Lists.RejectCompanyDocReason[Lists.name]], row[Lists.RejectCompanyDocReason[Lists.list]], row[Lists.RejectCompanyDocReason[Lists.active]]),
            createdAt = row[ValidationCompaniesAttachmentsEvents.created_at],
        )
}

class PeopleValidationEventRepo: EventRepo<PeopleCompanyId, ValidationPeopleAttachmentsEvent> {
    override suspend fun findAll(id: PeopleCompanyId): List<ValidationPeopleAttachmentsEvent> =
        newSuspendedTransaction(Dispatchers.IO) {
            ValidationPeopleAttachmentsEvents
                .join(PeopleAttachments, JoinType.INNER, ValidationPeopleAttachmentsEvents.people_attachments_id, PeopleAttachments.id)
                .join(Users, JoinType.INNER, ValidationPeopleAttachmentsEvents.user_id, Users.id)
                .join(Lists.RejectPeopleDocReason, JoinType.INNER, ValidationPeopleAttachmentsEvents.reason_id, Lists.RejectPeopleDocReason[Lists.id])
                .join(Lists.PeopleDocType, JoinType.INNER, PeopleAttachments.people_file_id, Lists.PeopleDocType[Lists.id])
                .select { PeopleAttachments.people_companies_id.eq(UUID.fromString(id.value)) }
                .orderBy(ValidationPeopleAttachmentsEvents.created_at to SortOrder.DESC)
                .map { toValidationPeopleAttachmentsEvent(it) }
        }

    override suspend fun save(event: ValidationPeopleAttachmentsEvent): UUID =
        newSuspendedTransaction(Dispatchers.IO) {
            ValidationPeopleAttachmentsEvents.insert {
                it[id] = UUID.randomUUID()
                it[people_attachments_id] = UUID.fromString(event.peopleAttachment.id)
                it[user_id] = UUID.fromString(event.userId)
                it[observation] = event.observation
                it[state] = event.state.toString()
                it[reason_id] = event.reason.id
                it[created_at] = LocalDateTime.now()
            } get ValidationPeopleAttachmentsEvents.id
        }

    private fun toValidationPeopleAttachmentsEvent(row: ResultRow): ValidationPeopleAttachmentsEvent =
        ValidationPeopleAttachmentsEvent(
            peopleAttachment =
            PeopleAttachment(
                id = row[PeopleAttachments.id].toString(),
                peopleCompanyId = row[PeopleAttachments.people_companies_id].toString(),
                name = row[PeopleAttachments.name],
                path = row[PeopleAttachments.path],
                state = AttachmentState.valueOf(row[PeopleAttachments.state]),
                active = row[PeopleAttachments.active],
                fileType = ValueList(row[Lists.PeopleDocType[Lists.id]], row[Lists.PeopleDocType[Lists.name]], row[Lists.PeopleDocType[Lists.list]], row[Lists.PeopleDocType[Lists.active]]),
                createdAt = row[PeopleAttachments.created_at],
                updatedAt = row[PeopleAttachments.updated_at]
            ),
            userId = row[Users.id].toString(),
            userName = "${row[Users.name]} ${row[Users.last_name]}",
            observation = row[ValidationPeopleAttachmentsEvents.observation],
            state = AttachmentState.valueOf(row[ValidationPeopleAttachmentsEvents.state]),
            reason = ValueList(row[Lists.RejectPeopleDocReason[Lists.id]], row[Lists.RejectPeopleDocReason[Lists.name]], row[Lists.RejectPeopleDocReason[Lists.list]], row[Lists.RejectPeopleDocReason[Lists.active]]),
            createdAt = row[ValidationPeopleAttachmentsEvents.created_at],
        )
}