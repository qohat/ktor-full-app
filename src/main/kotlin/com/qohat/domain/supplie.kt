package com.qohat.domain

import com.qohat.codec.Codecs
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@JvmInline
@Serializable
value class Price(@Serializable(Codecs.BigDecimalSerializer::class) val value: BigDecimal)

@JvmInline
@Serializable
value class SupplyId(val value: Long) {
    companion object {
        fun unApply(arg: String): SupplyId? =
            arg.toLongOrNull()?.let { SupplyId(it) }
    }
}


@Serializable
data class Supply(
    val id: SupplyId,
    val name: Name,
    val cropGroup: ValueList,
    val price: Price
)

@Serializable
data class SupplyShow(
    val id: SupplyId,
    val name: Name,
    val cropGroup: Name,
    val price: Price,
    val active: Active,
    val createdAt: CreatedAt
)