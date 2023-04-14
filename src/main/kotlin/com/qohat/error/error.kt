package com.qohat.error

import arrow.core.NonEmptyList
import com.qohat.domain.AttachmentState
import com.qohat.domain.NewPayment
import com.qohat.domain.StorageId
import com.qohat.domain.SupplyId
import com.qohat.infra.S3KeyObject
import kotlinx.serialization.Serializable

sealed interface DomainError
data class Unexpected(val message: String, val error: Throwable) : DomainError
data class Malformed(val message: String) : DomainError

sealed interface RecoverPasswordError: DomainError
data class UnableToRecover(val message: String): RecoverPasswordError
sealed interface SignUpError: DomainError
object EmailDoesNotMatch: SignUpError
object PasswordDoesNotMatch: SignUpError
data class InvalidSignUp(val errors: NonEmptyList<String>): SignUpError
data class InvalidRoleError(val name: String): DomainError
data class InvalidPermissionError(val name: String): DomainError
object UserNotFound: DomainError
object BalanceNotFound: DomainError
sealed interface AuthError: DomainError
object EmailError: AuthError
object PasswordError: AuthError
sealed interface DBTransactionError: DomainError
data class RepoTransactionError(val message: String): DBTransactionError
sealed interface BuildError: DomainError
data class RequestStateError(val message: String): BuildError
data class RequestTypeError(val message: String): BuildError
sealed interface ProductsError: DomainError
object ProductNotFoundError: ProductsError
sealed interface SuppliesError: DomainError
data class SupplyNotFoundError(val supplyId: SupplyId): SuppliesError
sealed interface StoragesError: DomainError
data class StorageNotFoundError(val storageId: StorageId): StoragesError
sealed interface S3Error: DomainError
data class UploadError(val keyObject: S3KeyObject): S3Error
sealed interface SESError: DomainError
object SendEmailError: SESError
sealed interface PeopleRequestError: DomainError
object PeopleRequestNotFound: PeopleRequestError
object RequestStateChangeError: PeopleRequestError
data class InvalidAttachmentState(val states: List<AttachmentState>, val state: AttachmentState): DomainError
object RequestBackgroundStateChangeError: PeopleRequestError
object NumberOfRequestsExceeded: PeopleRequestError
data class UserAssignmentError(val name: String): DomainError
sealed interface ExportReportError: DomainError
object ToPayReportIsEmpty: ExportReportError
sealed interface ImportFileError: DomainError
object EmptyOrInvalidHeaderFileError: ImportFileError
data class PaymentAlreadyExits(val newPayment: NewPayment): ImportFileError
data class FileNotFoundError(val path: String): DomainError

sealed interface PeopleError: DomainError
object PeopleNotFoundError: PeopleError
data class PeopleAlreadyExist(val message: String): DomainError

data class InvalidMeasurementUnit(val unit: String): DomainError

sealed interface ConfigErrors: DomainError
data class ConfigValueNotFound(val name: String): ConfigErrors
data class PersistConfigError(val message: String): ConfigErrors


@Serializable
data class ServiceError(val message: String, val code: Int)

sealed class ServiceErrorGen(val code: Int) {
    fun gen(message: String): ServiceError =
        ServiceError(message, code)
}
object CompanyErrors {
    object DuplicatedDocument: ServiceErrorGen(4000)
    object DuplicatedEmail: ServiceErrorGen(4001)
    object DuplicatedName: ServiceErrorGen(4002)
}
object PeopleCompanyErrors {
    object StartDateConflict: ServiceErrorGen(7000)
    object PeopleCompanyDoesNotExists: ServiceErrorGen(7001)
    object PeopleCompanyInvalidState: ServiceErrorGen(7002)
    object ReopenErrors: ServiceErrorGen(7003)
    object NonAssignableError: ServiceErrorGen(7004)
    object WomenMenQuantityConflict: ServiceErrorGen(7005)
}
object S3Errors {
    object S3UploadError: ServiceErrorGen(10000)
}
object ImportErrors {
    object HeaderFileError: ServiceErrorGen(11000)
    object EmptyFileError: ServiceErrorGen(11001)
    object InvalidImportRequest: ServiceErrorGen(11002)
}
object ExportErrors {
    object NotFoundDataError: ServiceErrorGen(12000)
}
