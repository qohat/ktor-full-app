package com.qohat.domain

import com.qohat.codec.Codecs
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

@Serializable
data class PeopleWithPaymentCount(
    val onePayment: Int,
    val twoPayments: Int,
    val threePayments: Int
)

@Serializable
data class CompanyAmountDrawn(
    val name: String,
    @Serializable(Codecs.BigDecimalSerializer::class)
    val total: BigDecimal
) {
    override fun toString(): String =
        "${this.name},${this.total}"
}

@Serializable
data class PeopleAmountDrawn(
    val fullName: String,
    val document: String,
    @Serializable(Codecs.BigDecimalSerializer::class)
    val total: BigDecimal
) {
    override fun toString(): String =
        "${this.fullName},${this.document},${this.total}"
}

@Serializable
data class PeopleRelatedToCompany(
    val peopleName: String,
    val document: String,
    val companyName: String
){
    override fun toString(): String =
        "${this.peopleName},${this.document},${this.companyName}"
}

@Serializable
data class UserDoingProcess(
    val createdBy: String,
    val ccf: String,
    val assignedTo: String,
    val peopleName: String,
    val document: String,
    val companyName: String
){
    override fun toString(): String =
        "${this.createdBy},${this.ccf},${this.assignedTo},${this.peopleName},${this.document},${this.companyName}"
}

@Serializable
data class PaymentMade(
    val paymentsMade: Int
)

@Serializable
data class PaymentApprovals(
    val nit: String,
    val companyName: String,
    val docType: String,
    val document: String,
    val name: String,
    val lastName: String,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val approvalDate: LocalDateTime,
    val arlLevel: String,
    @Serializable(Codecs.BigDecimalSerializer::class)
    val value: BigDecimal,
    val paymentNumber: String,
    val monthApplied: String,
){
    override fun toString(): String =
        "$nit,$companyName,$docType,$document,$name,$lastName,$approvalDate,$arlLevel,$value,$paymentNumber,$monthApplied"
}

@Serializable
data class PaymentCommission(
    val nit: String,
    val companyName: String,
    val docType: String,
    val document: String,
    val name: String,
    val lastName: String,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val paymentDate: LocalDateTime,
    val arlLevel: String,
    @Serializable(Codecs.BigDecimalSerializer::class)
    val value: BigDecimal,
    val paymentNumber: String,
    val monthApplied: String,
){
    override fun toString(): String =
        "$nit,$companyName,$docType,$document,$name,$lastName,$paymentDate,$arlLevel,$value,$paymentNumber,$monthApplied"
}