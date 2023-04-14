package com.qohat.domain

import arrow.core.Either
import arrow.core.Option
import arrow.core.ValidatedNel
import arrow.core.invalidNel
import arrow.core.traverse
import arrow.core.valid
import arrow.core.zip
import arrow.typeclasses.Semigroup
import com.qohat.codec.Codecs
import com.qohat.error.DomainError
import com.qohat.error.EmailDoesNotMatch
import com.qohat.error.InvalidPermissionError
import com.qohat.error.InvalidSignUp
import com.qohat.error.PasswordDoesNotMatch
import com.qohat.error.SignUpError
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class User(
    val id: UserId,
    val name: String,
    val lastName: String,
    val email: String,
    val emailConfirmation: String?,
    val password: String,
    val passwordConfirmation: String?,
    val documentType: ValueList,
    val document: String,
    @Serializable(Codecs.OptionSerializer::class)
    val address: Option<String>,
    val role: Role,
    val active: Boolean,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(Codecs.LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val ccf: CcfId
)

fun User.confirmEmailAndPassword(): Either<InvalidSignUp, User> =
    validateEmailConfirmation().zip(
        Semigroup.nonEmptyList(),
        validatePasswordConfirmation()
    ){ _, _ -> this }
    .toEither()
    .mapLeft { InvalidSignUp(it.map { err -> err.toString() }) }

private fun User.validateEmailConfirmation(): ValidatedNel<SignUpError, String> =
    if(emailConfirmation != email) EmailDoesNotMatch.invalidNel() else email.valid()

private fun User.validatePasswordConfirmation(): ValidatedNel<SignUpError, String> =
    if(passwordConfirmation != password) PasswordDoesNotMatch.invalidNel() else password.valid()


@JvmInline
@Serializable
value class Password(val value: String)
@Serializable
data class EncryptedPassword(val value: String)

@JvmInline
@Serializable
value class RecoverToken(@Serializable(Codecs.UUIDSerializer::class) val value: UUID)
@Serializable
data class RecoverPasswordRequest(val token: RecoverToken, val password: Password)
@Serializable
data class CcfId(val value: Int)
@Serializable
data class UserId(@Serializable(Codecs.UUIDSerializer::class) val value: UUID) {
    companion object {
        fun unApply(arg: String): UserId? =
            Either.catch { UUID.fromString(arg) }
                .map { UserId(it) }
                .orNull()
    }
}

@JvmInline
@Serializable
value class NewUserId(@Serializable(Codecs.UUIDSerializer::class) val value: UUID)

@JvmInline
@Serializable
value class CreatedById(@Serializable(Codecs.UUIDSerializer::class) val value: UUID)

@Serializable
data class UserEmail(val value: String) {
    companion object {
        fun unApply(arg: String?): UserEmail? =
            arg?.let { UserEmail(it) }
    }
}

@Serializable
data class Role(
    val id: RoleId,
    val name: Name,
    val active: Active,
    val permissions: PermissionChain
)

fun Role.isValid(): Either<DomainError, Role> =
    permissions.value.split(":")
        .traverse { permission ->
            Either.fromNullable(PermissionCode.unApply(permission))
            .mapLeft { InvalidPermissionError(permission) }
        }
        .map { this }

@JvmInline
@Serializable
value class RoleId(val value: Int)

enum class RoleName(val value: String) {
    Admin("admin"),
    Agent("agent"),
    Fidu("fidu"),
    Validator("validator"),
    CCF("ccf"),
    FIDUSEC("fidusec");
    companion object {
        fun unApply(arg: String): RoleName? =
            values().find{ it.value == arg }
    }
}

@JvmInline
@Serializable
value class PermissionChain(val value: String)

enum class PermissionCode {   //ls.can('RdU')
    RdU, //ReadUser //Get
    WtU, //WriteUser //POST
    DltU, //DeleteUser //DELETE
    UptU, //UpdateUser //PUT, PATCH
    RdC, //ReadCompanies
    WtC, //WriteCompanies
    DltC, // DeleteCompanies
    UptC, // UpdateCompanies
    RdP, // Read People
    WtP, // Write People
    DltP, // Delete People
    UptP, // Update People
    RdPC, // Read People Company
    WtPC, // Write People Company
    DltPC, // Delete People Company
    UptPC, // Update People Company
    RdVal, // Read Validations
    WtVal, // Write Validations
    RdNPaid, // Read Non Paid Validations
    RdAllNPaid, // Read Non Paid Validations
    RdPaid, // Read Non Paid Validations
    RdCompleted, // Read Non Paid Validations
    RdRpts, // Read Non Paid Validations
    ExptRpts, // Read Non Paid Validations
    ImptRpts, // Read Non Paid Validations
    RdPR, // Read People Requests // GET
    RdPRNf, // Read People Requests Not Filtered// GET
    RdPRTp, // Read People Requests To Pay// GET
    WtPR, // Write People Requests
    UptPR, // Update People Requests
    UptPRSt, // Update People Requests
    RdPrd,
    WtPrd,
    DltPrd,
    UptPrd,
    RdSup,
    WtSup,
    DltSup,
    UptSup,
    RdStg,
    WtStg,
    DltStg,
    UptStg,
    RdCf,
    WtObs,
    RdObs,
    Empty;

    companion object {
        fun unApply(arg: String): PermissionCode? =
            values().filterNot { it == Empty }.find { it.name == arg }
    }
}

@Serializable
data class UserAttempt(val email: String, val password: String)

@Serializable
data class UserToken(val id: String, val fullName: String, val token: String, val permissionChain: PermissionChain, val ccf: Int, val roleName: Name)