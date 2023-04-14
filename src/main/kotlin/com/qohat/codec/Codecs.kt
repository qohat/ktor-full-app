package com.qohat.codec

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class Codecs {
    object LocalDateTimeSerializer: KSerializer<LocalDateTime> {

        override fun deserialize(decoder: Decoder): LocalDateTime =
            LocalDateTime.parse(decoder.decodeString())

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: LocalDateTime) =
            encoder.encodeString(value.toString())
    }

    object LocalDateSerializer: KSerializer<LocalDate> {

        override fun deserialize(decoder: Decoder): LocalDate =
            LocalDate.parse(decoder.decodeString(), DateTimeFormatter.ISO_DATE)

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: LocalDate) =
            encoder.encodeString(value.toString())
    }

    object OptionSerializer: KSerializer<Option<String>> {
        @OptIn(ExperimentalSerializationApi::class)
        override fun deserialize(decoder: Decoder): Option<String> {
            val isNotNull = decoder.decodeNotNullMark()
            if (isNotNull) {
                val stringDecoded = decoder.decodeString()
                return if(stringDecoded.isNotEmpty()) Some(stringDecoded) else None
            } else return None
        }

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Option<String>", PrimitiveKind.STRING)

        @OptIn(ExperimentalSerializationApi::class)
        override fun serialize(encoder: Encoder, value: Option<String>) {
            when(value) {
                is None -> encoder.encodeNull()
                is Some -> encoder.encodeString(value.value)
            }
        }

    }

    object BigDecimalSerializer: KSerializer<BigDecimal> {

        override fun deserialize(decoder: Decoder): BigDecimal =
            BigDecimal(decoder.decodeString())

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: BigDecimal) =
            encoder.encodeString(value.toString())
    }

    object UUIDSerializer: KSerializer<UUID> {

        override fun deserialize(decoder: Decoder): UUID =
            UUID.fromString(decoder.decodeString())

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: UUID) =
        encoder.encodeString(value.toString())
    }
}