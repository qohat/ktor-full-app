package com.qohat.domain

import com.qohat.codec.Codecs
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@JvmInline
@Serializable
value class Id(@Serializable(Codecs.UUIDSerializer::class) val value: UUID)

@JvmInline
@Serializable
value class Active(val value: Boolean)

@JvmInline
@Serializable
value class CreatedAt(@Serializable(Codecs.LocalDateTimeSerializer::class) val value: LocalDateTime)

@JvmInline
@Serializable
value class UpdatedAt(@Serializable(Codecs.LocalDateTimeSerializer::class) val value: LocalDateTime)