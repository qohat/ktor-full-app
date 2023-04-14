package com.qohat.repo

import arrow.core.Either
import com.qohat.domain.Document
import com.qohat.domain.Email
import com.qohat.domain.Limit
import com.qohat.domain.NewPaginationParams
import com.qohat.domain.NewPeople
import com.qohat.domain.NewPeopleId
import com.qohat.domain.Offset
import com.qohat.domain.Pagination
import com.qohat.domain.UserId
import com.qohat.entities.OrganizationBelongingInformation
import com.qohat.entities.TempUserProduct
import com.qohat.entities.TempUserProducts
import com.qohat.entities.toNewPeople
import com.qohat.entities.toTempUserProduct
import com.qohat.error.DomainError
import org.jetbrains.exposed.sql.Database
import com.qohat.entities.PaymentInformation as PaymentInformationEntity
import com.qohat.entities.People as PeopleEntity
import com.qohat.entities.PropertyInformation as PropertyInformationEntity

interface PeopleRepo {
    suspend fun findBy(email: Email): Either<DomainError, NewPeople?>
    suspend fun findBy(newPeopleId: NewPeopleId): Either<DomainError, NewPeople?>
    suspend fun findBy(document: Document): Either<DomainError, NewPeople?>
    suspend fun findAllBy(userId: UserId, params: NewPaginationParams): Either<DomainError, List<NewPeople>>
    suspend fun findAll(): Either<DomainError, List<NewPeople>>
    suspend fun findNumbersBy(limit: Limit, offset: Offset): Either<DomainError, List<TempUserProduct>>
    suspend fun save(people: NewPeople): Either<DomainError, NewPeopleId>
    suspend fun update(id: NewPeopleId, people: NewPeople): Either<DomainError, Unit>
}

class DefaultPeopleRepo(private val db: Database): PeopleRepo {

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
}