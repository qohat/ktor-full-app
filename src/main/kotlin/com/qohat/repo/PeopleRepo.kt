package com.qohat.repo

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.singleOrNone
import arrow.core.toOption
import com.qohat.domain.*
import com.qohat.entities.Lists
import com.qohat.entities.OrganizationBelongingInformation
import com.qohat.entities.TempUserProduct
import com.qohat.entities.TempUserProducts
import com.qohat.entities.toNewPeople
import com.qohat.entities.toTempUserProduct
import com.qohat.error.DomainError
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.orWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import com.qohat.entities.PaymentInformation as PaymentInformationEntity
import com.qohat.entities.People as PeopleEntity
import com.qohat.entities.PropertyInformation as PropertyInformationEntity

interface PeopleRepo {
    suspend fun findBy(id: UUID): Option<People>
    suspend fun findBy(document: PersonDocument): Option<People>
    @Deprecated("Use findBy(Email): NewPeople?")
    suspend fun findBy(email: PersonEmail): Option<People>
    suspend fun findBy(email: Email): Either<DomainError, NewPeople?>
    suspend fun findBy(newPeopleId: NewPeopleId): Either<DomainError, NewPeople?>
    suspend fun findBy(document: Document): Either<DomainError, NewPeople?>
    suspend fun findAllBy(userId: UserId, params: PaginationParams): List<People>
    suspend fun findAllBy(userId: UserId, params: NewPaginationParams): Either<DomainError, List<NewPeople>>
    suspend fun findAll(): Either<DomainError, List<NewPeople>>
    suspend fun findNumbersBy(limit: Limit, offset: Offset): Either<DomainError, List<TempUserProduct>>
    @Deprecated("Use save(newPeople)")
    suspend fun save(people: People): UUID
    suspend fun save(people: NewPeople): Either<DomainError, NewPeopleId>
    suspend fun update(id: UUID, people: People)
    suspend fun update(id: NewPeopleId, people: NewPeople): Either<DomainError, Unit>
    suspend fun delete(id: UUID)
}

class DefaultPeopleRepo(private val db: Database): PeopleRepo {

    override suspend fun findBy(id: UUID): Option<People> =
        newSuspendedTransaction(Dispatchers.IO, db) {
            PeopleEntity
                .join(Lists.Locality, JoinType.INNER, PeopleEntity.locality_id, Lists.Locality[Lists.id])
                .join(Lists.DocumentType, JoinType.INNER, PeopleEntity.document_type, Lists.DocumentType[Lists.id])
                .join(Lists.Gender, JoinType.INNER, PeopleEntity.gender_id, Lists.Gender[Lists.id])
                .join(Lists.PopulationGroup, JoinType.INNER, PeopleEntity.population_group_id, Lists.PopulationGroup[Lists.id])
                .join(Lists.EthnicGroup, JoinType.INNER, PeopleEntity.ethnic_group_id, Lists.EthnicGroup[Lists.id])
                .join(Lists.Disability, JoinType.INNER, PeopleEntity.disability_id, Lists.Disability[Lists.id])
                .select { PeopleEntity.id.eq(id) and PeopleEntity.active.eq(true) }
                .singleOrNone()
                .map { toPeople(it) }
        }

    override suspend fun findBy(document: PersonDocument): Option<People> =
        newSuspendedTransaction(Dispatchers.IO, db) {
            PeopleEntity
                .join(Lists.Locality, JoinType.INNER, PeopleEntity.locality_id, Lists.Locality[Lists.id])
                .join(Lists.DocumentType, JoinType.INNER, PeopleEntity.document_type, Lists.DocumentType[Lists.id])
                .join(Lists.Gender, JoinType.INNER, PeopleEntity.gender_id, Lists.Gender[Lists.id])
                .join(Lists.PopulationGroup, JoinType.INNER, PeopleEntity.population_group_id, Lists.PopulationGroup[Lists.id])
                .join(Lists.EthnicGroup, JoinType.INNER, PeopleEntity.ethnic_group_id, Lists.EthnicGroup[Lists.id])
                .join(Lists.Disability, JoinType.INNER, PeopleEntity.disability_id, Lists.Disability[Lists.id])
                .select { PeopleEntity.document.eq(document.value) }
                .singleOrNone()
                .map { toPeople(it) }
        }

    override suspend fun findBy(email: PersonEmail): Option<People> =
        newSuspendedTransaction(Dispatchers.IO, db) {
            PeopleEntity
                .join(Lists.Locality, JoinType.INNER, PeopleEntity.locality_id, Lists.Locality[Lists.id])
                .join(Lists.DocumentType, JoinType.INNER, PeopleEntity.document_type, Lists.DocumentType[Lists.id])
                .join(Lists.Gender, JoinType.INNER, PeopleEntity.gender_id, Lists.Gender[Lists.id])
                .join(Lists.PopulationGroup, JoinType.INNER, PeopleEntity.population_group_id, Lists.PopulationGroup[Lists.id])
                .join(Lists.EthnicGroup, JoinType.INNER, PeopleEntity.ethnic_group_id, Lists.EthnicGroup[Lists.id])
                .join(Lists.Disability, JoinType.INNER, PeopleEntity.disability_id, Lists.Disability[Lists.id])
                .select { PeopleEntity.email.eq(email.value) }
                .singleOrNone()
                .map { toPeople(it) }
        }

    override suspend fun findAllBy(userId: UserId, params: PaginationParams): List<People> =
        newSuspendedTransaction(Dispatchers.IO, db) {
            val query = PeopleEntity
                .join(Lists.Locality, JoinType.INNER, PeopleEntity.locality_id, Lists.Locality[Lists.id])
                .join(Lists.DocumentType, JoinType.INNER, PeopleEntity.document_type, Lists.DocumentType[Lists.id])
                .join(Lists.Gender, JoinType.INNER, PeopleEntity.gender_id, Lists.Gender[Lists.id])
                .join(Lists.PopulationGroup, JoinType.INNER, PeopleEntity.population_group_id, Lists.PopulationGroup[Lists.id])
                .join(Lists.EthnicGroup, JoinType.INNER, PeopleEntity.ethnic_group_id, Lists.EthnicGroup[Lists.id])
                .join(Lists.Disability, JoinType.INNER, PeopleEntity.disability_id, Lists.Disability[Lists.id])

            val select = if (params.text.isNotBlank() or params.text.isNotEmpty()) {
                query.select {
                    concat(
                        separator = " ",
                        listOf(
                            PeopleEntity.name.lowerCase(),
                            PeopleEntity.last_name.lowerCase()
                        )
                    ) like "%${params.text.lowercase()}%"
                }.orWhere {
                    PeopleEntity.document like "%${params.text}%"
                }.andWhere {
                    PeopleEntity.active.eq(true) and PeopleEntity.created_by_id.eq(userId.value)
                }
            } else {
                query.select {
                    PeopleEntity.active.eq(true) and PeopleEntity.created_by_id.eq(userId.value) }
            }

            select
            .limit(params.limit, params.offset)
            .map { toPeople(it) }
        }

    override suspend fun findAllBy(userId: UserId, params: NewPaginationParams): Either<DomainError, List<NewPeople>> =
        transact(db) {
            PeopleEntity.selectAllQuery(params, userId).map { it.toNewPeople() }
        }

    override suspend fun findAll(): Either<DomainError, List<NewPeople>> = transact(db) {
        val pagination = Pagination(Limit(Int.MAX_VALUE), Offset(0))
        PeopleEntity.selectAllQuery(NewPaginationParams(pagination, null), null).map { it.toNewPeople() }
    }

    override suspend fun findNumbersBy(limit: Limit, offset: Offset): Either<DomainError, List<TempUserProduct>> =
        transact(db) {
            TempUserProducts.selectQuery(limit, offset).map { it.toTempUserProduct() }
        }

    override suspend fun save(people: People): UUID =
        newSuspendedTransaction(Dispatchers.IO, db) {
            PeopleEntity.insert {
                it[id] = UUID.randomUUID()
                it[name] = people.name
                it[last_name] = people.lastName
                it[document_type] = people.documentType.id
                it[document] = people.document
                it[issue_document_date] = people.issueDocumentDate
                it[birthday] = people.birthday
                it[gender_id] = people.gender.id
                it[address] = people.address.getOrElse { null }
                it[locality_id] = people.locality.id
                it[neighborhood] = people.neighborhood.getOrElse { null }
                it[phone] = people.phone.getOrElse { null }
                it[cell_phone] = people.cellPhone.getOrElse { null }
                it[email] = people.email
                it[population_group_id] =people.populationGroup.id
                it[ethnic_group_id] = people.ethnicGroup.id
                it[disability_id] = people.disability.id
                it[active] = true
                it[created_at] = LocalDateTime.now()
                it[updated_at] = LocalDateTime.now()
                it[created_by_id] = people.createdBy.value
            } get PeopleEntity.id
        }

    override suspend fun findBy(email: Email): Either<DomainError, NewPeople?> =
        transact(db) { PeopleEntity.selectByQuery(email)?.toNewPeople() }

    override suspend fun findBy(newPeopleId: NewPeopleId): Either<DomainError, NewPeople?> =
        transact(db) { PeopleEntity.selectByQuery(newPeopleId)?.toNewPeople() }

    override suspend fun findBy(document: Document): Either<DomainError, NewPeople?> =
        transact(db) { PeopleEntity.selectByQuery(document)?.toNewPeople() }

    override suspend fun save(people: NewPeople): Either<DomainError, NewPeopleId> =
        transact(db) {
            val peopleId = PeopleEntity.insertQuery(people)
            val newPeopleId = NewPeopleId(peopleId)
            people.propertyInformation?.let { it ->
                PropertyInformationEntity.insertQuery(newPeopleId, it)
            }
            people.organizationBelongingInfo?.let { it ->
                OrganizationBelongingInformation.insertQuery(newPeopleId, it)
            }
            people.paymentInformation?.let { it ->
                PaymentInformationEntity.insertQuery(newPeopleId, it)
            }
            newPeopleId
        }

    override suspend fun update(id: UUID, people: People) =
        newSuspendedTransaction(Dispatchers.IO, db) {
            PeopleEntity.update({ PeopleEntity.id eq id }) {
                it[name] = people.name
                it[last_name] = people.lastName
                it[document_type] = people.documentType.id
                it[document] = people.document
                it[issue_document_date] = people.issueDocumentDate
                it[birthday] = people.birthday
                it[gender_id] = people.gender.id
                it[address] = people.address.getOrElse { null }
                it[locality_id] = people.locality.id
                it[neighborhood] = people.neighborhood.getOrElse { null }
                it[phone] = people.phone.getOrElse { null }
                it[cell_phone] = people.cellPhone.getOrElse { null }
                it[email] = people.email
                it[population_group_id] =people.populationGroup.id
                it[ethnic_group_id] = people.ethnicGroup.id
                it[disability_id] = people.disability.id
                it[active] = true
                it[updated_at] = LocalDateTime.now()
            }
            Unit
        }

    override suspend fun update(id: NewPeopleId, people: NewPeople): Either<DomainError, Unit> =
        transact(db) {
            PeopleEntity.updateQuery(id, people)
            people.propertyInformation?.let {
                PropertyInformationEntity.updateQuery(id, it)
            }
            people.organizationBelongingInfo?.let {
                OrganizationBelongingInformation.updateQuery(id, it)
            }
            people.paymentInformation?.let {
                PaymentInformationEntity.updateQuery(id, it)
            }
        }.void()

    override suspend fun delete(id: UUID) =
        newSuspendedTransaction(Dispatchers.IO, db) {
            PeopleEntity.update({ PeopleEntity.id eq id }) {
                it[active] = false
                it[updated_at] = LocalDateTime.now()
            }
            Unit
        }

    @Deprecated("Use toNewPeople")
    private suspend fun toPeople(row: ResultRow): People =
        People(
            id = PeopleId(row[PeopleEntity.id].toString()),
            name = row[PeopleEntity.name],
            lastName = row[PeopleEntity.last_name],
            documentType = ValueList(row[Lists.DocumentType[Lists.id]], row[Lists.DocumentType[Lists.name]], row[Lists.DocumentType[Lists.list]], row[Lists.DocumentType[Lists.active]]),
            document = row[PeopleEntity.document],
            issueDocumentDate = row[PeopleEntity.issue_document_date] ?: LocalDate.now(),
            birthday = row[PeopleEntity.birthday],
            gender = ValueList(row[Lists.Gender[Lists.id]], row[Lists.Gender[Lists.name]], row[Lists.Gender[Lists.list]], row[Lists.Gender[Lists.active]]),
            address = row[PeopleEntity.address].toOption(),
            locality = ValueList(row[Lists.Locality[Lists.id]], row[Lists.Locality[Lists.name]], row[Lists.Locality[Lists.list]], row[Lists.Locality[Lists.active]]),
            neighborhood = row[PeopleEntity.neighborhood].toOption(),
            phone = row[PeopleEntity.phone].toOption(),
            cellPhone = row[PeopleEntity.cell_phone].toOption(),
            email = row[PeopleEntity.email],
            populationGroup = ValueList(row[Lists.PopulationGroup[Lists.id]], row[Lists.PopulationGroup[Lists.name]], row[Lists.PopulationGroup[Lists.list]], row[Lists.PopulationGroup[Lists.active]]),
            ethnicGroup = ValueList(row[Lists.EthnicGroup[Lists.id]], row[Lists.EthnicGroup[Lists.name]], row[Lists.EthnicGroup[Lists.list]], row[Lists.EthnicGroup[Lists.active]]),
            disability = ValueList(row[Lists.Disability[Lists.id]], row[Lists.Disability[Lists.name]], row[Lists.Disability[Lists.list]], row[Lists.Disability[Lists.active]]),
            active = row[PeopleEntity.active],
            createdAt = row[PeopleEntity.created_at],
            updatedAt = row[PeopleEntity.updated_at],
            createdBy = row[PeopleEntity.created_by_id]?.let { UserId(it) } ?: UserId(UUID.randomUUID()),
            peopleCompanies = DefaultPeopleCompanyRepo(db).findAllBy(PeopleId(row[PeopleEntity.id].toString()))
        )
}