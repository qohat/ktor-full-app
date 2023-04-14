package com.qohat.repo

import arrow.core.Either
import com.qohat.domain.NewPayment
import com.qohat.domain.NotPaidReport
import com.qohat.domain.requests.BillReturnId
import com.qohat.entities.BillReturnNotPaidReport
import com.qohat.entities.BillReturnPayments
import com.qohat.entities.toNewPayment
import com.qohat.error.DomainError
import org.jetbrains.exposed.sql.Database

interface PaymentRepo {
    suspend fun findBy(billReturnId: BillReturnId): Either<DomainError, NewPayment?>
    suspend fun save(payment: NewPayment): Either<DomainError, Unit>
    suspend fun save(report: NotPaidReport): Either<DomainError, Unit>
}

class DefaultPaymentRepo(private val db: Database): PaymentRepo {
    override suspend fun findBy(billReturnId: BillReturnId): Either<DomainError, NewPayment?> =
        transact(db) {
            BillReturnPayments.selectQuery(billReturnId)?.toNewPayment()
        }

    override suspend fun save(payment: NewPayment): Either<DomainError, Unit> =
        transact(db) {
            BillReturnPayments.insertQuery(payment)
        }.void()

    override suspend fun save(report: NotPaidReport): Either<DomainError, Unit> =
        transact(db) {
            BillReturnNotPaidReport.insertQuery(report)
        }.void()
}