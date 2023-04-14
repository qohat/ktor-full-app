package com.qohat.domain

import java.util.Locale

enum class AttachmentState(val value: String) {
    InReview("in-review"),
    Approved("approved"),
    Rejected("rejected"),
    RequiresValidation("requires-validation"),
    @Deprecated("Not use that state for files anymore")
    NonPaid("28vnerueg9wr9bur4g9nu"),
    @Deprecated("Not use that state for files anymore")
    Completed("28vnerueg9wr9bur4g9nssu"),
    @Deprecated("Not use that state for files anymore")
    NonState("28vn334eg9wr9bur4g9nu"),
    @Deprecated("Not use that state for files anymore")
    Paid("28v4rrueg9wr9bur4g9nu");

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


fun String.capitalizeText(): String {
    val capitalized = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    return if (capitalized.isNotEmpty()) capitalized[0] + capitalized.substring(1).lowercase()
        else capitalized
}



