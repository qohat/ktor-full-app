package com.qohat.repo

import arrow.core.Either
import arrow.core.traverse
import com.qohat.domain.AttachmentId
import com.qohat.domain.AttachmentIds
import com.qohat.domain.AttachmentState
import com.qohat.domain.BillReturnRequest
import com.qohat.domain.BillReturnResponse
import com.qohat.domain.NewAttachment
import com.qohat.domain.NewPaginationParams
import com.qohat.domain.NewPeopleId
import com.qohat.domain.NonContentAttachment
import com.qohat.domain.PeopleRequestBillReturn
import com.qohat.domain.PeopleRequestExpiration
import com.qohat.domain.PeopleRequestId
import com.qohat.domain.ProductShow
import com.qohat.domain.RequestExpiration
import com.qohat.domain.RequestState
import com.qohat.domain.RequestType
import com.qohat.domain.ResponseExpiration
import com.qohat.domain.SupplyDetail
import com.qohat.domain.SupplyDetailShow
import com.qohat.domain.UserId
import com.qohat.domain.requests.BillReturnId
import com.qohat.domain.requests.UpdateBillReturn
import com.qohat.entities.BillReturns
import com.qohat.entities.BillReturnsAttachments
import com.qohat.entities.BillReturnsSupplies
import com.qohat.entities.PeopleRequestExpirations
import com.qohat.entities.PeopleRequests
import com.qohat.entities.toNonContentAttachment
import com.qohat.entities.toPeopleRequestBillReturn
import com.qohat.entities.toPeopleRequestExpiration
import com.qohat.entities.toProductShow
import com.qohat.entities.toSupplyDetailShow
import com.qohat.error.DomainError
import org.jetbrains.exposed.sql.Database

interface PeopleRequestRepo {
    suspend fun save(billReturnReq: BillReturnRequest): Either<DomainError, BillReturnResponse>
    suspend fun saveAttachments(billReturnId: BillReturnId, newAttachments: List<NewAttachment>): Either<DomainError, Unit>
    suspend fun saveSupplies(billReturnId: BillReturnId, supplies: List<SupplyDetail>): Either<DomainError, Unit>
    suspend fun save(peopleRequestId: PeopleRequestId, peopleRequestExpiration: PeopleRequestExpiration): Either<DomainError, Unit>
    suspend fun update(
        peopleRequestId: PeopleRequestId,
        requestExpiration: RequestExpiration,
        responseExpiration: ResponseExpiration
    ): Either<DomainError, Unit>
    suspend fun findAll(state: RequestState, requestType: RequestType, params: NewPaginationParams, userId: UserId): Either<DomainError, List<PeopleRequestBillReturn>>
    suspend fun findAll(state: RequestState, requestType: RequestType, params: NewPaginationParams): Either<DomainError, List<PeopleRequestBillReturn>>
    suspend fun findBy(peopleRequestId: PeopleRequestId, requestType: RequestType): Either<DomainError, PeopleRequestBillReturn?>
    suspend fun findAllBy(peopleId: NewPeopleId, requestType: RequestType): Either<DomainError, List<PeopleRequestBillReturn>>
    suspend fun findSuppliesBy(billReturnId: BillReturnId): Either<DomainError, List<SupplyDetailShow>>
    suspend fun findProductBy(billReturnId: BillReturnId): Either<DomainError, ProductShow?>
    suspend fun findIdBy(billReturnId: BillReturnId): Either<DomainError, PeopleRequestId?>
    suspend fun findAttachmentsBy(billReturnId: BillReturnId): Either<DomainError, List<NonContentAttachment>>
    suspend fun findExpirations(peopleRequestId: PeopleRequestId): Either<DomainError, List<PeopleRequestExpiration>>
    suspend fun update(peopleRequestId: PeopleRequestId, requestState: RequestState): Either<DomainError, Unit>
    suspend fun update(billReturnId: BillReturnId, updateBillReturn: UpdateBillReturn): Either<DomainError, Unit>
    suspend fun update(peopleRequestId: List<PeopleRequestId>, requestState: RequestState): Either<DomainError, Unit>
    suspend fun update(billReturnId: BillReturnId, attachmentId: AttachmentId, state: AttachmentState): Either<DomainError, Unit>
    suspend fun update(billReturnId: BillReturnId, attachmentIds: AttachmentIds, state: AttachmentState): Either<DomainError, Unit>
    suspend fun deleteSupplies(billReturnId: BillReturnId): Either<DomainError, Unit>

}

class DefaultPeopleRequestRepo(private val db: Database): PeopleRequestRepo {
    override suspend fun save(billReturnReq: BillReturnRequest): Either<DomainError, BillReturnResponse> =
        transact(db) {
            val pRId = PeopleRequests.insertQuery(billReturnReq.peopleRequest, RequestType.BILL_RETURN_REQUEST)
            val peopleRequestId = PeopleRequestId(pRId)
            val bRId = BillReturns.insertQuery(
                billReturnReq.billReturn.copy(peopleRequestId = peopleRequestId)
            )
            val billReturnId = BillReturnId(bRId)
            BillReturnResponse(peopleRequestId, billReturnId)
        }

    override suspend fun save(
        peopleRequestId: PeopleRequestId,
        peopleRequestExpiration: PeopleRequestExpiration
    ): Either<DomainError, Unit> = transact(db) {
        PeopleRequestExpirations.insertQuery(peopleRequestId, peopleRequestExpiration)
    }.void()

    override suspend fun saveSupplies(
        billReturnId: BillReturnId,
        supplies: List<SupplyDetail>
    ): Either<DomainError, Unit> =
        transact(db) { BillReturnsSupplies.insertBatchQuery(billReturnId, supplies) }

    override suspend fun update(
        peopleRequestId: PeopleRequestId,
        requestExpiration: RequestExpiration,
        responseExpiration: ResponseExpiration
    ): Either<DomainError, Unit> =
        transact(db) {
            PeopleRequestExpirations.updateQuery(peopleRequestId, requestExpiration, responseExpiration)
        }

    override suspend fun saveAttachments(billReturnId: BillReturnId, newAttachments: List<NewAttachment>): Either<DomainError, Unit> =
        newAttachments.traverse {
            transact(db) { BillReturnsAttachments.insertQuery(billReturnId, it) }.void()
        }.void()

    override suspend fun findAll(state: RequestState, requestType: RequestType, params: NewPaginationParams, userId: UserId): Either<DomainError, List<PeopleRequestBillReturn>> =
        transact(db) { PeopleRequests.selectAllBy(state, requestType, params, userId).map { it.toPeopleRequestBillReturn() } }

    override suspend fun findAll(
        state: RequestState,
        requestType: RequestType,
        params: NewPaginationParams
    ): Either<DomainError, List<PeopleRequestBillReturn>> =
        transact(db) { PeopleRequests.selectAllBy(state, requestType, params).map { it.toPeopleRequestBillReturn() } }

    override suspend fun findBy(peopleRequestId: PeopleRequestId, requestType: RequestType): Either<DomainError, PeopleRequestBillReturn?> =
        transact(db) { PeopleRequests.selectBy(peopleRequestId, requestType)?.toPeopleRequestBillReturn() }

    override suspend fun findAllBy(
        peopleId: NewPeopleId,
        requestType: RequestType
    ): Either<DomainError, List<PeopleRequestBillReturn>> =
        transact(db) {
            PeopleRequests.selectAllBy(peopleId, requestType).map { it.toPeopleRequestBillReturn() }
        }

    override suspend fun findSuppliesBy(billReturnId: BillReturnId): Either<DomainError, List<SupplyDetailShow>> =
        transact(db) {
            BillReturnsSupplies.selectQuery(billReturnId).map { it.toSupplyDetailShow() }
        }
    override suspend fun findProductBy(billReturnId: BillReturnId): Either<DomainError, ProductShow?> =
        transact(db) {
            BillReturns.selectProduct(billReturnId)?.toProductShow()
        }

    override suspend fun findIdBy(billReturnId: BillReturnId): Either<DomainError, PeopleRequestId?> =
        transact(db) { BillReturns.selectPeopleRequestIdQuery(billReturnId) }

    override suspend fun findAttachmentsBy(billReturnId: BillReturnId): Either<DomainError, List<NonContentAttachment>> =
        transact(db) {
            BillReturnsAttachments.selectQuery(billReturnId).map { it.toNonContentAttachment() }
        }
    override suspend fun findExpirations(peopleRequestId: PeopleRequestId): Either<DomainError, List<PeopleRequestExpiration>> =
        transact(db) {
            PeopleRequestExpirations.selectQuery(peopleRequestId).map { it.toPeopleRequestExpiration() }
        }
    override suspend fun update(
        peopleRequestId: PeopleRequestId,
        requestState: RequestState
    ): Either<DomainError, Unit> = transact(db) {
        PeopleRequests.updateStateQuery(peopleRequestId, requestState)
        Unit
    }

    override suspend fun update(
        billReturnId: BillReturnId,
        updateBillReturn: UpdateBillReturn
    ): Either<DomainError, Unit> =
        transact(db) {
            BillReturns.updateQuery(billReturnId, updateBillReturn)
        }.void()

    override suspend fun update(
        peopleRequestId: List<PeopleRequestId>,
        requestState: RequestState
    ): Either<DomainError, Unit> =
        transact(db) {
            PeopleRequests
                .updateStateBatchQuery(peopleRequestId, requestState)
        }

    override suspend fun update(billReturnId: BillReturnId, attachmentId: AttachmentId, state: AttachmentState): Either<DomainError, Unit> =
        transact(db) {
            BillReturnsAttachments.updateStateQuery(billReturnId, state, attachmentId)
        }.void()

    override suspend fun update(
        billReturnId: BillReturnId,
        attachmentIds: AttachmentIds,
        state: AttachmentState
    ): Either<DomainError, Unit> =
        transact(db) {
            BillReturnsAttachments
                .updateStateBatchQuery(billReturnId, state, attachmentIds.ids)
        }

    override suspend fun deleteSupplies(billReturnId: BillReturnId): Either<DomainError, Unit> =
        transact(db) { BillReturnsSupplies.deleteQuery(billReturnId) }
}
