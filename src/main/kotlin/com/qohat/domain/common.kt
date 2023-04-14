package com.qohat.domain

import com.qohat.codec.Codecs
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@JvmInline
@Serializable
value class Active(val value: Boolean)

@JvmInline
@Serializable
value class CreatedAt(@Serializable(Codecs.LocalDateTimeSerializer::class) val value: LocalDateTime)

@JvmInline
@Serializable
value class UpdatedAt(@Serializable(Codecs.LocalDateTimeSerializer::class) val value: LocalDateTime)