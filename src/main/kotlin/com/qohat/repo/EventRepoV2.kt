package com.qohat.repo

import arrow.core.Either
import com.qohat.domain.AttachmentsValidationEvent
import com.qohat.domain.AttachmentsValidationEventShow
import com.qohat.domain.NewEventId
import com.qohat.domain.requests.BillReturnId
import com.qohat.domain.requests.BillReturnObservation
import com.qohat.domain.requests.BillReturnObservationId
import com.qohat.entities.BillReturnObservations
import com.qohat.entities.BillReturnsValidationAttachmentsEvents
import com.qohat.entities.toAttachmentsValidationEventShow
import com.qohat.entities.toBillReturnObservation
import com.qohat.error.DomainError
import org.jetbrains.exposed.sql.Database

interface EventRepoV2 {
    suspend fun saveBillReturnsEvent(event: AttachmentsValidationEvent): Either<DomainError, NewEventId>
    suspend fun saveBillReturnsEvents(events: List<AttachmentsValidationEvent>): Either<DomainError, Unit>
    suspend fun findAllBy(billReturnId: BillReturnId): Either<DomainError, List<AttachmentsValidationEventShow>>
    suspend fun save(billReturnObservation: BillReturnObservation): Either<DomainError, BillReturnObservationId>
    suspend fun findAllObservations(billReturnId: BillReturnId): Either<DomainError, List<BillReturnObservation>>

}

class DefaultEventRepoV2(private val db: Database): EventRepoV2 {
    override suspend fun saveBillReturnsEvent(event: AttachmentsValidationEvent): Either<DomainError, NewEventId> =
        transact(db) {
            val id = BillReturnsValidationAttachmentsEvents.insertQuery(event)
            NewEventId(id)
        }

    override suspend fun saveBillReturnsEvents(events: List<AttachmentsValidationEvent>): Either<DomainError, Unit> =
        transact(db) {
            BillReturnsValidationAttachmentsEvents.insertBatch(events)
        }.void()

    override suspend fun findAllBy(billReturnId: BillReturnId): Either<DomainError, List<AttachmentsValidationEventShow>> =
        transact(db) {
            BillReturnsValidationAttachmentsEvents.selectQueryBy(billReturnId).map { it.toAttachmentsValidationEventShow() }
        }

    override suspend fun save(billReturnObservation: BillReturnObservation): Either<DomainError, BillReturnObservationId> =
        transact(db) {
            val id = BillReturnObservations.insertQuery(billReturnObservation)
            BillReturnObservationId(id)
        }

    override suspend fun findAllObservations(billReturnId: BillReturnId): Either<DomainError, List<BillReturnObservation>> =
        transact(db) {
            BillReturnObservations.selectBy(billReturnId).map { it.toBillReturnObservation() }
        }
}