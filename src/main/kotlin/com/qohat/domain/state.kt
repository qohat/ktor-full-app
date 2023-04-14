package com.qohat.domain

enum class AttachmentState(val value: String) {
    InReview("in-review"),
    Approved("approved"),
    Rejected("rejected"),
    RequiresValidation("requires-validation");

    companion object {
        fun unApply(arg: String): AttachmentState? =
            listOf(
                InReview,
                Approved,
                Rejected,
                RequiresValidation
            ).find { it.value == arg }
    }
}

@kotlinx.serialization.Serializable
data class PaginationParams(val limit: Int, val offset: Long, val text: String)

@kotlinx.serialization.Serializable
data class NewPaginationParams(val pagination: Pagination, val text: Text?)

@JvmInline
@kotlinx.serialization.Serializable
value class Limit(val value: Int)

@JvmInline
@kotlinx.serialization.Serializable
value class Offset(val value: Long)

@JvmInline
@kotlinx.serialization.Serializable
value class Text(val value: String)

@kotlinx.serialization.Serializable
data class Pagination(val limit: Limit, val offset: Offset)



