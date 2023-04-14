package com.qohat

import arrow.core.some
import com.qohat.domain.Active
import com.qohat.domain.Address
import com.qohat.domain.ArmedConflictVictim
import com.qohat.domain.AttachmentContent
import com.qohat.domain.AttachmentName
import com.qohat.domain.AttachmentPath
import com.qohat.domain.AttachmentState
import com.qohat.domain.BelongsOrganization
import com.qohat.domain.BillReturnRequest
import com.qohat.domain.BirthDay
import com.qohat.domain.BoughtDate
import com.qohat.domain.CcfId
import com.qohat.domain.CellPhone
import com.qohat.domain.Displaced
import com.qohat.domain.Document
import com.qohat.domain.Email
import com.qohat.domain.Hectares
import com.qohat.domain.IssueDocumentDate
import com.qohat.domain.Lane
import com.qohat.domain.LastName
import com.qohat.domain.MinimumToApply
import com.qohat.domain.Name
import com.qohat.domain.NewAttachment
import com.qohat.domain.NewPeople
import com.qohat.domain.NewPeopleId
import com.qohat.domain.Nit
import com.qohat.domain.Observation
import com.qohat.domain.OrganizationBelongingInfo
import com.qohat.domain.PaymentInformation
import com.qohat.domain.PeopleAccountNumber
import com.qohat.domain.PeopleRequest
import com.qohat.domain.PeopleRequestId
import com.qohat.domain.Percentage
import com.qohat.domain.PermissionChain
import com.qohat.domain.PermissionCode
import com.qohat.domain.Phone
import com.qohat.domain.Product
import com.qohat.domain.SupplyDetail
import com.qohat.domain.ProductId
import com.qohat.domain.SupplyValue
import com.qohat.domain.PropertyInformation
import com.qohat.domain.Quantity
import com.qohat.domain.Role
import com.qohat.domain.RoleId
import com.qohat.domain.RoleName
import com.qohat.domain.SingleMother
import com.qohat.domain.StorageId
import com.qohat.domain.SupplyId
import com.qohat.domain.TermsAcceptance
import com.qohat.domain.User
import com.qohat.domain.UserId
import com.qohat.domain.ValueList
import com.qohat.domain.requests.Amount
import com.qohat.domain.requests.BillReturn
import com.qohat.domain.requests.BillReturnId
import com.qohat.domain.requests.BillReturnObservation
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
}
object data {
    val depts = listOf(167, 168, 169, 173, 174, 175, 176)
    val sex = 63
    val doctype = 56
    val gender = 60
    val populationGroup = 66
    val crop = 3
    val presentation = 16
    val bank = 19
    val bank_branch = 41
    val accountType = 45
    val paymentType = 47
    val fileType = 52

    val newPeople: NewPeople = NewPeople(
        id = NewPeopleId(UUID.randomUUID()),
        name = Name("name"),
        lastName = LastName("lastName"),
        documentType = ValueList(doctype, "", "", true),
        document = Document(getRandomString()),
        issueDocumentDate = IssueDocumentDate(LocalDate.now()),
        birthday = BirthDay(LocalDate.now()),
        sex = ValueList(sex, "", "", true),
        gender = ValueList(gender, "", "", true),
        address = Address("Address"),
        phone = Phone("3049595"),
        cellPhone = CellPhone("3009494833"),
        email = Email("${getRandomString()}@mail.com"),
        populationGroup = ValueList(populationGroup, "", "", true),
        ethnicGroup = ValueList(populationGroup + 1, "", "", true),
        disability = ValueList(populationGroup + 2, "", "", true),
        armedConflictVictim = ArmedConflictVictim(true),
        displaced = Displaced(true),
        termsAcceptance = TermsAcceptance(true),
        propertyInformation = PropertyInformation(
            address = Address("address"),
            name = Name("name"),
            department = ValueList(1351, "", "", true),
            city = ValueList(1383, "", "", true),
            lane = Lane("it"),
            hectares = Hectares(BigDecimal.valueOf(2.3)),
        ),
        belongsOrganization = BelongsOrganization(true),
        organizationBelongingInfo = OrganizationBelongingInfo(
            type = ValueList(1, "", "", true),
            name = Name("organization"),
            nit = Nit("organization"),
        ),
        paymentInformation = PaymentInformation(
            paymentType = ValueList(paymentType, "", "", true),
            accountType = ValueList(accountType, "", "", true),
            accountNumber = PeopleAccountNumber("233333344"),
            bank = ValueList(bank, "", "", true),
            branch = ValueList(bank_branch, "", "", true),
        ),
        singleMother = SingleMother(false),
        createdBy = null
    )
    val userId = UserId(UUID.randomUUID())
    fun user(roleId: RoleId = RoleId(3)): User {

        return User(userId,
            "name",
            "lastname",
            "${getRandomString()}@mail.com",
            "${getRandomString()}@mail.com",
            "12345678",
            "12345678",
            ValueList(1, "", "1", true),
            "0${roleId.value}",
            "address".some(),
            Role(roleId, Name("name"), Active(true), PermissionChain("")),
            true,
            LocalDateTime.now(),
            LocalDateTime.now(),
            CcfId(1)
        )
    }

    val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhdWRpZW5jZSIsInJvbGUiOiIzLUZpZHUiLCJjY2YiOjEsImlzcyI6Imlzc3VlciIsImZ1bGxOYW1lIjoibmFtZSBsYXN0bmFtZSIsImlkIjoiNjM5ZDZhZTUtYzM4NC00ZjAxLWE3ZTYtYmRlOWE3YTQ3OWVkIiwic2NvcGVzIjoiUmRQUjpXdFBSOlVwdFBSOlJkUFJOZjpSZFBSVHA6UmRScHRzOkV4cHRScHRzOkltcHRScHRzIiwiZXhwIjozNjE2NzkxNTQ1MzUsImVtYWlsIjoiRHJleUJnWnJsRGxmQG1haWwuY29tIn0.2ful3hUzmsiDI333mJbMhZA5kdgFnZ5Brynxv7Q_czc"
    val peopleRequest = PeopleRequest(
        id = null,
        peopleId = NewPeopleId(UUID.randomUUID()),
        type = null,
        state = null,
        number = null
    )

    val billReturn = BillReturn(
        id = null,
        peopleRequestId = PeopleRequestId(UUID.randomUUID()),
        product = ProductId(18),
        storage = StorageId(4),
        attachments = listOf(
            NewAttachment(
                null,
                AttachmentName("facture.pdf"),
                AttachmentPath("/path"),
                AttachmentContent("content"),
                ValueList(fileType, "", "", true),
                AttachmentState.InReview
            ),
            NewAttachment(
                null,
                AttachmentName("document.pdf"),
                AttachmentPath("/path"),
                AttachmentContent("content"),
                ValueList(fileType, "", "", true),
                AttachmentState.InReview
            ),
            NewAttachment(
                null,
                AttachmentName("facture.pdf"),
                AttachmentPath("/path"),
                AttachmentContent("content"),
                ValueList(fileType, "", "", true),
                AttachmentState.InReview
            ),
            NewAttachment(
                null,
                AttachmentName("document.pdf"),
                AttachmentPath("/path"),
                AttachmentContent("content"),
                ValueList(fileType, "", "", true),
                AttachmentState.InReview
            ),
            NewAttachment(
                null,
                AttachmentName("facture.pdf"),
                AttachmentPath("/path"),
                AttachmentContent("content"),
                ValueList(fileType, "", "", true),
                AttachmentState.InReview
            ),
            NewAttachment(
                null,
                AttachmentName("document.pdf"),
                AttachmentPath("/path"),
                AttachmentContent("content"),
                ValueList(fileType, "", "", true),
                AttachmentState.InReview
            ),
        ),
        supplies = listOf(
            SupplyDetail(
                supply = SupplyId(1),
                quantity = Quantity(10),
                boughtDate = BoughtDate(LocalDate.now()),
                presentation = ValueList(1, "", "1", true),
                measurementUnit = ValueList(12, "kg", "1", true),
                value = SupplyValue(BigDecimal.valueOf(2000000))
            ),
            SupplyDetail(
                supply = SupplyId(2),
                quantity = Quantity(10),
                boughtDate = BoughtDate(LocalDate.now()),
                presentation = ValueList(1, "", "1", true),
                measurementUnit = ValueList(14, "Ton", "1", true),
                value = SupplyValue(BigDecimal.valueOf(6000000L))
            ),
            SupplyDetail(
                supply = SupplyId(3),
                quantity = Quantity(1000),
                boughtDate = BoughtDate(LocalDate.now()),
                presentation = ValueList(1, "", "1", true),
                measurementUnit = ValueList(15, "gr", "1", true),
                value = SupplyValue(BigDecimal.valueOf(500000L))
            )
        )
    )

    val billReturnRequest = BillReturnRequest(peopleRequest, billReturn)

    val billReturnObservation = BillReturnObservation(
        null,
        BillReturnId(UUID.randomUUID()),
        Observation("My Observ"),
        UserId(UUID.randomUUID()),
        null,
        null,
        null
    )

    private fun getRandomString() : String {
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
        return (1..12)
            .map { charset.random() }
            .joinToString("")
    }

}