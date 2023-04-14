package com.qohat.repo

import arrow.core.Option
import arrow.core.singleOrNone
import arrow.core.toOption
import com.qohat.domain.*
import com.qohat.entities.*
import com.qohat.entities.People
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

interface PeopleCompanyRepo {
    suspend fun findBy(id: UUID): Option<PeopleCompany>
    suspend fun findBy(id: PeopleCompanyValidationId): Option<PeopleCompanyValidation>
    suspend fun findBy(paymentImportRecord: PaymentImportRecord): Option<PeopleCompanyForPayment>
    suspend fun findAllLite(state: AttachmentState, params: PaginationParams): List<PeopleCompanyLite>
    suspend fun findAllLite(state: AttachmentState, userId: UserId, params: PaginationParams): List<PeopleCompanyLite>
    suspend fun findAllLite(companyId: CompanyId): List<PeopleCompanyLite>
    suspend fun findAllArchived(userId: UserId): List<PeopleCompanyLite>
    suspend fun findAllBy(peopleId: PeopleId): List<PeopleCompany>
    suspend fun findAll(): List<PeopleCompany>
    suspend fun findAllValidationBy(state: AttachmentState, userId: UserId): List<PeopleCompanyValidation>
    suspend fun findAllValidationBy(state: AttachmentState): List<PeopleCompanyValidation>
    suspend fun findAllValidationAtLeastOnePaid(): List<PeopleCompanyValidation>
    suspend fun save(peopleCompany: PeopleCompany): UUID
    suspend fun update(id: UUID, peopleCompany: PeopleCompany)
    suspend fun update(id: UUID, newState: AttachmentState)
    suspend fun delete(id: UUID)
}

class DefaultPeopleCompanyRepo(val db: Database): PeopleCompanyRepo {
    override suspend fun findBy(id: UUID): Option<PeopleCompany> =
        newSuspendedTransaction(Dispatchers.IO) {
            PeopleCompanies
                .join(Lists.ContractType, JoinType.INNER, PeopleCompanies.contract_type_id, Lists.ContractType[Lists.id])
                .join(Lists.MothRequestApplied, JoinType.INNER, PeopleCompanies.month_request_applied_id, Lists.MothRequestApplied[Lists.id])
                .join(Lists.CurrentMonthApplied, JoinType.INNER, PeopleCompanies.current_month_applied, Lists.CurrentMonthApplied[Lists.id])
                .join(Lists.ArlLevel, JoinType.INNER, PeopleCompanies.arl_level_id, Lists.ArlLevel[Lists.id])
                .join(Companies, JoinType.INNER, PeopleCompanies.company_id, Companies.id)
                .select { PeopleCompanies.id.eq(id) and PeopleCompanies.active.eq(true) }
                .singleOrNone()
                .map { toPeopleCompany(it) }
        }

    override suspend fun findBy(id: PeopleCompanyValidationId): Option<PeopleCompanyValidation> =
        newSuspendedTransaction(Dispatchers.IO) {
            val companyLocality = Lists.alias("CompanyLocality")
            PeopleCompanies
                .join(Lists.ContractType, JoinType.INNER, PeopleCompanies.contract_type_id, Lists.ContractType[Lists.id])
                .join(Lists.MothRequestApplied, JoinType.INNER, PeopleCompanies.month_request_applied_id, Lists.MothRequestApplied[Lists.id])
                .join(Lists.CurrentMonthApplied, JoinType.INNER, PeopleCompanies.current_month_applied, Lists.CurrentMonthApplied[Lists.id])
                .join(Lists.ArlLevel, JoinType.INNER, PeopleCompanies.arl_level_id, Lists.ArlLevel[Lists.id])
                .join(Companies, JoinType.INNER, PeopleCompanies.company_id, Companies.id)
                .join(People, JoinType.INNER, PeopleCompanies.people_id, People.id)
                .join(Lists.Locality, JoinType.INNER, People.locality_id, Lists.Locality[Lists.id])
                .join(Lists.DocumentType, JoinType.INNER, People.document_type, Lists.DocumentType[Lists.id])
                .join(Lists.Gender, JoinType.INNER, People.gender_id, Lists.Gender[Lists.id])
                .join(Lists.PopulationGroup, JoinType.INNER, People.population_group_id, Lists.PopulationGroup[Lists.id])
                .join(Lists.EthnicGroup, JoinType.INNER, People.ethnic_group_id, Lists.EthnicGroup[Lists.id])
                .join(Lists.Disability, JoinType.INNER, People.disability_id, Lists.Disability[Lists.id])
                .join(Lists.CompanyType, JoinType.INNER, Companies.company_type_id, Lists.CompanyType[Lists.id])
                .join(Lists.CompanySize, JoinType.INNER, Companies.company_size_id, Lists.CompanySize[Lists.id])
                .join(Lists.Ccf, JoinType.INNER, Companies.ccf_id, Lists.Ccf[Lists.id])
                .join(Lists.Bank, JoinType.INNER, Companies.bank_id, Lists.Bank[Lists.id])
                .join(Lists.AccountBankType, JoinType.INNER, Companies.account_bank_type_id, Lists.AccountBankType[Lists.id])
                .join(companyLocality, JoinType.INNER, Companies.locality_id, companyLocality[Lists.id])
                .select { PeopleCompanies.id.eq(UUID.fromString(id.value)) }
                .singleOrNone()
                .map { toPeopleCompanyValidation(it) }
        }

    override suspend fun findBy(paymentImportRecord: PaymentImportRecord): Option<PeopleCompanyForPayment> =
        newSuspendedTransaction(Dispatchers.IO) {
            PeopleCompanies
                .join(Lists.CurrentMonthApplied, JoinType.INNER, PeopleCompanies.current_month_applied, Lists.CurrentMonthApplied[Lists.id])
                .join(Lists.ArlLevel, JoinType.INNER, PeopleCompanies.arl_level_id, Lists.ArlLevel[Lists.id])
                .join(Companies, JoinType.INNER, PeopleCompanies.company_id, Companies.id)
                .join(People, JoinType.INNER, PeopleCompanies.people_id, People.id)
                .slice(PeopleCompanies.id,
                    Lists.CurrentMonthApplied[Lists.id],
                    Lists.CurrentMonthApplied[Lists.name],
                    Lists.CurrentMonthApplied[Lists.list],
                    Lists.CurrentMonthApplied[Lists.active],
                    Lists.ArlLevel[Lists.id],
                    Lists.ArlLevel[Lists.name],
                    Lists.ArlLevel[Lists.list],
                    Lists.ArlLevel[Lists.active]
                )
                .select {
                    People.document.eq(paymentImportRecord.peopleDoc) and
                            Companies.document.eq(paymentImportRecord.companyDoc) and
                            Lists.CurrentMonthApplied[Lists.name].eq("${paymentImportRecord.month.value}-${paymentImportRecord.year.value.toString().replace("20", "")}") and
                            PeopleCompanies.state.eq(AttachmentState.NonPaid.name) and
                            PeopleCompanies.active.eq(true)
                }
                .singleOrNone()
                .map { toPeopleCompanyForPayment(it) }
        }

    override suspend fun findAllLite(state: AttachmentState, params: PaginationParams): List<PeopleCompanyLite> =
        newSuspendedTransaction(Dispatchers.IO) {
            val query = PeopleCompanies
                .join(Companies, JoinType.INNER, PeopleCompanies.company_id, Companies.id)
                .join(People, JoinType.INNER, PeopleCompanies.people_id, People.id)
                .join(PeopleAttachments.PeopleAttachmentCount, JoinType.INNER, PeopleCompanies.id, PeopleAttachments.PeopleAttachmentCount[PeopleAttachments.people_companies_id])
                .join(CompaniesAttachment.CompanyAttachmentCount, JoinType.INNER, Companies.id, CompaniesAttachment.CompanyAttachmentCount[CompaniesAttachment.company_id])
                .slice(PeopleCompanies.id,
                    PeopleCompanies.state,
                    PeopleCompanies.created_at,
                    Companies.name,
                    Companies.document,
                    People.name,
                    People.last_name,
                    People.document,
                    PeopleAttachments.PeopleAttachmentCount[PeopleAttachments.PeopleAttachmentCountVal],
                    CompaniesAttachment.CompanyAttachmentCount[CompaniesAttachment.CompanyAttachmentCountVal],
                    PeopleCompanies.active)

            val select = if (params.text.isNotBlank() or params.text.isNotEmpty()) {
                    query.select {
                        concat(separator = " ", listOf(People.name.lowerCase(), People.last_name.lowerCase())) like "%${params.text.lowercase()}%"
                    }.orWhere {
                        People.document like "%${params.text}%"
                    }.orWhere {
                        Companies.name.lowerCase() like "%${params.text.lowercase()}%"
                    }.orWhere {
                        Companies.document like "%${params.text}%"
                    }.andWhere {
                        PeopleCompanies.state.eq(state.name) and PeopleCompanies.active.eq(true)
                    }
                } else {
                    query.select {
                        PeopleCompanies.state.eq(state.name) and PeopleCompanies.active.eq(true) }
                }

            select
            .limit(params.limit, params.offset)
            .map { toPeopleCompanyLite(it) }
        }

    override suspend fun findAllLite(state: AttachmentState, userId: UserId, params: PaginationParams): List<PeopleCompanyLite> =
        newSuspendedTransaction(Dispatchers.IO) {
            val query = PeopleCompanies
                .join(Companies, JoinType.INNER, PeopleCompanies.company_id, Companies.id)
                .join(People, JoinType.INNER, PeopleCompanies.people_id, People.id)
                .join(PeopleAttachments.PeopleAttachmentCount, JoinType.INNER, PeopleCompanies.id, PeopleAttachments.PeopleAttachmentCount[PeopleAttachments.people_companies_id])
                .join(CompaniesAttachment.CompanyAttachmentCount, JoinType.INNER, Companies.id, CompaniesAttachment.CompanyAttachmentCount[CompaniesAttachment.company_id])
                .slice(PeopleCompanies.id,
                    PeopleCompanies.state,
                    PeopleCompanies.created_at,
                    Companies.name,
                    Companies.document,
                    People.name,
                    People.last_name,
                    People.document,
                    PeopleAttachments.PeopleAttachmentCount[PeopleAttachments.PeopleAttachmentCountVal],
                    CompaniesAttachment.CompanyAttachmentCount[CompaniesAttachment.CompanyAttachmentCountVal],
                    PeopleCompanies.active)

            val select = if (params.text.isNotBlank() or params.text.isNotEmpty()) {
                query.select {
                    concat(separator = " ", listOf(People.name.lowerCase(), People.last_name.lowerCase())) like "%${params.text.lowercase()}%"
                }.orWhere {
                    People.document like "%${params.text}%"
                }.orWhere {
                    Companies.name.lowerCase() like "%${params.text.lowercase()}%"
                }.orWhere {
                    Companies.document like "%${params.text}%"
                }.andWhere {
                    PeopleCompanies.state.eq(state.name) and PeopleCompanies.active.eq(true)
                }
            } else {
                query.select {
                    PeopleCompanies.state.eq(state.name) and
                            PeopleCompanies.active.eq(true) }
            }

            select
            .andWhere {
                    (PeopleCompanies.created_by_id.eq(userId.value) or
                            PeopleCompanies.assigned_to_id.eq(userId.value)) }
            .limit(params.limit, params.offset)
            .map { toPeopleCompanyLite(it) }
        }

    override suspend fun findAllLite(companyId: CompanyId): List<PeopleCompanyLite> =
        newSuspendedTransaction(Dispatchers.IO) {
            PeopleCompanies
                .join(Companies, JoinType.INNER, PeopleCompanies.company_id, Companies.id)
                .join(People, JoinType.INNER, PeopleCompanies.people_id, People.id)
                .join(PeopleAttachments.PeopleAttachmentCount, JoinType.INNER, PeopleCompanies.id, PeopleAttachments.PeopleAttachmentCount[PeopleAttachments.people_companies_id])
                .join(CompaniesAttachment.CompanyAttachmentCount, JoinType.INNER, Companies.id, CompaniesAttachment.CompanyAttachmentCount[CompaniesAttachment.company_id])
                .slice(PeopleCompanies.id,
                    PeopleCompanies.state,
                    PeopleCompanies.created_at,
                    Companies.name,
                    Companies.document,
                    People.name,
                    People.last_name,
                    People.document,
                    PeopleAttachments.PeopleAttachmentCount[PeopleAttachments.PeopleAttachmentCountVal],
                    CompaniesAttachment.CompanyAttachmentCount[CompaniesAttachment.CompanyAttachmentCountVal],
                    PeopleCompanies.active)
                .select {
                    PeopleCompanies.company_id.eq(UUID.fromString(companyId.value)) and
                            PeopleCompanies.active.eq(true)
                }
                .map { toPeopleCompanyLite(it) }
        }

    override suspend fun findAllArchived(userId: UserId): List<PeopleCompanyLite> =
        newSuspendedTransaction(Dispatchers.IO) {
            PeopleCompanies
                .join(Companies, JoinType.INNER, PeopleCompanies.company_id, Companies.id)
                .join(People, JoinType.INNER, PeopleCompanies.people_id, People.id)
                .join(PeopleAttachments.PeopleAttachmentCount, JoinType.INNER, PeopleCompanies.id, PeopleAttachments.PeopleAttachmentCount[PeopleAttachments.people_companies_id])
                .join(CompaniesAttachment.CompanyAttachmentCount, JoinType.INNER, Companies.id, CompaniesAttachment.CompanyAttachmentCount[CompaniesAttachment.company_id])
                .slice(PeopleCompanies.id,
                    PeopleCompanies.state,
                    PeopleCompanies.created_at,
                    Companies.name,
                    Companies.document,
                    People.name,
                    People.last_name,
                    People.document,
                    PeopleAttachments.PeopleAttachmentCount[PeopleAttachments.PeopleAttachmentCountVal],
                    CompaniesAttachment.CompanyAttachmentCount[CompaniesAttachment.CompanyAttachmentCountVal],
                    PeopleCompanies.active)
                .select {PeopleCompanies.active.eq(false) and (PeopleCompanies.created_by_id.eq(userId.value) or
                        PeopleCompanies.assigned_to_id.eq(userId.value))}
                .map { toPeopleCompanyLite(it) }
        }

    override suspend fun findAllBy(peopleId: PeopleId): List<PeopleCompany> =
        newSuspendedTransaction(Dispatchers.IO) {
            PeopleCompanies
                .join(Lists.ContractType, JoinType.INNER, PeopleCompanies.contract_type_id, Lists.ContractType[Lists.id])
                .join(Lists.MothRequestApplied, JoinType.INNER, PeopleCompanies.month_request_applied_id, Lists.MothRequestApplied[Lists.id])
                .join(Lists.CurrentMonthApplied, JoinType.INNER, PeopleCompanies.current_month_applied, Lists.CurrentMonthApplied[Lists.id])
                .join(Lists.ArlLevel, JoinType.INNER, PeopleCompanies.arl_level_id, Lists.ArlLevel[Lists.id])
                .join(Companies, JoinType.INNER, PeopleCompanies.company_id, Companies.id)
                .select { PeopleCompanies.people_id.eq(UUID.fromString(peopleId.value)) and
                        PeopleCompanies.active.eq(true)}
                .map { toPeopleCompany(it) }
        }

    override suspend fun findAll(): List<PeopleCompany> =
        newSuspendedTransaction(Dispatchers.IO) {
            PeopleCompanies
                .join(Lists.ContractType, JoinType.INNER, PeopleCompanies.contract_type_id, Lists.ContractType[Lists.id])
                .join(Lists.MothRequestApplied, JoinType.INNER, PeopleCompanies.month_request_applied_id, Lists.MothRequestApplied[Lists.id])
                .join(Lists.CurrentMonthApplied, JoinType.INNER, PeopleCompanies.current_month_applied, Lists.CurrentMonthApplied[Lists.id])
                .join(Lists.ArlLevel, JoinType.INNER, PeopleCompanies.arl_level_id, Lists.ArlLevel[Lists.id])
                .join(Companies, JoinType.INNER, PeopleCompanies.company_id, Companies.id)
                .select { PeopleCompanies.active.eq(true) }
                .map { toPeopleCompany(it) }
        }

    override suspend fun findAllValidationBy(state: AttachmentState, userId: UserId): List<PeopleCompanyValidation> =
        newSuspendedTransaction(Dispatchers.IO) {
            val companyLocality = Lists.alias("CompanyLocality")
            PeopleCompanies
                .join(Lists.ContractType, JoinType.INNER, PeopleCompanies.contract_type_id, Lists.ContractType[Lists.id])
                .join(Lists.MothRequestApplied, JoinType.INNER, PeopleCompanies.month_request_applied_id, Lists.MothRequestApplied[Lists.id])
                .join(Lists.CurrentMonthApplied, JoinType.INNER, PeopleCompanies.current_month_applied, Lists.CurrentMonthApplied[Lists.id])
                .join(Lists.ArlLevel, JoinType.INNER, PeopleCompanies.arl_level_id, Lists.ArlLevel[Lists.id])
                .join(Companies, JoinType.INNER, PeopleCompanies.company_id, Companies.id)
                .join(People, JoinType.INNER, PeopleCompanies.people_id, People.id)
                .join(Lists.Locality, JoinType.INNER, People.locality_id, Lists.Locality[Lists.id])
                .join(Lists.DocumentType, JoinType.INNER, People.document_type, Lists.DocumentType[Lists.id])
                .join(Lists.Gender, JoinType.INNER, People.gender_id, Lists.Gender[Lists.id])
                .join(Lists.PopulationGroup, JoinType.INNER, People.population_group_id, Lists.PopulationGroup[Lists.id])
                .join(Lists.EthnicGroup, JoinType.INNER, People.ethnic_group_id, Lists.EthnicGroup[Lists.id])
                .join(Lists.Disability, JoinType.INNER, People.disability_id, Lists.Disability[Lists.id])
                .join(Lists.CompanyType, JoinType.INNER, Companies.company_type_id, Lists.CompanyType[Lists.id])
                .join(Lists.CompanySize, JoinType.INNER, Companies.company_size_id, Lists.CompanySize[Lists.id])
                .join(Lists.Ccf, JoinType.INNER, Companies.ccf_id, Lists.Ccf[Lists.id])
                .join(Lists.Bank, JoinType.INNER, Companies.bank_id, Lists.Bank[Lists.id])
                .join(Lists.AccountBankType, JoinType.INNER, Companies.account_bank_type_id, Lists.AccountBankType[Lists.id])
                .join(companyLocality, JoinType.INNER, Companies.locality_id, companyLocality[Lists.id])
                .select { PeopleCompanies.state.eq(state.name) and
                        (PeopleCompanies.created_by_id.eq(userId.value) or
                                PeopleCompanies.assigned_to_id.eq(userId.value)) and
                        PeopleCompanies.active.eq(true) }
                .map { toPeopleCompanyValidation(it) }
        }

    override suspend fun findAllValidationBy(state: AttachmentState): List<PeopleCompanyValidation> =
        newSuspendedTransaction(Dispatchers.IO) {
            val companyLocality = Lists.alias("CompanyLocality")
            PeopleCompanies
                .join(Lists.ContractType, JoinType.INNER, PeopleCompanies.contract_type_id, Lists.ContractType[Lists.id])
                .join(Lists.MothRequestApplied, JoinType.INNER, PeopleCompanies.month_request_applied_id, Lists.MothRequestApplied[Lists.id])
                .join(Lists.CurrentMonthApplied, JoinType.INNER, PeopleCompanies.current_month_applied, Lists.CurrentMonthApplied[Lists.id])
                .join(Lists.ArlLevel, JoinType.INNER, PeopleCompanies.arl_level_id, Lists.ArlLevel[Lists.id])
                .join(Companies, JoinType.INNER, PeopleCompanies.company_id, Companies.id)
                .join(People, JoinType.INNER, PeopleCompanies.people_id, People.id)
                .join(Lists.Locality, JoinType.INNER, People.locality_id, Lists.Locality[Lists.id])
                .join(Lists.DocumentType, JoinType.INNER, People.document_type, Lists.DocumentType[Lists.id])
                .join(Lists.Gender, JoinType.INNER, People.gender_id, Lists.Gender[Lists.id])
                .join(Lists.PopulationGroup, JoinType.INNER, People.population_group_id, Lists.PopulationGroup[Lists.id])
                .join(Lists.EthnicGroup, JoinType.INNER, People.ethnic_group_id, Lists.EthnicGroup[Lists.id])
                .join(Lists.Disability, JoinType.INNER, People.disability_id, Lists.Disability[Lists.id])
                .join(Lists.CompanyType, JoinType.INNER, Companies.company_type_id, Lists.CompanyType[Lists.id])
                .join(Lists.CompanySize, JoinType.INNER, Companies.company_size_id, Lists.CompanySize[Lists.id])
                .join(Lists.Ccf, JoinType.INNER, Companies.ccf_id, Lists.Ccf[Lists.id])
                .join(Lists.Bank, JoinType.INNER, Companies.bank_id, Lists.Bank[Lists.id])
                .join(Lists.AccountBankType, JoinType.INNER, Companies.account_bank_type_id, Lists.AccountBankType[Lists.id])
                .join(companyLocality, JoinType.INNER, Companies.locality_id, companyLocality[Lists.id])
                .select { PeopleCompanies.state.eq(state.name) and
                        PeopleCompanies.active.eq(true) }
                .map { toPeopleCompanyValidation(it) }
        }

    override suspend fun findAllValidationAtLeastOnePaid(): List<PeopleCompanyValidation> =
        newSuspendedTransaction(Dispatchers.IO) {
            val companyLocality = Lists.alias("CompanyLocality")
            PeopleCompanies
                .join(Lists.ContractType, JoinType.INNER, PeopleCompanies.contract_type_id, Lists.ContractType[Lists.id])
                .join(Lists.MothRequestApplied, JoinType.INNER, PeopleCompanies.month_request_applied_id, Lists.MothRequestApplied[Lists.id])
                .join(Lists.CurrentMonthApplied, JoinType.INNER, PeopleCompanies.current_month_applied, Lists.CurrentMonthApplied[Lists.id])
                .join(Lists.ArlLevel, JoinType.INNER, PeopleCompanies.arl_level_id, Lists.ArlLevel[Lists.id])
                .join(Companies, JoinType.INNER, PeopleCompanies.company_id, Companies.id)
                .join(People, JoinType.INNER, PeopleCompanies.people_id, People.id)
                .join(Lists.Locality, JoinType.INNER, People.locality_id, Lists.Locality[Lists.id])
                .join(Lists.DocumentType, JoinType.INNER, People.document_type, Lists.DocumentType[Lists.id])
                .join(Lists.Gender, JoinType.INNER, People.gender_id, Lists.Gender[Lists.id])
                .join(Lists.PopulationGroup, JoinType.INNER, People.population_group_id, Lists.PopulationGroup[Lists.id])
                .join(Lists.EthnicGroup, JoinType.INNER, People.ethnic_group_id, Lists.EthnicGroup[Lists.id])
                .join(Lists.Disability, JoinType.INNER, People.disability_id, Lists.Disability[Lists.id])
                .join(Lists.CompanyType, JoinType.INNER, Companies.company_type_id, Lists.CompanyType[Lists.id])
                .join(Lists.CompanySize, JoinType.INNER, Companies.company_size_id, Lists.CompanySize[Lists.id])
                .join(Lists.Ccf, JoinType.INNER, Companies.ccf_id, Lists.Ccf[Lists.id])
                .join(Lists.Bank, JoinType.INNER, Companies.bank_id, Lists.Bank[Lists.id])
                .join(Lists.AccountBankType, JoinType.INNER, Companies.account_bank_type_id, Lists.AccountBankType[Lists.id])
                .join(companyLocality, JoinType.INNER, Companies.locality_id, companyLocality[Lists.id])
                .select { PeopleCompanies.active.eq(true) }
                .map { toPeopleCompanyValidation(it) }
        }

    override suspend fun save(peopleCompany: PeopleCompany): UUID =
        newSuspendedTransaction(Dispatchers.IO) {
            PeopleCompanies.insert {
                it[id] = UUID.randomUUID()
                it[people_id] = UUID.fromString(peopleCompany.peopleId.value)
                it[company_id] = UUID.fromString(peopleCompany.companyId.value)
                it[contract_type_id] = peopleCompany.contractType.id
                it[duration] = peopleCompany.duration
                it[start_date] = peopleCompany.startDate
                it[end_date] = peopleCompany.endDate
                it[monthly_income] = peopleCompany.monthlyIncome
                it[month_request_applied_id] = peopleCompany.monthRequestApplied.id
                it[current_month_applied] = peopleCompany.currentMonthApplied.id
                it[arl_level_id] = peopleCompany.arlLevel.id
                it[active] = true
                it[created_at] = LocalDateTime.now()
                it[updated_at] = LocalDateTime.now()
                it[created_by_id] = peopleCompany.createdBy.value
                it[assigned_to_id] = peopleCompany.assignedTo.value
            } get PeopleCompanies.id
        }

    override suspend fun update(id: UUID, peopleCompany: PeopleCompany) =
        newSuspendedTransaction(Dispatchers.IO) {
            PeopleCompanies.update({ PeopleCompanies.id eq id }) {
                it[company_id] = UUID.fromString(peopleCompany.companyId.value)
                it[contract_type_id] = peopleCompany.contractType.id
                it[duration] = peopleCompany.duration
                it[start_date] = peopleCompany.startDate
                it[end_date] = peopleCompany.endDate
                it[monthly_income] = peopleCompany.monthlyIncome
                it[month_request_applied_id] = peopleCompany.monthRequestApplied.id
                it[current_month_applied] = peopleCompany.currentMonthApplied.id
                it[arl_level_id] = peopleCompany.arlLevel.id
                it[active] = true
                it[updated_at] = LocalDateTime.now()
            }
            Unit
        }

    override suspend fun update(id: UUID, newState: AttachmentState) =
        newSuspendedTransaction(Dispatchers.IO) {
            PeopleCompanies.update({ PeopleCompanies.id eq id }) {
                it[state] = newState.toString()
                it[updated_at] = LocalDateTime.now()
            }
            Unit
        }


    override suspend fun delete(id: UUID) =
        newSuspendedTransaction(Dispatchers.IO) {
            PeopleCompanies.update({ PeopleCompanies.id eq id }) {
                it[active] = false
                it[updated_at] = LocalDateTime.now()
            }
            Unit
        }

    private suspend fun toPeopleCompany(row: ResultRow): PeopleCompany =
        PeopleCompany(
            id = PeopleCompanyId(row[PeopleCompanies.id].toString()),
            peopleId = PeopleId(row[PeopleCompanies.people_id].toString()),
            companyId = CompanyId(row[PeopleCompanies.company_id].toString()),
            companyName = CompanyName(row[Companies.name]),
            contractType = ValueList(row[Lists.ContractType[Lists.id]], row[Lists.ContractType[Lists.name]], row[Lists.ContractType[Lists.list]], row[Lists.ContractType[Lists.active]]),
            duration = row[PeopleCompanies.duration],
            startDate = row[PeopleCompanies.start_date],
            endDate = row[PeopleCompanies.end_date],
            monthlyIncome = row[PeopleCompanies.monthly_income],
            monthRequestApplied = ValueList(row[Lists.MothRequestApplied[Lists.id]], row[Lists.MothRequestApplied[Lists.name]], row[Lists.MothRequestApplied[Lists.list]], row[Lists.MothRequestApplied[Lists.active]]),
            currentMonthApplied = ValueList(row[Lists.CurrentMonthApplied[Lists.id]], row[Lists.CurrentMonthApplied[Lists.name]], row[Lists.CurrentMonthApplied[Lists.list]], row[Lists.CurrentMonthApplied[Lists.active]]),
            arlLevel = ValueList(row[Lists.ArlLevel[Lists.id]], row[Lists.ArlLevel[Lists.name]], row[Lists.ArlLevel[Lists.list]], row[Lists.ArlLevel[Lists.active]]),
            state = AttachmentState.valueOf(row[PeopleCompanies.state]),
            createdAt = row[PeopleCompanies.created_at],
            updatedAt = row[PeopleCompanies.updated_at],
            attachments = DefaultPeopleFileRepo().findAllBy(PeopleCompanyId(row[PeopleCompanies.id].toString())),
            payments = DefaultPaymentRepo(db).findAllBy(PeopleCompanyId(row[PeopleCompanies.id].toString())),
            createdBy = UserId(row[PeopleCompanies.created_by_id]),
            assignedTo = UserId(row[PeopleCompanies.assigned_to_id])
        )

    private suspend fun toPeopleCompanyLite(row: ResultRow): PeopleCompanyLite =
        PeopleCompanyLite(
            id = PeopleCompanyId(row[PeopleCompanies.id].toString()),
            peopleName = PersonName(row[People.name] + " " + row[People.last_name]),
            peopleDocument = PersonDocument(row[People.document]),
            peopleAttachmentsCount = row[PeopleAttachments.PeopleAttachmentCount[PeopleAttachments.PeopleAttachmentCountVal]],
            companyName = CompanyName(row[Companies.name]),
            companyDocument = CompanyDocument(row[Companies.document]),
            companyAttachmentsCount = row[CompaniesAttachment.CompanyAttachmentCount[CompaniesAttachment.CompanyAttachmentCountVal]],
            state = AttachmentState.valueOf(row[PeopleCompanies.state]),
            createdAt = row[PeopleCompanies.created_at],
            paymentsCount = DefaultPaymentRepo(db).findAllBy(PeopleCompanyId(row[PeopleCompanies.id].toString())).size,
            active = row[PeopleCompanies.active]
        )

    private suspend fun toPeopleCompanyValidation(row: ResultRow): PeopleCompanyValidation = PeopleCompanyValidation(
        id = PeopleCompanyId(row[PeopleCompanies.id].toString()),
        people = toPeople(row),
        company = toCompany(row),
        contractType = ValueList(row[Lists.ContractType[Lists.id]], row[Lists.ContractType[Lists.name]], row[Lists.ContractType[Lists.list]], row[Lists.ContractType[Lists.active]]),
        duration = row[PeopleCompanies.duration],
        startDate = row[PeopleCompanies.start_date],
        endDate = row[PeopleCompanies.end_date],
        monthlyIncome = row[PeopleCompanies.monthly_income],
        monthRequestApplied = ValueList(row[Lists.MothRequestApplied[Lists.id]], row[Lists.MothRequestApplied[Lists.name]], row[Lists.MothRequestApplied[Lists.list]], row[Lists.MothRequestApplied[Lists.active]]),
        currentMonthApplied = ValueList(row[Lists.CurrentMonthApplied[Lists.id]], row[Lists.CurrentMonthApplied[Lists.name]], row[Lists.CurrentMonthApplied[Lists.list]], row[Lists.CurrentMonthApplied[Lists.active]]),
        arlLevel = ValueList(row[Lists.ArlLevel[Lists.id]], row[Lists.ArlLevel[Lists.name]], row[Lists.ArlLevel[Lists.list]], row[Lists.ArlLevel[Lists.active]]),
        state = AttachmentState.valueOf(row[PeopleCompanies.state]),
        createdAt = row[PeopleCompanies.created_at],
        updatedAt = row[PeopleCompanies.updated_at],
        attachments = DefaultPeopleFileRepo().findAllBy(PeopleCompanyId(row[PeopleCompanies.id].toString())),
        payments = DefaultPaymentRepo(db).findAllBy(PeopleCompanyId(row[PeopleCompanies.id].toString()))
    )

    private fun toPeople(row: ResultRow): com.qohat.domain.People =
        com.qohat.domain.People(
            id = PeopleId(row[People.id].toString()),
            name = row[People.name],
            lastName = row[People.last_name],
            documentType = ValueList(row[Lists.DocumentType[Lists.id]], row[Lists.DocumentType[Lists.name]], row[Lists.DocumentType[Lists.list]], row[Lists.DocumentType[Lists.active]]),
            document = row[People.document],
            issueDocumentDate = row[People.issue_document_date] ?: LocalDate.now(),
            birthday = row[People.birthday],
            gender = ValueList(row[Lists.Gender[Lists.id]], row[Lists.Gender[Lists.name]], row[Lists.Gender[Lists.list]], row[Lists.Gender[Lists.active]]),
            address = row[People.address].toOption(),
            locality = ValueList(row[Lists.Locality[Lists.id]], row[Lists.Locality[Lists.name]], row[Lists.Locality[Lists.list]], row[Lists.Locality[Lists.active]]),
            neighborhood = row[People.neighborhood].toOption(),
            phone = row[People.phone].toOption(),
            cellPhone = row[People.cell_phone].toOption(),
            email = row[People.email],
            populationGroup = ValueList(row[Lists.PopulationGroup[Lists.id]], row[Lists.PopulationGroup[Lists.name]], row[Lists.PopulationGroup[Lists.list]], row[Lists.PopulationGroup[Lists.active]]),
            ethnicGroup = ValueList(row[Lists.EthnicGroup[Lists.id]], row[Lists.EthnicGroup[Lists.name]], row[Lists.EthnicGroup[Lists.list]], row[Lists.EthnicGroup[Lists.active]]),
            disability = ValueList(row[Lists.Disability[Lists.id]], row[Lists.Disability[Lists.name]], row[Lists.Disability[Lists.list]], row[Lists.Disability[Lists.active]]),
            active = row[People.active],
            createdAt = row[People.created_at],
            updatedAt = row[People.updated_at],
            peopleCompanies = listOf(),
            createdBy = row[People.created_by_id]?.let { UserId(it) } ?: UserId(UUID.randomUUID()),
        )

    private suspend fun toCompany(row: ResultRow): Company =
        Company(
            id = CompanyId(row[Companies.id].toString()),
            name = row[Companies.name],
            companyType = ValueList(row[Lists.CompanyType[Lists.id]], row[Lists.CompanyType[Lists.name]], row[Lists.CompanyType[Lists.list]], row[Lists.CompanyType[Lists.active]]),
            document = row[Companies.document],
            address = row[Companies.address],
            locality = ValueList(row[Lists.Locality[Lists.id]], row[Lists.Locality[Lists.name]], row[Lists.Locality[Lists.list]], row[Lists.Locality[Lists.active]]),
            neighborhood = row[Companies.neighborhood].toOption(),
            phone = row[Companies.phone].toOption(),
            cellPhone = row[Companies.cell_phone].toOption(),
            email = row[Companies.email],
            employees = row[Companies.employees],
            companySize = ValueList(row[Lists.CompanySize[Lists.id]], row[Lists.CompanySize[Lists.name]], row[Lists.CompanySize[Lists.list]], row[Lists.CompanySize[Lists.active]]),
            economicActivity = row[Companies.economic_activity_code].toOption(),
            ccf = ValueList(row[Lists.Ccf[Lists.id]], row[Lists.Ccf[Lists.name]], row[Lists.Ccf[Lists.list]], row[Lists.Ccf[Lists.active]]),
            legalRepresentative = row[Companies.legal_representative].toOption(),
            legalRepresentativeDocument = row[Companies.legal_representative_document].toOption(),
            postulationResponsible = row[Companies.postulation_responsible].toOption(),
            postulationResponsiblePhone = row[Companies.postulation_responsible_phone].toOption(),
            postulationResponsiblePosition = row[Companies.postulation_responsible_position].toOption(),
            postulationResponsibleEmail = row[Companies.postulation_responsible_email].toOption(),
            bank = ValueList(row[Lists.Bank[Lists.id]], row[Lists.Bank[Lists.name]], row[Lists.Bank[Lists.list]], row[Lists.Bank[Lists.active]]),
            accountBankType = ValueList(row[Lists.AccountBankType[Lists.id]], row[Lists.AccountBankType[Lists.name]], row[Lists.AccountBankType[Lists.list]], row[Lists.AccountBankType[Lists.active]]),
            accountNumber = row[Companies.account_number],
            active = row[Companies.active],
            createdAt = row[Companies.created_at],
            updatedAt = row[Companies.updated_at],
            attachments = DefaultCompanyFileRepo().findAllBy(CompanyId(row[Companies.id].toString())),
            createdBy = UserId(row[Companies.created_by_id])
        )

    private suspend fun toPeopleCompanyForPayment(row: ResultRow): PeopleCompanyForPayment =
        PeopleCompanyForPayment(
            id = PeopleCompanyId(row[PeopleCompanies.id].toString()),
            currentMonthApplied = ValueList(row[Lists.CurrentMonthApplied[Lists.id]], row[Lists.CurrentMonthApplied[Lists.name]], row[Lists.CurrentMonthApplied[Lists.list]], row[Lists.CurrentMonthApplied[Lists.active]]),
            arlLevel = ValueList(row[Lists.ArlLevel[Lists.id]], row[Lists.ArlLevel[Lists.name]], row[Lists.ArlLevel[Lists.list]], row[Lists.ArlLevel[Lists.active]]),
            paymentsCount = DefaultPaymentRepo(db).findAllBy(PeopleCompanyId(row[PeopleCompanies.id].toString())).size
        )


}