package com.qohat.repo

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.singleOrNone
import arrow.core.toOption
import com.qohat.domain.*
import com.qohat.entities.Companies
import com.qohat.entities.Lists
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.util.*

interface CompanyRepo {
    suspend fun findBy(id: UUID): Option<Company>
    suspend fun findBy(document: CompanyDocument): Option<Company>
    suspend fun findBy(email: CompanyEmail): Option<Company>
    suspend fun findBy(name: CompanyName): Option<Company>
    suspend fun findAllBy(ccfId: CcfId): List<Company>
    suspend fun findAll(): List<Company>
    suspend fun save(company: Company): UUID
    suspend fun update(id: UUID, company: Company)
    suspend fun delete(id: UUID)
}

class DefaultCompanyRepo: CompanyRepo {

    override suspend fun findBy(id: UUID): Option<Company> =
        newSuspendedTransaction(Dispatchers.IO) {
            Companies
                .join(Lists.CompanyType, JoinType.INNER, Companies.company_type_id, Lists.CompanyType[Lists.id])
                .join(Lists.Locality, JoinType.INNER, Companies.locality_id, Lists.Locality[Lists.id])
                .join(Lists.CompanySize, JoinType.INNER, Companies.company_size_id, Lists.CompanySize[Lists.id])
                .join(Lists.Ccf, JoinType.INNER, Companies.ccf_id, Lists.Ccf[Lists.id])
                .join(Lists.Bank, JoinType.INNER, Companies.bank_id, Lists.Bank[Lists.id])
                .join(Lists.AccountBankType, JoinType.INNER, Companies.account_bank_type_id, Lists.AccountBankType[Lists.id])
                .select { Companies.id.eq(id) and Companies.active.eq(true) }
                .singleOrNone()
                .map { toCompany(it) }
        }

    override suspend fun findBy(document: CompanyDocument): Option<Company> =
        newSuspendedTransaction(Dispatchers.IO) {
            Companies
                .join(Lists.CompanyType, JoinType.INNER, Companies.company_type_id, Lists.CompanyType[Lists.id])
                .join(Lists.Locality, JoinType.INNER, Companies.locality_id, Lists.Locality[Lists.id])
                .join(Lists.CompanySize, JoinType.INNER, Companies.company_size_id, Lists.CompanySize[Lists.id])
                .join(Lists.Ccf, JoinType.INNER, Companies.ccf_id, Lists.Ccf[Lists.id])
                .join(Lists.Bank, JoinType.INNER, Companies.bank_id, Lists.Bank[Lists.id])
                .join(Lists.AccountBankType, JoinType.INNER, Companies.account_bank_type_id, Lists.AccountBankType[Lists.id])
                .select { Companies.document.eq(document.value) }
                .singleOrNone()
                .map { toCompany(it) }
        }

    override suspend fun findBy(email: CompanyEmail): Option<Company> =
        newSuspendedTransaction(Dispatchers.IO) {
            Companies
                .join(Lists.CompanyType, JoinType.INNER, Companies.company_type_id, Lists.CompanyType[Lists.id])
                .join(Lists.Locality, JoinType.INNER, Companies.locality_id, Lists.Locality[Lists.id])
                .join(Lists.CompanySize, JoinType.INNER, Companies.company_size_id, Lists.CompanySize[Lists.id])
                .join(Lists.Ccf, JoinType.INNER, Companies.ccf_id, Lists.Ccf[Lists.id])
                .join(Lists.Bank, JoinType.INNER, Companies.bank_id, Lists.Bank[Lists.id])
                .join(Lists.AccountBankType, JoinType.INNER, Companies.account_bank_type_id, Lists.AccountBankType[Lists.id])
                .select { Companies.email.eq(email.value) }
                .singleOrNone()
                .map { toCompany(it) }
        }

    override suspend fun findBy(name: CompanyName): Option<Company> =
        newSuspendedTransaction(Dispatchers.IO) {
            Companies
                .join(Lists.CompanyType, JoinType.INNER, Companies.company_type_id, Lists.CompanyType[Lists.id])
                .join(Lists.Locality, JoinType.INNER, Companies.locality_id, Lists.Locality[Lists.id])
                .join(Lists.CompanySize, JoinType.INNER, Companies.company_size_id, Lists.CompanySize[Lists.id])
                .join(Lists.Ccf, JoinType.INNER, Companies.ccf_id, Lists.Ccf[Lists.id])
                .join(Lists.Bank, JoinType.INNER, Companies.bank_id, Lists.Bank[Lists.id])
                .join(Lists.AccountBankType, JoinType.INNER, Companies.account_bank_type_id, Lists.AccountBankType[Lists.id])
                .select { Companies.name.eq(name.value) }
                .singleOrNone()
                .map { toCompany(it) }
        }

    override suspend fun findAllBy(ccfId: CcfId): List<Company> =
        newSuspendedTransaction(Dispatchers.IO) {
            Companies
                .join(Lists.CompanyType, JoinType.INNER, Companies.company_type_id, Lists.CompanyType[Lists.id])
                .join(Lists.Locality, JoinType.INNER, Companies.locality_id, Lists.Locality[Lists.id])
                .join(Lists.CompanySize, JoinType.INNER, Companies.company_size_id, Lists.CompanySize[Lists.id])
                .join(Lists.Ccf, JoinType.INNER, Companies.ccf_id, Lists.Ccf[Lists.id])
                .join(Lists.Bank, JoinType.INNER, Companies.bank_id, Lists.Bank[Lists.id])
                .join(Lists.AccountBankType, JoinType.INNER, Companies.account_bank_type_id, Lists.AccountBankType[Lists.id])
                .select { Companies.active.eq(true) and Companies.ccf_id.eq(ccfId.value) }
                .map { toCompany(it) }
        }

    override suspend fun findAll(): List<Company> =
        newSuspendedTransaction(Dispatchers.IO) {
            Companies
                .join(Lists.CompanyType, JoinType.INNER, Companies.company_type_id, Lists.CompanyType[Lists.id])
                .join(Lists.Locality, JoinType.INNER, Companies.locality_id, Lists.Locality[Lists.id])
                .join(Lists.CompanySize, JoinType.INNER, Companies.company_size_id, Lists.CompanySize[Lists.id])
                .join(Lists.Ccf, JoinType.INNER, Companies.ccf_id, Lists.Ccf[Lists.id])
                .join(Lists.Bank, JoinType.INNER, Companies.bank_id, Lists.Bank[Lists.id])
                .join(Lists.AccountBankType, JoinType.INNER, Companies.account_bank_type_id, Lists.AccountBankType[Lists.id])
                .select { Companies.active.eq(true) }
                .map { toCompany(it) }
        }

    override suspend fun save(company: Company): UUID =
        newSuspendedTransaction(Dispatchers.IO) {
            Companies.insert {
                it[id] = UUID.randomUUID()
                it[name] = company.name
                it[company_type_id] = company.companyType.id
                it[document] = company.document
                it[address] = company.address
                it[locality_id] = company.locality.id
                it[neighborhood] = company.neighborhood.getOrElse { null }
                it[phone] = company.phone.getOrElse { null }
                it[cell_phone] = company.cellPhone.getOrElse { null }
                it[email] = company.email
                it[employees] = company.employees
                it[company_size_id] = company.companySize.id
                it[economic_activity_code] = company.economicActivity.getOrElse { null }
                it[ccf_id] = company.ccf.id
                it[legal_representative] = company.legalRepresentative.getOrElse { null }
                it[legal_representative_document] = company.legalRepresentativeDocument.getOrElse { null }
                it[postulation_responsible] = company.postulationResponsible.getOrElse { null }
                it[postulation_responsible_phone] = company.postulationResponsiblePhone.getOrElse { null }
                it[postulation_responsible_position] = company.postulationResponsiblePosition.getOrElse { null }
                it[postulation_responsible_email] = company.postulationResponsibleEmail.getOrElse { null }
                it[bank_id] = company.bank.id
                it[account_bank_type_id] = company.accountBankType.id
                it[account_number] = company.accountNumber
                it[created_at] = LocalDateTime.now()
                it[updated_at] = LocalDateTime.now()
                it[created_by_id] = company.createdBy.value
            } get Companies.id
        }

    override suspend fun update(id: UUID, company: Company) =
        newSuspendedTransaction(Dispatchers.IO) {
            Companies.update({ Companies.id eq id }) {
                it[name] = company.name
                it[company_type_id] = company.companyType.id
                it[document] = company.document
                it[address] = company.address
                it[locality_id] = company.locality.id
                it[neighborhood] = company.neighborhood.getOrElse { null }
                it[phone] = company.phone.getOrElse { null }
                it[cell_phone] = company.cellPhone.getOrElse { null }
                it[email] = company.email
                it[employees] = company.employees
                it[company_size_id] = company.companySize.id
                it[economic_activity_code] = company.economicActivity.getOrElse { null }
                it[ccf_id] = company.ccf.id
                it[legal_representative] = company.legalRepresentative.getOrElse { null }
                it[legal_representative_document] = company.legalRepresentativeDocument.getOrElse { null }
                it[postulation_responsible] = company.postulationResponsible.getOrElse { null }
                it[postulation_responsible_phone] = company.postulationResponsiblePhone.getOrElse { null }
                it[postulation_responsible_position] = company.postulationResponsiblePosition.getOrElse { null }
                it[postulation_responsible_email] = company.postulationResponsibleEmail.getOrElse { null }
                it[bank_id] = company.bank.id
                it[account_bank_type_id] = company.accountBankType.id
                it[account_number] = company.accountNumber
                if(company.women > 0) { it[women] = company.women }
                if(company.men > 0) { it[men] = company.men }
                it[updated_at] = LocalDateTime.now()
            }
            Unit
        }


    override suspend fun delete(id: UUID) =
        newSuspendedTransaction(Dispatchers.IO) {
            Companies.update({ Companies.id eq id }) {
                it[active] = false
                it[updated_at] = LocalDateTime.now()
            }
            Unit
        }

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
            createdBy = UserId(row[Companies.created_by_id]),
            women = row[Companies.women],
            men = row[Companies.men],
        )
}