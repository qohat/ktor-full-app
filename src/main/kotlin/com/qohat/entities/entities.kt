package com.qohat.entities

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.postgresql.util.PGobject

class PGEnum<T : Enum<T>>(enumTypeName: String, enumValue: T?) : PGobject() {
    init {
        value = enumValue?.name
        type = enumTypeName
    }
}

class RunSqlStatement(table: Table, private val statement: String? = null) : InsertStatement<Number>(table) {
    override fun prepareSQL(transaction: Transaction): String {
        return statement?.let {
            super.prepareSQL(transaction) + statement
        } ?: super.prepareSQL(transaction)
    }
}

fun <T : Table> T.insertWithRawStatement(body: T.(RunSqlStatement) -> Unit, statement: () -> String): InsertStatement<Number> =
    RunSqlStatement(this, statement.invoke()).apply {
        body(this)
        TransactionManager.current().exec(this)
    }