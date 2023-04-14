package com.qohat.domain

import com.qohat.codec.Codecs
import com.qohat.services.ValuesService
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class PeopleCompanyId(val value: String)

data class PeopleCompanyValidationId(val value: String)

@Serializable
data class PeopleCompany(
    val id: PeopleCompanyId,
    val peopleId: PeopleId,
    val companyId: CompanyId,
    val companyName: CompanyName,
    val contractType: ValueList,
    val duration: Int,
    @Serializable(Codecs.LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(Codecs.LocalDateSerializer::class)
    val endDate: LocalDate,
    val monthlyIncome: Int,
    val monthRequestApplied: ValueList,
    val currentMonthApplied: ValueList,
    val arlLevel: ValueList,
    val state: AttachmentState,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val attachments: List<PeopleAttachment>,
    val payments: List<Payment>,
    val createdBy: UserId,
    val assignedTo: UserId
)

@Serializable
data class PeopleCompanyLite(
    val id: PeopleCompanyId,
    val peopleName: PersonName,
    val peopleDocument: PersonDocument,
    val peopleAttachmentsCount: Long,
    val companyName: CompanyName,
    val companyDocument: CompanyDocument,
    val companyAttachmentsCount: Long,
    val paymentsCount: Int,
    val state: AttachmentState,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val active: Boolean
)

@Serializable
data class PeopleCompanyValidation(
    val id: PeopleCompanyId,
    val people: People,
    val company: Company,
    val contractType: ValueList,
    val duration: Int,
    @Serializable(Codecs.LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(Codecs.LocalDateSerializer::class)
    val endDate: LocalDate,
    val monthlyIncome: Int,
    val monthRequestApplied: ValueList,
    val currentMonthApplied: ValueList,
    val arlLevel: ValueList,
    val state: AttachmentState,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val attachments: List<PeopleAttachment>,
    val payments: List<Payment>
) {
    fun toString(valuesService: ValuesService): String =
        "${company.name},${company.document},${people.name},${people.lastName},${people.documentType.name},${people.document},${people.issueDocumentDate},${currentMonthApplied.name},${arlLevel.name},${monthlyIncome},${company.bank.name},${company.accountNumber},${company.accountBankType.name},${Payment.calculatePayment(valuesService, arlLevel, currentMonthApplied)}"

    fun toPayment(valuesService: ValuesService): String {
        val payment = Payment.calculatePayment(valuesService, arlLevel, currentMonthApplied)
        return "${company.document},${company.name},$payment,${company.bank.name},${company.accountNumber},${if(company.accountBankType.name == "Ahorros") "CH" else "CC" },${currentMonthApplied.name},$payment,${people.name},${people.lastName},${people.documentType.name},${people.document},${company.ccf.name}"
    }

    fun toPaidSpin(): String =
        "${company.document},${company.name},${people.documentType.name},${people.document},${people.name},${people.lastName},${arlLevel.name},$updatedAt,${payments.joinToString().trim()},${payments.total()}"
}

data class PeopleCompanyForPayment(
    val id: PeopleCompanyId,
    val currentMonthApplied: ValueList,
    val arlLevel: ValueList,
    val paymentsCount: Int
)
