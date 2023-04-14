package com.qohat.domain

import arrow.core.Either
import com.qohat.codec.Codecs
import com.qohat.domain.requests.BillReturn
import com.qohat.domain.requests.BillReturnId
import com.qohat.entities.Config
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

@JvmInline
@Serializable
value class PeopleRequestId(@Serializable(Codecs.UUIDSerializer::class) val value: UUID) {
    companion object {
        fun unApply(arg: String): PeopleRequestId? =
            Either.catch { UUID.fromString(arg) }
                .map { PeopleRequestId(it) }
                .orNull()
    }
}

enum class RequestType(val value: String) {
    BILL_RETURN_REQUEST("bill-return-request");
    companion object {
        fun unApply(arg: String): RequestType? =
            RequestType.values().find { it.value == arg }
    }
}

enum class RequestState(val value: String) {
    Created("created"),
    InReview("in-review"),
    Approved("approved"),
    RequiresValidation("requires-validation"),
    NonPaid("non-paid"),
    Paid("paid"),
    Completed("completed"),
    Rejected("rejected"),
    Frozen("frozen"),
    Canceled("canceled");

    companion object {
        fun unApply(arg: String): RequestState? =
            RequestState.values().find { it.value == arg }

        fun from(attachmentState: AttachmentState): RequestState =
            when(attachmentState) {
                AttachmentState.Approved -> Approved
                AttachmentState.Rejected -> Rejected
                AttachmentState.InReview -> InReview
                AttachmentState.RequiresValidation -> RequiresValidation
                else -> InReview
            }
    }
}

fun RequestState.ableToChange(): Boolean =
    listOf(RequestState.InReview, RequestState.Rejected, RequestState.Frozen, RequestState.Canceled)
        .contains(this)

@Serializable
@JvmInline
value class PeopleRequestNumber(val value: Long)

@Serializable
data class PeopleRequest(
    val id: PeopleRequestId?,
    val number: PeopleRequestNumber?,
    val peopleId: NewPeopleId,
    val type: RequestType?,
    val state: RequestState?
)

@Serializable
@JvmInline
value class RequestExpiration(@Serializable(Codecs.LocalDateSerializer::class) val value: LocalDate)
@Serializable
@JvmInline
value class ResponseExpiration(@Serializable(Codecs.LocalDateSerializer::class) val value: LocalDate)
@Serializable
data class PeopleRequestExpiration(
    val requestExpiration: RequestExpiration,
    val responseExpiration: ResponseExpiration?
)

@Serializable
data class BillReturnRequest(
    val peopleRequest: PeopleRequest,
    val billReturn: BillReturn
)

fun BillReturnRequest.canBeCreated(config: Config, list: List<PeopleRequestBillReturn>, product: ProductShow): Boolean {
    return list.size < config.value.value.toInt()
            && list.all { it.product == product }
}

@Serializable
data class BillReturnResponse(
    val peopleRequestId: PeopleRequestId,
    val billReturnId: BillReturnId
)

@Serializable
data class PeopleRequestBillReturn(
    val id: PeopleRequestId,
    val number: PeopleRequestNumber?,
    val storage: StorageShow,
    val chain: Name,
    val cropGroup: Name,
    val product: ProductShow,
    val people: NewPeople,
    val billReturnId: BillReturnId,
    val type: RequestType,
    val state: RequestState,
    val expirations: List<PeopleRequestExpiration>,
    val createdAt: CreatedAt
)