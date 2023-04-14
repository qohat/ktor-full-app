package com.qohat.entities

import com.qohat.domain.Active
import com.qohat.domain.Address
import com.qohat.domain.ArmedConflictVictim
import com.qohat.domain.Assignment
import com.qohat.domain.AttachmentId
import com.qohat.domain.AttachmentName
import com.qohat.domain.AttachmentPath
import com.qohat.domain.AttachmentsValidationEventShow
import com.qohat.domain.BelongsOrganization
import com.qohat.domain.BirthDay
import com.qohat.domain.BoughtDate
import com.qohat.domain.CellPhone
import com.qohat.domain.CreatedAt
import com.qohat.domain.CreatedById
import com.qohat.domain.Displaced
import com.qohat.domain.Document
import com.qohat.domain.Email
import com.qohat.domain.Hectares
import com.qohat.domain.IssueDocumentDate
import com.qohat.domain.Lane
import com.qohat.domain.LastName
import com.qohat.domain.MinimumToApply
import com.qohat.domain.Name
import com.qohat.domain.NewPayment
import com.qohat.domain.NewPeople
import com.qohat.domain.NewPeopleId
import com.qohat.domain.Nit
import com.qohat.domain.NonContentAttachment
import com.qohat.domain.Observation
import com.qohat.domain.OrganizationBelongingInfo
import com.qohat.domain.PaymentDate
import com.qohat.domain.PeopleAccountNumber
import com.qohat.domain.PeopleRequestBillReturn
import com.qohat.domain.PeopleRequestExpiration
import com.qohat.domain.PeopleRequestId
import com.qohat.domain.PeopleRequestNumber
import com.qohat.domain.Percentage
import com.qohat.domain.PermissionChain
import com.qohat.domain.Phone
import com.qohat.domain.Price
import com.qohat.domain.ProductId
import com.qohat.domain.ProductShow
import com.qohat.domain.Quantity
import com.qohat.domain.RegisterNumber
import com.qohat.domain.RequestExpiration
import com.qohat.domain.ResponseExpiration
import com.qohat.domain.Role
import com.qohat.domain.RoleId
import com.qohat.domain.SingleMother
import com.qohat.domain.StorageId
import com.qohat.domain.StorageShow
import com.qohat.domain.SupplyDetailShow
import com.qohat.domain.SupplyId
import com.qohat.domain.SupplyShow
import com.qohat.domain.SupplyValue
import com.qohat.domain.TermsAcceptance
import com.qohat.domain.UserId
import com.qohat.domain.ValueList
import com.qohat.domain.requests.Amount
import com.qohat.domain.requests.BillReturnId
import com.qohat.domain.requests.BillReturnObservation
import com.qohat.domain.requests.BillReturnObservationId
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.count
import java.math.RoundingMode

fun ResultRow.toNewPeople(): NewPeople = NewPeople(
    id = NewPeopleId(this[People.id]),
    name = Name(this[People.name]),
    lastName = LastName(this[People.last_name]),
    documentType = ValueList(this[Lists.DocumentType[Lists.id]], this[Lists.DocumentType[Lists.name]], this[Lists.DocumentType[Lists.list]], this[Lists.DocumentType[Lists.active]]),
    document = Document(this[People.document]),
    issueDocumentDate = this[People.issue_document_date]?.let { IssueDocumentDate(it) },
    birthday = BirthDay(this[People.birthday]),
    sex = ValueList(this[Lists.Sex[Lists.id]], this[Lists.Sex[Lists.name]], this[Lists.Sex[Lists.list]], this[Lists.Sex[Lists.active]]),
    gender = ValueList(this[Lists.Gender[Lists.id]], this[Lists.Gender[Lists.name]], this[Lists.Gender[Lists.list]], this[Lists.Gender[Lists.active]]),
    address = this[People.address]?.let { Address(it) },
    phone = this[People.phone]?.let { Phone(it) },
    cellPhone = this[People.cell_phone]?.let { CellPhone(it) },
    email = Email(this[People.email]),
    populationGroup = ValueList(this[Lists.PopulationGroup[Lists.id]], this[Lists.PopulationGroup[Lists.name]], this[Lists.PopulationGroup[Lists.list]], this[Lists.PopulationGroup[Lists.active]]),
    ethnicGroup = ValueList(this[Lists.EthnicGroup[Lists.id]], this[Lists.EthnicGroup[Lists.name]], this[Lists.EthnicGroup[Lists.list]], this[Lists.EthnicGroup[Lists.active]]),
    disability = ValueList(this[Lists.Disability[Lists.id]], this[Lists.Disability[Lists.name]], this[Lists.Disability[Lists.list]], this[Lists.Disability[Lists.active]]),
    armedConflictVictim = this[People.armed_conflict_victim]?.let { ArmedConflictVictim(it) },
    displaced = this[People.displaced]?.let { Displaced(it) },
    termsAcceptance = TermsAcceptance(this[People.terms_acceptance]),
    singleMother = this[People.single_mother]?.let { SingleMother(it) },
    propertyInformation = this[PropertyInformation.people_id]?.let {
        com.qohat.domain.PropertyInformation(
            address = Address(this[PropertyInformation.address]),
            name = Name(this[PropertyInformation.name]),
            department = ValueList(this[Lists.Department[Lists.id]], this[Lists.Department[Lists.name]], this[Lists.Department[Lists.list]], this[Lists.Department[Lists.active]]),
            city = ValueList(this[Lists.City[Lists.id]], this[Lists.City[Lists.name]], this[Lists.City[Lists.list]], this[Lists.City[Lists.active]]),
            lane = this[PropertyInformation.lane]?.let { Lane(it) },
            hectares = Hectares(this[PropertyInformation.hectares].setScale(2, RoundingMode.HALF_EVEN)),
        )
    },
    belongsOrganization = this[People.belongs_organization]?.let { BelongsOrganization(it) },
    organizationBelongingInfo = this[OrganizationBelongingInformation.people_id]?.let {
        OrganizationBelongingInfo(
            type = ValueList(this[Lists.OrganizationType[Lists.id]], this[Lists.OrganizationType[Lists.name]], this[Lists.OrganizationType[Lists.list]], this[Lists.OrganizationType[Lists.active]]),
            name = Name(this[OrganizationBelongingInformation.name]),
            nit = this[OrganizationBelongingInformation.nit]?.let { Nit(it) }
        )
    },
    paymentInformation = this[PaymentInformation.payment_type]?.let {
        com.qohat.domain.PaymentInformation(
            paymentType = ValueList(this[Lists.PaymentType[Lists.id]], this[Lists.PaymentType[Lists.name]], this[Lists.PaymentType[Lists.list]], this[Lists.PaymentType[Lists.active]]),
            accountType = this[PaymentInformation.account_bank_type_id]?.let { ValueList(this[Lists.AccountBankType[Lists.id]], this[Lists.AccountBankType[Lists.name]], this[Lists.AccountBankType[Lists.list]], this[Lists.AccountBankType[Lists.active]]) },
            bank = this[PaymentInformation.bank_id]?.let { ValueList(this[Lists.Bank[Lists.id]], this[Lists.Bank[Lists.name]], this[Lists.Bank[Lists.list]], this[Lists.Bank[Lists.active]])},
            accountNumber = this[PaymentInformation.account_number]?.let { PeopleAccountNumber(it) },
            branch = this[PaymentInformation.branch_id]?.let { ValueList(this[Lists.BankBranch[Lists.id]], this[Lists.BankBranch[Lists.name]], this[Lists.BankBranch[Lists.list]], this[Lists.BankBranch[Lists.active]]) }
        )
    },
    createdBy = this[People.created_by_id]?.let { CreatedById(it) }
)
fun ResultRow.toPeopleRequestBillReturn(): PeopleRequestBillReturn =
    PeopleRequestBillReturn(
        id = PeopleRequestId(this@toPeopleRequestBillReturn[PeopleRequests.id].value),
        people = toNewPeople(),
        number = this@toPeopleRequestBillReturn[PeopleRequests.number]?.let { PeopleRequestNumber(it) },
        chain = Name(this@toPeopleRequestBillReturn[Lists.Chain[Lists.name]]),
        cropGroup = Name(this@toPeopleRequestBillReturn[Lists.CropGroup[Lists.name]]),
        product = toProductShow(),
        billReturnId = BillReturnId(this@toPeopleRequestBillReturn[BillReturns.id]),
        type = this@toPeopleRequestBillReturn[PeopleRequests.type],
        state = this@toPeopleRequestBillReturn[PeopleRequests.state],
        createdAt = CreatedAt(this@toPeopleRequestBillReturn[PeopleRequests.created_at]),
        storage = toStorageShow(),
        expirations = listOf()
    )
fun ResultRow.toProductShow(): ProductShow =
    ProductShow(
        id = ProductId(this[Products.id]),
        name = Name(this[Products.name]),
        chain = Name(this[Lists.Chain[Lists.name]]),
        cropGroup = Name(this[Lists.CropGroup[Lists.name]]),
        percentage = Percentage(this[Products.percentage]),
        maximumToSubsidize = Amount(this[Products.maximum_to_subsidize]),
        minimumToApply = MinimumToApply(this[Products.minimum_to_apply]),
        active = Active(this[Products.active]),
        createdAt = CreatedAt(this[Products.created_at])
    )

fun ResultRow.toSupplyShow(): SupplyShow =
    SupplyShow(
        id = SupplyId(this[Supplies.id]),
        name = Name(this[Supplies.name]),
        cropGroup = Name(this[Lists.CropGroup[Lists.name]]),
        price = Price(this[Supplies.price]),
        active = Active(this[Supplies.active]),
        createdAt = CreatedAt(this[Supplies.created_at])
    )

fun ResultRow.toStorageShow(): StorageShow =
    StorageShow(
        id = StorageId(this[Storages.id]),
        name = Name(this[Storages.name]),
        address = this[Storages.address]?.let { Address(it) },
        document = Document(this[Storages.document]),
        department = Name(this[Storages.StorageDepartment[Lists.name]]),
        city = Name(this[Storages.StorageCity[Lists.name]]),
        registerNumber = this[Storages.register_number]?.let { RegisterNumber(it) },
        activityRegistered = Name(this[Lists.Activity[Lists.name]]),
        email = this[Storages.email]?.let { Email(it) },
        phone = this[Storages.phone]?.let { Phone(it) },
        active = Active(this[Storages.active]),
        createdAt = CreatedAt(this[Storages.created_at])
    )

fun ResultRow.toNonContentAttachment(): NonContentAttachment =
    NonContentAttachment(
        id = AttachmentId(this[BillReturnsAttachments.id].value),
        name = AttachmentName(this[BillReturnsAttachments.name]),
        path = AttachmentPath(this[BillReturnsAttachments.path]),
        state = this[BillReturnsAttachments.state],
        fileTypeName = Name(this[Lists.Files[Lists.name]])
    )

fun ResultRow.toSupplyDetailShow(): SupplyDetailShow =
    SupplyDetailShow(
        supplyId = SupplyId(this[Supplies.id]),
        supplyName = Name(this[Supplies.name]),
        supplyPrice = Price(this[Supplies.price]),
        value = SupplyValue(this[BillReturnsSupplies.value]),
        measurementUnit = Name(this[Lists.MeasurementUnit[Lists.name]]),
        presentation = this[Lists.Presentation[Lists.name]]?.let { Name(it) },
        boughtDate = this[BillReturnsSupplies.bought_date]?.let { BoughtDate(it) },
        quantity = Quantity(this[BillReturnsSupplies.quantity])
    )

fun ResultRow.toAssignment() =
    Assignment(
        userId = UserId(this[Assignments.user_id]),
        assignments = this[Assignments.people_request_id.count()]
    )

fun ResultRow.toAttachmentsValidationEventShow() =
    AttachmentsValidationEventShow(
        user = Name(this[Users.name]),
        observation = this[BillReturnsValidationAttachmentsEvents.observation]?.let { Observation(it) },
        createdAt = CreatedAt(this[BillReturnsValidationAttachmentsEvents.created_at]),
        reason = Name(this[Lists.Reason[Lists.name]]),
        fileTypeName = Name(this[Lists.Files[Lists.name]]),
        state = this[BillReturnsValidationAttachmentsEvents.state]
    )


fun ResultRow.toBillReturnObservation() =
    BillReturnObservation(
        id = BillReturnObservationId(this[BillReturnObservations.id].value),
        billReturnId = BillReturnId(this[BillReturnObservations.bill_return_id]),
        observation = Observation(this[BillReturnObservations.observation]),
        userId = UserId(this[Users.id]),
        userName = Name(this[Users.name]),
        userLastName = LastName(this[Users.last_name]),
        createdAt = CreatedAt(this[BillReturnObservations.created_at])
    )

fun ResultRow.toNewPayment() =
    NewPayment(
        billReturnId = BillReturnId(this[BillReturnPayments.bill_return_id]),
        date = PaymentDate(this[BillReturnPayments.payment_date]),
        userId = UserId(this[BillReturnPayments.user_id])
    )

fun ResultRow.toPeopleRequestExpiration() =
    PeopleRequestExpiration(
        requestExpiration = RequestExpiration(this[PeopleRequestExpirations.request_expiration]),
        responseExpiration = this[PeopleRequestExpirations.response_expiration]?.let { ResponseExpiration(it) }
    )

fun ResultRow.toRole() =
    Role(
        id = RoleId(this[Roles.id]),
        name = Name(this[Roles.name]),
        active = Active(this[Roles.active]),
        permissions = PermissionChain(this[Roles.permissions])
    )