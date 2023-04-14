package com.qohat.repo

import arrow.core.Option
import arrow.core.singleOrNone
import com.qohat.domain.*
import com.qohat.entities.Assignments
import com.qohat.entities.Lists
import com.qohat.entities.PeopleAttachments
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.util.*

interface PeopleFileRepo {
    suspend fun findBy(id: UUID): Option<PeopleAttachment>
    suspend fun findBy(peopleCompanyId: PeopleCompanyId, fileListId: Int): Option<PeopleAttachment>
    suspend fun findAllBy(name: String): List<PeopleAttachment>
    suspend fun findAllBy(peopleCompanyId: PeopleCompanyId): List<PeopleAttachment>
    suspend fun findAllBy(peopleCompanyId: PeopleCompanyId, state: AttachmentState): List<PeopleAttachment>
    suspend fun save(peopleAttachment: PeopleAttachment): UUID
    suspend fun update(id: UUID, peopleAttachment: PeopleAttachment)
    suspend fun delete(id: UUID)
}

class DefaultPeopleFileRepo: PeopleFileRepo {
    override suspend fun findBy(id: UUID): Option<PeopleAttachment> =
        newSuspendedTransaction(Dispatchers.IO) {
            (PeopleAttachments innerJoin Lists)
                .select {
                    PeopleAttachments.id.eq(id) and
                    PeopleAttachments.active.eq(true) and
                    PeopleAttachments.people_file_id.eq(Lists.id)
                }
                .singleOrNone()
                .map { toPeopleAttachment(it) }
        }

    override suspend fun findBy(peopleCompanyId: PeopleCompanyId, fileListId: Int): Option<PeopleAttachment> =
        newSuspendedTransaction(Dispatchers.IO) {
            PeopleAttachments
                .join(Lists, JoinType.INNER, PeopleAttachments.people_file_id, Lists.id)
                .select {
                    PeopleAttachments.people_companies_id.eq(UUID.fromString(peopleCompanyId.value)) and
                            PeopleAttachments.people_file_id.eq(fileListId)
                }
                .singleOrNone()
                .map { toPeopleAttachment(it) }
        }

    override suspend fun findAllBy(name: String): List<PeopleAttachment> =
        newSuspendedTransaction(Dispatchers.IO) {
            (PeopleAttachments innerJoin Lists)
                .select {
                            PeopleAttachments.active.eq(true) and
                            PeopleAttachments.people_file_id.eq(Lists.id) and
                            PeopleAttachments.name.eq(name)
                }
                .orderBy(PeopleAttachments.created_at to SortOrder.DESC)
                .map { toPeopleAttachment(it) }
        }

    override suspend fun findAllBy(peopleCompanyId: PeopleCompanyId): List<PeopleAttachment> =
        newSuspendedTransaction(Dispatchers.IO) {
            (PeopleAttachments innerJoin Lists)
                .select {
                    PeopleAttachments.people_companies_id.eq(UUID.fromString(peopleCompanyId.value)) and
                    PeopleAttachments.active.eq(true) and
                    PeopleAttachments.people_file_id.eq(Lists.id)
                }
                .map { toPeopleAttachment(it) }
        }

    override suspend fun findAllBy(peopleCompanyId: PeopleCompanyId, state: AttachmentState): List<PeopleAttachment> =
        newSuspendedTransaction(Dispatchers.IO) {
            (PeopleAttachments innerJoin Lists)
                .select {
                    PeopleAttachments.people_companies_id.eq(UUID.fromString(peopleCompanyId.value)) and
                            PeopleAttachments.state.eq(state.name) and
                            PeopleAttachments.people_file_id.eq(Lists.id)
                }
                .map { toPeopleAttachment(it) }
        }

    override suspend fun save(peopleAttachment: PeopleAttachment): UUID =
        newSuspendedTransaction(Dispatchers.IO) {
            PeopleAttachments.insert {
                it[id] = UUID.randomUUID()
                it[people_companies_id] = UUID.fromString(peopleAttachment.peopleCompanyId)
                it[name] = peopleAttachment.name
                it[path] = peopleAttachment.path
                it[state] = peopleAttachment.state.name
                it[active] = true
                it[people_file_id] = peopleAttachment.fileType.id
                it[created_at] = LocalDateTime.now()
                it[updated_at] = LocalDateTime.now()
            } get PeopleAttachments.id
        }

    override suspend fun update(id: UUID, peopleAttachment: PeopleAttachment) =
        newSuspendedTransaction(Dispatchers.IO) {
            PeopleAttachments.update({ PeopleAttachments.id eq id }) {
                //Should Path and Name not update?
                it[name] = peopleAttachment.name
                it[state] = peopleAttachment.state.toString()
                it[updated_at] = LocalDateTime.now()
            }
            Unit
        }

    override suspend fun delete(id: UUID) =
        newSuspendedTransaction(Dispatchers.IO) {
            PeopleAttachments.update({ PeopleAttachments.id eq id }) {
                it[active] = false
                it[updated_at] = LocalDateTime.now()
            }
            Unit
        }

    private fun toPeopleAttachment(row: ResultRow): PeopleAttachment =
        PeopleAttachment(
            id = row[PeopleAttachments.id].toString(),
            peopleCompanyId = row[PeopleAttachments.people_companies_id].toString(),
            name = row[PeopleAttachments.name],
            path = row[PeopleAttachments.path],
            state = AttachmentState.valueOf(row[PeopleAttachments.state]),
            active = row[PeopleAttachments.active],
            fileType = ValueList(row[Lists.id], row[Lists.name], row[Lists.list], row[Lists.active]),
            createdAt = row[PeopleAttachments.created_at],
            updatedAt = row[PeopleAttachments.updated_at],
        )
}