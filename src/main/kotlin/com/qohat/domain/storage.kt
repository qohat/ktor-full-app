package com.qohat.domain

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class StorageId(val value: Long) {
    companion object {
        fun unApply(arg: String): StorageId? =
            arg.toLongOrNull()?.let { StorageId(it) }
    }
}

@JvmInline
@Serializable
value class RegisterNumber(val value: String)

@Serializable
data class Storage(
    val id: StorageId?,
    val name: Name,
    val address: Address?,
    val document: Document,
    val email: Email?,
    val phone: Phone?,
    val department: ValueList,
    val city: ValueList,
    val registerNumber: RegisterNumber,
    val activityRegistered: ValueList
)

@Serializable
data class StorageShow(
    val id: StorageId,
    val name: Name,
    val address: Address?,
    val document: Document,
    val department: Name,
    val city: Name,
    val registerNumber: RegisterNumber?,
    val activityRegistered: Name,
    val email: Email?,
    val phone: Phone?,
    val active: Active,
    val createdAt: CreatedAt
)