package com.qohat.domain

import com.qohat.codec.Codecs
import com.qohat.domain.requests.BillReturnId
import kotlinx.serialization.Serializable
import java.time.LocalDate

sealed interface ImportRecord {
    val key: String
}
data class InvalidImportRecord(
    val message: String,
    override val key: String): ImportRecord

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
value class NotPaidReason(val value: Int) {
    init {
        require(value > 0)
    }
}