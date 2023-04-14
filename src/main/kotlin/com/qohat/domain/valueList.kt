package com.qohat.domain

import kotlinx.serialization.Serializable

@Serializable
data class ValueList(val id: Int, val name: String, val list: String, val active: Boolean)

data class ListKey(val value: String)