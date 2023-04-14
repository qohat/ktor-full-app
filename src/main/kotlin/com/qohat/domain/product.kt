package com.qohat.domain

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import com.qohat.codec.Codecs
import com.qohat.domain.requests.Amount
import com.qohat.error.DomainError
import com.qohat.error.InvalidMeasurementUnit
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@JvmInline
@Serializable
value class Percentage(@Serializable(Codecs.BigDecimalSerializer::class) val value: BigDecimal)

@JvmInline
@Serializable
value class MinimumToApply(val value: Int)

@JvmInline
@Serializable
value class ProductId(val value: Long) {
    companion object {
        fun unApply(arg: String): ProductId? =
            arg.toLongOrNull()?.let { ProductId(it) }
    }
}

@Serializable
data class Product(
    val name: Name,
    val cropGroup: ValueList,
    val percentage: Percentage,
    val maximumToSubsidize: Amount,
    val minimumToApply: MinimumToApply
)

@Serializable
data class ProductShow(
    val id: ProductId,
    val name: Name,
    val chain: Name,
    val cropGroup: Name,
    val percentage: Percentage,
    val maximumToSubsidize: Amount,
    val minimumToApply: MinimumToApply,
    val active: Active,
    val createdAt: CreatedAt
)

@JvmInline
@Serializable
value class Quantity(val value: Long)
fun Quantity.toKg(unit: UnitMeasurement): BigDecimal = when(unit) {
    UnitMeasurement.Kg -> BigDecimal.valueOf(value)
    UnitMeasurement.gr -> BigDecimal.valueOf(value).divide(BigDecimal.valueOf(1000),2, RoundingMode.HALF_UP)
    UnitMeasurement.Ton -> BigDecimal.valueOf(value) * BigDecimal.valueOf(1000)
}
@JvmInline
@Serializable
value class SupplyValue(@Serializable(Codecs.BigDecimalSerializer::class) val value: BigDecimal)

@JvmInline
@Serializable
value class BoughtDate(@Serializable(Codecs.LocalDateSerializer::class) val value: LocalDate)

@Serializable
data class SupplyDetail(
    val supply: SupplyId,
    val quantity: Quantity,
    val boughtDate: BoughtDate?,
    val presentation: ValueList?,
    val measurementUnit: ValueList,
    val value: SupplyValue
)

enum class UnitMeasurement(val value: String) {
    Kg("kg"),
    gr("gr"),
    Ton("Ton");

    companion object {
        fun unApply(arg: String): UnitMeasurement? =
            values().find { it.value == arg }
    }
}

suspend fun List<SupplyDetail>.validateUnit(): Either<DomainError, List<SupplyDetail>> = either {
    map {
        val unit = UnitMeasurement.unApply(it.measurementUnit.name)
        ensureNotNull(unit) { InvalidMeasurementUnit(it.measurementUnit.name) }
        it
    }
}

@Serializable
data class SupplyDetailShow(
    val supplyId: SupplyId,
    val supplyName: Name,
    val supplyPrice: Price,
    val quantity: Quantity,
    val boughtDate: BoughtDate?,
    val presentation: Name?,
    val measurementUnit: Name,
    val value: SupplyValue
)