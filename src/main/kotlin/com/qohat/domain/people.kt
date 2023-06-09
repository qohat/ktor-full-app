package com.qohat.domain

import arrow.core.Either
import com.qohat.codec.Codecs
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@JvmInline
@Serializable
value class NewPeopleId(
    @Serializable(Codecs.UUIDSerializer::class)
    val value: UUID
) {
    companion object {
        fun unApply(arg: String): NewPeopleId? =
            Either.catch { UUID.fromString(arg) }
                .map { NewPeopleId(it) }
                .orNull()
    }
}
@JvmInline
@Serializable
value class Name(val value: String)
@JvmInline
@Serializable
value class LastName(val value: String)
@JvmInline
@Serializable
value class Document(val value: String)
@JvmInline
@Serializable
value class IssueDocumentDate(
    @Serializable(Codecs.LocalDateSerializer::class)
    val value: LocalDate
)
@JvmInline
@Serializable
value class BirthDay(
    @Serializable(Codecs.LocalDateSerializer::class)
    val value: LocalDate
)
@JvmInline
@Serializable
value class Phone(val value: String)
@JvmInline
@Serializable
value class CellPhone(val value: String)
@JvmInline
@Serializable
value class Email(val value: String)
@JvmInline
@Serializable
value class ArmedConflictVictim(val value: Boolean)
@JvmInline
@Serializable
value class Displaced(val value: Boolean)
@JvmInline
@Serializable
value class TermsAcceptance(val value: Boolean)
@JvmInline
@Serializable
value class Address(val value: String)
@JvmInline
@Serializable
value class PeopleAccountNumber(val value: String)
@JvmInline
@Serializable
value class Lane(val value: String)
@JvmInline
@Serializable
value class Hectares(@Serializable(Codecs.BigDecimalSerializer::class) val value: BigDecimal)
@JvmInline
@Serializable
value class Nit(val value: String)
@JvmInline
@Serializable
value class BelongsOrganization(val value: Boolean)
@JvmInline
@Serializable
value class SingleMother(val value: Boolean)
@Serializable
data class PropertyInformation(
    val address: Address,
    val name: Name,
    val department: ValueList,
    val city: ValueList,
    val lane: Lane?,
    val hectares: Hectares
)

@Serializable
data class OrganizationBelongingInfo(
    val type: ValueList,
    val name: Name,
    val nit: Nit?
)

@Serializable
data class PaymentInformation(
    val paymentType: ValueList,
    val branch: ValueList?,
    val bank: ValueList?,
    val accountType: ValueList?,
    val accountNumber: PeopleAccountNumber?,
)

@Serializable
data class NewPeople(
    val id: NewPeopleId?,
    val name: Name,
    val lastName: LastName,
    val documentType: ValueList,
    val document: Document,
    val issueDocumentDate: IssueDocumentDate?,
    val birthday: BirthDay,
    val address: Address?,
    val sex: ValueList,
    val gender: ValueList,
    val phone: Phone?,
    val cellPhone: CellPhone?,
    val email: Email,
    val populationGroup: ValueList,
    val ethnicGroup: ValueList,
    val disability: ValueList,
    val armedConflictVictim: ArmedConflictVictim?,
    val displaced: Displaced?,
    val propertyInformation: PropertyInformation?,
    val belongsOrganization: BelongsOrganization?,
    val organizationBelongingInfo: OrganizationBelongingInfo?,
    val paymentInformation: PaymentInformation?,
    val termsAcceptance: TermsAcceptance,
    val singleMother: SingleMother?,
    val createdBy: CreatedById?
)