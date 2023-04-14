package com.qohat.repo

import arrow.core.Either
import arrow.core.Option
import arrow.core.singleOrNone
import com.qohat.domain.NewPayment
import com.qohat.domain.NotPaidReport
import com.qohat.domain.Payment
import com.qohat.domain.PaymentDate
import com.qohat.domain.PaymentId
import com.qohat.domain.PaymentMonth
import com.qohat.domain.PaymentNumber
import com.qohat.domain.PaymentYear
import com.qohat.domain.PeopleCompanyId
import com.qohat.domain.requests.BillReturnId
import com.qohat.entities.BillReturnNotPaidReport
import com.qohat.entities.BillReturnPayments
import com.qohat.entities.Payments
import com.qohat.entities.toNewPayment
import com.qohat.error.DomainError
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

interface PaymentRepo {
    suspend fun findAllBy(peopleCompanyId: PeopleCompanyId): List<Payment>
    suspend fun findBy(peopleCompanyId: PeopleCompanyId, paymentMonth: PaymentMonth, paymentYear: PaymentYear): Option<Payment>
    suspend fun save(payment: Payment): UUID
    suspend fun findBy(billReturnId: BillReturnId): Either<DomainError, NewPayment?>
    suspend fun save(payment: NewPayment): Either<DomainError, Unit>
    suspend fun save(report: NotPaidReport): Either<DomainError, Unit>
}

class DefaultPaymentRepo(private val db: Database): PaymentRepo {
    override suspend fun findAllBy(peopleCompanyId: PeopleCompanyId): List<Payment> =
        newSuspendedTransaction(Dispatchers.IO) {
            Payments
                .select { Payments.people_companies_id.eq(UUID.fromString(peopleCompanyId.value)) }
                .map { toPayment(it) }
        }

    override suspend fun findBy(peopleCompanyId: PeopleCompanyId, paymentMonth: PaymentMonth, paymentYear: PaymentYear): Option<Payment> =
        newSuspendedTransaction(Dispatchers.IO) {
            Payments
                .select {
                    Payments.people_companies_id.eq(UUID.fromString(peopleCompanyId.value)) and
                            Payments.month_applied.eq(paymentMonth.value) and
                            Payments.year_applied.eq(paymentYear.value)
                }
                .singleOrNone()
                .map { toPayment(it) }
        }

    override suspend fun findBy(billReturnId: BillReturnId): Either<DomainError, NewPayment?> =
        transact(db) {
            BillReturnPayments.selectQuery(billReturnId)?.toNewPayment()
        }

    override suspend fun save(payment: Payment): UUID =
        newSuspendedTransaction(Dispatchers.IO) {
            Payments.insert {
                it[id] = UUID.randomUUID()
                it[people_companies_id] = UUID.fromString(payment.peopleCompanyId.value)
                it[base_income] = payment.baseIncoming
                it[value] = payment.value
                it[month_applied] = payment.monthApplied.value
                it[year_applied] = payment.yearApplied.value
                it[p_date] = payment.date?.value
                it[p_number] = payment.number?.value
                it[created_at] = payment.createdAt
            } get Payments.id
        }

    override suspend fun save(payment: NewPayment): Either<DomainError, Unit> =
        transact(db) {
            BillReturnPayments.insertQuery(payment)
        }.void()

    override suspend fun save(report: NotPaidReport): Either<DomainError, Unit> =
        transact(db) {
            BillReturnNotPaidReport.insertQuery(report)
        }.void()


    private fun toPayment(row: ResultRow): Payment =
        Payment(
            id = PaymentId(row[Payments.id].toString()),
            peopleCompanyId = PeopleCompanyId(row[Payments.people_companies_id].toString()),
            baseIncoming = row[Payments.base_income],
            value = row[Payments.value],
            monthApplied = PaymentMonth(row[Payments.month_applied]),
            yearApplied = PaymentYear(row[Payments.year_applied]),
            date = row[Payments.p_date]?.let { PaymentDate(it) },
            number = row[Payments.p_number]?.let { PaymentNumber(it) },
            createdAt = row[Payments.created_at]
        )
}