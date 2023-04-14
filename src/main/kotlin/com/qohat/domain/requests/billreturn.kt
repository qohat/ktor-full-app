package com.qohat.domain.requests

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import com.qohat.codec.Codecs
import com.qohat.domain.CreatedAt
import com.qohat.domain.LastName
import com.qohat.domain.Name
import com.qohat.domain.NewAttachment
import com.qohat.domain.NonContentAttachment
import com.qohat.domain.Observation
import com.qohat.domain.PeopleRequestId
import com.qohat.domain.ProductId
import com.qohat.domain.ProductShow
import com.qohat.domain.StorageId
import com.qohat.domain.SupplyDetail
import com.qohat.domain.SupplyDetailShow
import com.qohat.domain.UnitMeasurement
import com.qohat.domain.UserId
import com.qohat.domain.toKg
import com.qohat.entities.ConfigName
import com.qohat.entities.ConfigValue
import com.qohat.error.ConfigValueNotFound
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@JvmInline
@Serializable
value class BillReturnId(@Serializable(Codecs.UUIDSerializer::class) val value: UUID) {
    companion object {
        fun unApply(arg: String): BillReturnId? =
            Either.catch { UUID.fromString(arg) }
                .map { BillReturnId(it) }
                .orNull()
    }
}

@Serializable
data class BillReturn(
    val id: BillReturnId?,
    val peopleRequestId: PeopleRequestId,
    val storage: StorageId,
    val product: ProductId,
    val supplies: List<SupplyDetail>,
    val attachments: List<NewAttachment>
)

@Serializable
@JvmInline
value class BillReturnObservationId(val id: Long)

@Serializable
data class BillReturnObservation(
    val id: BillReturnObservationId?,
    val billReturnId: BillReturnId,
    val observation: Observation,
    val userId: UserId,
    val userName: Name?,
    val userLastName: LastName?,
    val createdAt: CreatedAt?
)

@Serializable
data class UpdateBillReturn(
    val storage: StorageId,
    val product: ProductId,
    val supplies: List<SupplyDetail>,
    val attachments: List<NewAttachment>
)

@JvmInline
@Serializable
value class Amount(@Serializable(Codecs.BigDecimalSerializer::class) val value: BigDecimal)

@Serializable
data class BillPaymentProspect(
    val amount: Amount,
    val prospectSubsidyValue: Amount,
    val subsidyValue: Amount
)

@Serializable
data class BillReturnDetails(
    val product: ProductShow,
    val supplies: List<SupplyDetailShow>,
    val attachments: List<NonContentAttachment>,
    val paymentProspect: BillPaymentProspect? = null
)

suspend fun BillReturnDetails.withPaymentProspect(configs: Map<ConfigName, ConfigValue>): Either<ConfigValueNotFound, BillReturnDetails> = either {
    val percentageConfigName = ConfigName("percentage_to_subsidize")
    val percentage = ensureNotNull(configs.get(percentageConfigName)) { ConfigValueNotFound(percentageConfigName.value) }
    val amount = supplies.map { it.value.value }.reduce { a, b -> a + b }
    val amountProspect = supplies.map {
        //Calculate the value per kg
        UnitMeasurement.unApply(it.measurementUnit.value)?.let { unit ->
            val quantityInKg = it.quantity.toKg(unit)
            val kgValue = it.value.value.divide(quantityInKg, 2, RoundingMode.HALF_UP)
            //Calculate the prospect based on the reference price
            val prospect = if(kgValue > it.supplyPrice.value) it.supplyPrice.value
            else kgValue
            //Calculate the prospect per quantity
            prospect * quantityInKg
        } ?: BigDecimal.ZERO
    }.reduce { a, b -> a + b }
    val prospectValue = amountProspect.multiply(percentage.value)
    val value = if(prospectValue > product.maximumToSubsidize.value) { product.maximumToSubsidize.value } else prospectValue
    val paymentProspect = BillPaymentProspect(Amount(amount), Amount(prospectValue), Amount(value))
    BillReturnDetails(product, supplies, attachments, paymentProspect)
}