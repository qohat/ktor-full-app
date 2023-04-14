package com.qohat.repo

import arrow.core.Either
import arrow.core.continuations.either
import com.qohat.domain.PeopleRequestId
import com.qohat.error.DomainError
import com.qohat.error.RepoTransactionError
import com.qohat.http.logger
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> transact(db: Database, transaction: suspend Transaction.() -> T): Either<DomainError, T> =
    Either.catch {
        newSuspendedTransaction(Dispatchers.IO, db) { transaction() }
    }
    .tapLeft { e -> logger.error("Failed transaction", e) }
    .mapLeft { RepoTransactionError("Failed Transaction in Repository") }