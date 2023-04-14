package com.qohat.domain

import com.qohat.codec.Codecs
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@JvmInline
@Serializable
value class Observation(val value: String)

@JvmInline
@Serializable
value class NewEventId(val value: Long)

@Serializable
data class AttachmentsValidationEvent(
    val id: NewEventId?,
    val attachmentId: AttachmentId,
    val userId: NewUserId,
    val observation: Observation?,
    val reason: ValueList,
    val state: AttachmentState
)

@Serializable
data class AttachmentsValidationEventShow(
    val user: Name,
    val observation: Observation?,
    val createdAt: CreatedAt,
    val reason: Name,
    val fileTypeName: Name,
    val state: AttachmentState
)

@Serializable
data class AttachmentsValidationEvents(
    val value: List<AttachmentsValidationEvent>
)


@Serializable
data class ValidationCompaniesAttachmentsEvent(
    val companyAttachment: CompanyAttachment,
    val userId: String,
    val userName: String,
    val observation: String,
    val state: AttachmentState,
    val reason: ValueList,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime
)

@Serializable
data class ValidationPeopleAttachmentsEvent(
    val peopleAttachment: PeopleAttachment,
    val userId: String,
    val userName: String,
    val observation: String,
    val state: AttachmentState,
    val reason: ValueList,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime
)

@Serializable
data class EventId(val value: String)