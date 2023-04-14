package com.qohat.domain

import kotlinx.serialization.Serializable

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