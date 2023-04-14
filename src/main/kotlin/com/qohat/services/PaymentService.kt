package com.qohat.services

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import arrow.core.computations.either
import arrow.core.singleOrNone
import com.qohat.config.BusinessParams
import com.qohat.domain.AttachmentState
import com.qohat.domain.ListKey
import com.qohat.domain.Payment
import com.qohat.domain.PaymentId
import com.qohat.domain.PaymentImportRecord
import com.qohat.domain.PaymentMonth
import com.qohat.domain.PaymentYear
import com.qohat.domain.PeopleCompanyId
import com.qohat.domain.findNext
import com.qohat.repo.PaymentRepo
import com.qohat.repo.PeopleCompanyRepo
import java.util.UUID

interface PaymentService {
    suspend fun findAllBy(peopleCompanyId: PeopleCompanyId): List<Payment>
    suspend fun save(peopleCompanyId: PeopleCompanyId, paymentMonth: PaymentMonth, paymentYear: PaymentYear, payment: Payment): PaymentId
    suspend fun save(paymentImportRecord: PaymentImportRecord): Either<String, Pair<PeopleCompanyId, PaymentId>>

    suspend fun updateCurrentMonth(peopleCompanyId: PeopleCompanyId): Either<String, Unit>

    suspend fun toPaid(peopleCompanyId: PeopleCompanyId): Unit

}

class DefaultPaymentService(private val paymentRepo: PaymentRepo,
                            private val peopleCompanyRepo: PeopleCompanyRepo,
                            private val valuesService: ValuesService,
                            private val businessParams: BusinessParams,
                            private val listService: ListService
): PaymentService {
    override suspend fun findAllBy(peopleCompanyId: PeopleCompanyId): List<Payment> =
        paymentRepo.findAllBy(peopleCompanyId)

    override suspend fun save(peopleCompanyId: PeopleCompanyId, paymentMonth: PaymentMonth, paymentYear: PaymentYear, payment: Payment): PaymentId =
        when(val dbPayment = paymentRepo.findBy(peopleCompanyId, paymentMonth, paymentYear)) {
          is Some -> dbPayment.value.id
          is None -> PaymentId(paymentRepo.save(payment).toString())
        }

    override suspend fun save(paymentImportRecord: PaymentImportRecord): Either<String, Pair<PeopleCompanyId, PaymentId>> =
        when(val peopleCompanyForPayment = peopleCompanyRepo.findBy(paymentImportRecord)) {
            is None -> Either.Left("No hay una relación empresa(${paymentImportRecord.companyDoc})/joven(${paymentImportRecord.peopleDoc})" +
                    "/mes(${paymentImportRecord.month.value})/año(${paymentImportRecord.year.value}) válida o el pago ya fue registrado.")
            is Some ->
                if (peopleCompanyForPayment.value.paymentsCount < businessParams.maxAllowedPayments) {
                    val payment = Payment.from(paymentImportRecord, peopleCompanyForPayment.value, valuesService)
                    val paymentId = save(peopleCompanyForPayment.value.id, paymentImportRecord.month, paymentImportRecord.year, payment)
                        .also {
                            if (peopleCompanyForPayment.value.paymentsCount == businessParams.maxAllowedPayments - 1)
                                peopleCompanyRepo.update(UUID.fromString(peopleCompanyForPayment.value.id.value), AttachmentState.Completed)
                        }
                    Either.Right(Pair(peopleCompanyForPayment.value.id, paymentId))
                } else {
                    Either.Left("Ya has registrado el número máximo de pagos permitidos para la relacion empresa(${paymentImportRecord.companyDoc})/joven(${paymentImportRecord.peopleDoc})")
                }
        }

    override suspend fun toPaid(peopleCompanyId: PeopleCompanyId) {
        peopleCompanyRepo.update(UUID.fromString(peopleCompanyId.value), AttachmentState.Paid)
    }

    override suspend fun updateCurrentMonth(peopleCompanyId: PeopleCompanyId): Either<String, Unit> = either {
        val pc = peopleCompanyRepo.findBy(UUID.fromString(peopleCompanyId.value))
            .toEither { "Error updating current moth, PeopleCompany doesn't exists" }
            .bind()

        val valueList = listService.findBy(ListKey(pc.currentMonthApplied.list))
            .filter { it.name == pc.currentMonthApplied.findNext() }
            .singleOrNone()
            .toEither { "Error updating current moth, next month doesn't exists" }
            .bind()

        peopleCompanyRepo.update(UUID.fromString(pc.id.value), pc.copy(currentMonthApplied = valueList))
    }

}