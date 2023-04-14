package com.qohat.repo

import arrow.core.Option
import arrow.core.singleOrNone
import com.qohat.domain.ListKey
import com.qohat.domain.ValueList
import com.qohat.entities.Lists
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

interface ListRepo {
    suspend fun findBy(key: ListKey): List<ValueList>
    suspend fun findBy(name: String, key: ListKey): Option<ValueList>
}

class DefaultListRepo(private val db: Database): ListRepo {
    override suspend fun findBy(key: ListKey): List<ValueList> =
        newSuspendedTransaction(Dispatchers.IO, db) {
            Lists
            .select { Lists.list.eq(key.value) and Lists.active.eq(true) }
            .map { toValueList(it) }
        }

    override suspend fun findBy(name: String, key: ListKey): Option<ValueList> =
        newSuspendedTransaction(Dispatchers.IO, db) {
            Lists
                .select { Lists.list.eq(key.value) and Lists.name.eq(name) and Lists.active.eq(true) }
                .singleOrNone()
                .map { toValueList(it) }
        }

    private fun toValueList(row: ResultRow): ValueList =
        ValueList(
            id = row[Lists.id],
            name = row[Lists.name],
            list = row[Lists.list],
            active = row[Lists.active]
        )
}