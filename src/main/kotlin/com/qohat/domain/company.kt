package com.qohat.domain

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import com.qohat.codec.Codecs
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class CompanyId(val value: String)

@JvmInline
@Serializable
value class NewCompanyId(@Serializable(Codecs.UUIDSerializer::class) val value: UUID)
@Serializable
data class CompanyDocument(val value: String)
data class CompanyEmail(val value: String)

@Serializable
data class CompanyName(val value: String)

//TODO Create refined type
typealias AccountNumber = String

@Serializable
data class Company(
    val id: CompanyId,
    val name: String,
    val companyType: ValueList,
    val document: String,
    val address: String,
    val locality: ValueList,
    @Serializable(Codecs.OptionSerializer::class)
    val neighborhood: Option<String>,
    @Serializable(Codecs.OptionSerializer::class)
    val phone: Option<String>,
    @Serializable(Codecs.OptionSerializer::class)
    val cellPhone: Option<String>,
    val email: String,
    val employees: Int,
    val companySize: ValueList,
    @Serializable(Codecs.OptionSerializer::class)
    val economicActivity: Option<String>,
    val ccf: ValueList,
    @Serializable(Codecs.OptionSerializer::class)
    val legalRepresentative: Option<String>,
    @Serializable(Codecs.OptionSerializer::class)
    val legalRepresentativeDocument: Option<String>,
    @Serializable(Codecs.OptionSerializer::class)
    val postulationResponsible: Option<String>,
    @Serializable(Codecs.OptionSerializer::class)
    val postulationResponsiblePhone: Option<String>,
    @Serializable(Codecs.OptionSerializer::class)
    val postulationResponsiblePosition: Option<String>,
    @Serializable(Codecs.OptionSerializer::class)
    val postulationResponsibleEmail: Option<String>,
    val bank: ValueList,
    val accountBankType: ValueList,
    val accountNumber: AccountNumber, // Could be 000124343234 Whe should keep left zeros
    val active: Boolean,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val attachments: List<CompanyAttachment>,
    val createdBy: UserId,
    val women: Int = 0,
    val men: Int = 0
) {
    override fun toString(): String =
        "$document,$name,${companyType.name},$address,${locality.name},${neighborhood.getOrElse { "" }},${phone.getOrElse { "" }},${cellPhone.getOrElse { "" }},$email,$employees,${companySize.name},${economicActivity.getOrElse { "" }},${ccf.name},${legalRepresentative.getOrElse { "" }},${legalRepresentativeDocument.getOrElse { "" }},${postulationResponsible.getOrElse { "" }},${postulationResponsiblePosition.getOrElse { "" }},${postulationResponsiblePhone.getOrElse { "" }},${postulationResponsibleEmail.getOrElse { "" }},${bank.name},${accountBankType.name},$accountNumber"
}

fun Company.hasEnoughEmployees() =
    (this.women < ((this.women + this.men) * .6) && this.women + this.men == 500)
            || this.women + this.men == 600

fun Company.plusGender(people: People): Option<Company> =
    if(people.gender.name.contains(Gender.Masculino.toString())) {
        Some(this.copy(men = this.men + 1))
    } else if (people.gender.name.contains(Gender.Femenino.toString())) {
        Some(this.copy(women = this.women + 1))
    } else {
        None
    }

fun Company.minusGender(people: People): Option<Company> =
    if(people.gender.name.contains(Gender.Masculino.toString())) {
        Some(this.copy(men = this.men - 1))
    } else if (people.gender.name.contains(Gender.Femenino.toString())) {
        Some(this.copy(women = this.women - 1))
    } else {
        None
    }

fun Company.discountGender(newPeople: People, lastPeople: People): Option<Company> =
    if(lastPeople.gender.name != newPeople.gender.name &&
        newPeople.gender.name.contains(Gender.Masculino.toString())) {
        Some(this.copy(men = this.men + 1, women = this.women - 1))
    } else if (lastPeople.gender.name != newPeople.gender.name &&
        newPeople.gender.name.contains(Gender.Femenino.toString())) {
        Some(this.copy(men = this.men - 1, women = this.women + 1))
    } else {
        Some(this)
    }

@Serializable
data class AttachFileToCompany(val companyId: CompanyId, val attachment: Attachment)

@Serializable
data class CompanyAttachment(
    val id: String,
    val companyId: String,
    val name: String,
    val path: String,
    val state: AttachmentState,
    val active: Boolean,
    val fileType: ValueList,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime
)