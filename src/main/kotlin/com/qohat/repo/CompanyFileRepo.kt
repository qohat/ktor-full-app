package com.qohat.repo

import arrow.core.Option
import arrow.core.singleOrNone
import com.qohat.domain.*
import com.qohat.entities.CompaniesAttachment
import com.qohat.entities.Lists
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.util.*

interface CompanyFileRepo {
    suspend fun findBy(id: UUID): Option<CompanyAttachment>
    suspend fun findBy(companyId: CompanyId, fileListId: Int): Option<CompanyAttachment>
    suspend fun findAllBy(companyId: CompanyId): List<CompanyAttachment>
    suspend fun findAllBy(companyId: CompanyId, state: AttachmentState): List<CompanyAttachment>
    suspend fun save(companyAttachment: CompanyAttachment): UUID
    suspend fun update(id: UUID, companyAttachment: CompanyAttachment)
    suspend fun delete(id: UUID)
}

class DefaultCompanyFileRepo: CompanyFileRepo {
    override suspend fun findBy(id: UUID): Option<CompanyAttachment> =
        newSuspendedTransaction(Dispatchers.IO) {
            (CompaniesAttachment innerJoin Lists)
                .select {
                    CompaniesAttachment.id.eq(id) and
                    CompaniesAttachment.active.eq(true) and
                    CompaniesAttachment.company_file_id.eq(Lists.id)
                }
                .singleOrNone()
                .map { toCompanyAttachment(it) }
        }

    override suspend fun findBy(companyId: CompanyId, fileListId: Int): Option<CompanyAttachment> =
        newSuspendedTransaction(Dispatchers.IO) {
            CompaniesAttachment
                .join(Lists, JoinType.INNER, CompaniesAttachment.company_file_id, Lists.id)
                .select {
                    CompaniesAttachment.company_id.eq(UUID.fromString(companyId.value)) and
                            CompaniesAttachment.company_file_id.eq(fileListId)
                }
                .singleOrNone()
                .map { toCompanyAttachment(it) }
        }

    override suspend fun findAllBy(companyId: CompanyId): List<CompanyAttachment> =
        newSuspendedTransaction(Dispatchers.IO) {
            (CompaniesAttachment innerJoin Lists)
                .select {
                    CompaniesAttachment.company_id.eq(UUID.fromString(companyId.value)) and
                    CompaniesAttachment.active.eq(true) and
                    CompaniesAttachment.company_file_id.eq(Lists.id)
                }
                .map { toCompanyAttachment(it) }
        }

    override suspend fun findAllBy(companyId: CompanyId, state: AttachmentState): List<CompanyAttachment> =
        newSuspendedTransaction(Dispatchers.IO) {
            (CompaniesAttachment innerJoin Lists)
                .select {
                    CompaniesAttachment.company_id.eq(UUID.fromString(companyId.value)) and
                            CompaniesAttachment.state.eq(state.name) and
                            CompaniesAttachment.company_file_id.eq(Lists.id)
                }
                .map { toCompanyAttachment(it) }
        }

    override suspend fun save(companyAttachment: CompanyAttachment): UUID =
        newSuspendedTransaction(Dispatchers.IO) {
            CompaniesAttachment.insert {
                it[id] = UUID.randomUUID()
                it[company_id] = UUID.fromString(companyAttachment.companyId)
                it[name] = companyAttachment.name
                it[path] = companyAttachment.path
                it[state] = companyAttachment.state.name
                it[active] = true
                it[company_file_id] = companyAttachment.fileType.id
                it[created_at] = LocalDateTime.now()
                it[updated_at] = LocalDateTime.now()
            } get CompaniesAttachment.id
        }

    override suspend fun update(id: UUID, companyAttachment: CompanyAttachment) =
        newSuspendedTransaction(Dispatchers.IO) {
            CompaniesAttachment.update({ CompaniesAttachment.id eq id }) {
                it[name] = companyAttachment.name
                it[state] = companyAttachment.state.toString()
                it[updated_at] = LocalDateTime.now()
            }
            Unit
        }

    override suspend fun delete(id: UUID) =
        newSuspendedTransaction(Dispatchers.IO) {
            CompaniesAttachment.update({ CompaniesAttachment.id eq id }) {
                it[active] = false
                it[updated_at] = LocalDateTime.now()
            }
            Unit
        }

    private fun toCompanyAttachment(row: ResultRow): CompanyAttachment =
        CompanyAttachment(
            id = row[CompaniesAttachment.id].toString(),
            companyId = row[CompaniesAttachment.company_id].toString(),
            name = row[CompaniesAttachment.name],
            path = row[CompaniesAttachment.path],
            state = AttachmentState.valueOf(row[CompaniesAttachment.state]),
            active = row[CompaniesAttachment.active],
            fileType = ValueList(row[Lists.id], row[Lists.name], row[Lists.list], row[Lists.active]),
            createdAt = row[CompaniesAttachment.created_at],
            updatedAt = row[CompaniesAttachment.updated_at],
        )
}