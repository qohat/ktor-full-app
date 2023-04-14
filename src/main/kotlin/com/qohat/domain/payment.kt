package com.qohat.domain

import com.qohat.codec.Codecs
import com.qohat.domain.requests.Amount
import com.qohat.domain.requests.BillReturnId
import com.qohat.services.ValuesService
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface ImportRecord {
    val key: String
}
data class PaymentImportRecord(
    val companyDoc: String,
    val peopleDoc: String,
    val month: PaymentMonth,
    val year: PaymentYear,
    val date: PaymentDate,
    val number: PaymentNumber,
    val line: Int,
    override val key: String
): ImportRecord

data class InvalidImportRecord(val message: String,
                               override val key: String): ImportRecord

@Serializable
data class PaymentId(val value: String)

@Serializable
data class Payment(
    val id: PaymentId,
    val peopleCompanyId: PeopleCompanyId,
    @Serializable(Codecs.BigDecimalSerializer::class)
    val baseIncoming: BigDecimal,
    @Serializable(Codecs.BigDecimalSerializer::class)
    val value: BigDecimal,
    val monthApplied: PaymentMonth,
    val yearApplied: PaymentYear,
    val date: PaymentDate?,
    val number: PaymentNumber?,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(paymentImportRecord: PaymentImportRecord, peopleCompany: PeopleCompanyForPayment, valuesService: ValuesService) =
            Payment(
                id = PaymentId(""),
                peopleCompanyId = peopleCompany.id,
                baseIncoming = BigDecimal(valuesService.baseSalary.getValue("20${peopleCompany.currentMonthApplied.name.split("-")[1]}")),
                value = calculatePayment(valuesService, peopleCompany.arlLevel, peopleCompany.currentMonthApplied),
                monthApplied = paymentImportRecord.month,
                yearApplied = paymentImportRecord.year,
                date = paymentImportRecord.date,
                number = paymentImportRecord.number,
                createdAt = LocalDateTime.now()
            )

        fun calculatePayment(valuesService: ValuesService, arlLevel: ValueList, currentMonthApplied: ValueList): BigDecimal {
            val baseIncome =
                BigDecimal(valuesService.baseSalary.getValue("20${currentMonthApplied.name.split("-")[1]}"))
            val arlRisk = BigDecimal(valuesService.arlRisk.getValue(arlLevel.name))

            return BigDecimal(valuesService.deductiblePercentage.getValue("global")).multiply(
                baseIncome.add(
                    baseIncome.multiply(BigDecimal(valuesService.deductiblePercentage.getValue("health")))
                        .add(
                            baseIncome.multiply(BigDecimal(valuesService.deductiblePercentage.getValue("pension")))
                                .add(
                                    baseIncome.multiply(BigDecimal(valuesService.deductiblePercentage.getValue("ccf")))
                                        .add(baseIncome.multiply(arlRisk))
                                )
                        )
                ).add(
                    (baseIncome.multiply(BigDecimal(1.12))
                        .add(baseIncome.divide(BigDecimal(2), 2, BigDecimal.ROUND_HALF_EVEN).add(baseIncome)))
                        .divide(BigDecimal(12), 2, BigDecimal.ROUND_HALF_EVEN)
                    //(E5*(1+0,12)+E5+E5/2)/12
                ).add(baseIncome.multiply(BigDecimal(valuesService.deductiblePercentage.getValue("transport"))))
            ).setScale(0, BigDecimal.ROUND_HALF_DOWN)
        }
    }
}

fun List<Payment>.joinToString(): String {
    val defaultPayments = listOf(Pair("",""), Pair("",""), Pair("",""))
    return if (this.isEmpty()) defaultPayments.joinToString { "${it.first},${it.second}" }
    else {
        if(this.size >= 3) {
            this.joinToString { "${it.createdAt},${it.value}" }
        } else {
            val currentPayments = this.map { Pair("${it.createdAt}","${it.value}") }
            (currentPayments + defaultPayments.take(defaultPayments.size - currentPayments.size)).joinToString { "${it.first},${it.second}" }
        }
    }
}

fun List<Payment>.total(): BigDecimal =
    if (this.isEmpty()) BigDecimal.ZERO else this.map { it.value }.reduce { a, b -> a.add(b) }

@Serializable
data class ImportPayment(
    val content: String
)

@Serializable
data class NewPaymentRecord(
    val billReturnId: BillReturnId,
    val productId: ProductId,
    val document: Document,
    val date: PaymentDate,
    val line: Int,
    override val key: String
): ImportRecord {
    companion object {
        val header = "Producto,Documento,Fecha"
        val samples = listOf("1,1082985965,2023-03-01", "1,36543221,2023-03-02").joinToString("\n")
    }
}

@Serializable
data class NotPaidReportRecord(
    val billReturnId: BillReturnId,
    val productId: ProductId,
    val document: Document,
    val reasonId: NotPaidReason,
    val line: Int,
    override val key: String
): ImportRecord {
    companion object {
        val header = "Producto,Documento,Razón"
        val samples = listOf("1,1082985965,1", "1,36543221,2").joinToString("\n")
    }
}

data class ValidBillReturnRecord(
    val productId: ProductId,
    val document: Document,
    val billReturnId: BillReturnId,
)

@Serializable
data class NewPayment(
    val billReturnId: BillReturnId,
    val date: PaymentDate,
    val userId: UserId
)

@Serializable
data class NotPaidReport(
    val billReturnId: BillReturnId,
    val userId: UserId,
    val reasonId: NotPaidReason,
)

@Serializable
data class NewImportPayment(
    val content: FileContent
)

@JvmInline
@Serializable
value class FileContent(val value: String)

@JvmInline
@Serializable
value class PaymentYear(val value: Int) {
    init {
        require(value in 2021..2023)
    }
}

@JvmInline
@Serializable
value class PaymentMonth(val value: String) {
    init {
        require(value in listOf("Ene",
            "Feb",
            "Mar",
            "Abr",
            "May",
            "Jun",
            "Jul",
            "Ago",
            "Sep",
            "Oct",
            "Nov",
            "Dic")
        )
    }
}

@JvmInline
@Serializable
value class PaymentDate(
    @Serializable(Codecs.LocalDateSerializer::class) val value: LocalDate)

@JvmInline
@Serializable
value class PaymentNumber(val value: Int) {
    init {
        require(value > 0)
    }
}

@JvmInline
@Serializable
value class NotPaidReason(val value: Int) {
    init {
        require(value > 0)
    }
}