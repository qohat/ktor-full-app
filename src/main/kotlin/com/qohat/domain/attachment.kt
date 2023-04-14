package com.qohat.domain

import arrow.core.Either
import com.qohat.codec.Codecs
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Attachment(
    val id: String?,
    val name: String,
    val path: String,
    val content: String,
    val fileId: ValueList
)

@JvmInline
@Serializable
value class AttachmentId(@Serializable(Codecs.UUIDSerializer::class) val value: UUID) {
    companion object {
        fun unApply(arg: String): AttachmentId? =
            Either.catch { UUID.fromString(arg) }
                .map { AttachmentId(it) }
                .orNull()
    }
}

@Serializable
data class AttachmentIds(val ids: List<AttachmentId>)

@JvmInline
@Serializable
value class AttachmentName(val value: String)

@JvmInline
@Serializable
value class AttachmentPath(val value: String)

@JvmInline
@Serializable
value class AttachmentContent(val value: String)
@Serializable
data class NewAttachment(
    val id: AttachmentId?,
    val name: AttachmentName,
    val path: AttachmentPath,
    val content: AttachmentContent,
    val fileTypeId: ValueList,
    val state: AttachmentState = AttachmentState.InReview
)

@Serializable
data class NonContentAttachment(
    val id: AttachmentId,
    val name: AttachmentName,
    val path: AttachmentPath,
    val fileTypeName: Name,
    val state: AttachmentState
)