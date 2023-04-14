package com.qohat.domain

import kotlinx.serialization.Serializable

@Serializable
data class ValueList(val id: Int, val name: String, val list: String, val active: Boolean)

data class ListKey(val value: String)

fun ValueList.findNext(): String {
    val nextMap = mapOf(
        "Jul-21" to "Ago-21",
        "Ago-21" to "Sep-21",
        "Sep-21" to "Oct-21",
        "Oct-21" to "Nov-21",
        "Nov-21" to "Dic-21",
        "Dic-21" to "Ene-22",
        "Ene-22" to "Feb-22",
        "Feb-22" to "Mar-22",
        "Mar-22" to "Abr-22",
        "Abr-22" to "May-22",
        "May-22" to "Jun-22",
        "Jun-22" to "Jul-22",
        "Jul-22" to "Ago-22",
        "Ago-22" to "Sep-22",
        "Sep-22" to "Oct-22",
        "Oct-22" to "Nov-22",
        "Nov-22" to "Dic-22",
        "Dic-22" to "None",
    )
    return nextMap.getOrDefault(this.name, this.name)
}