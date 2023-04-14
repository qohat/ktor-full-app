package com.qohat.services

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.qohat.config.JWTConfig
import com.qohat.domain.User
import com.qohat.domain.UserAttempt
import com.qohat.domain.UserEmail
import com.qohat.domain.UserToken
import com.qohat.error.DomainError
import com.qohat.error.EmailError
import com.qohat.error.PasswordError
import com.qohat.repo.UserRepo
import java.util.Date

interface AuthService {
    suspend fun userAttempt(userAttempt: UserAttempt): Either<DomainError, User>
    suspend fun authenticate(user: User): UserToken
    suspend fun logout()
}

class DefaultAuthService(val jwtConfig: JWTConfig, val userRepo: UserRepo): AuthService {
    override suspend fun userAttempt(userAttempt: UserAttempt): Either<DomainError, User> = either {
        val email = userAttempt.email.lowercase()
        val userEmail = UserEmail(email)
        val maybeUser = userRepo.findBy(userEmail).bind()
        val user = ensureNotNull(maybeUser) { EmailError }
        ensure(BCrypt.verifyer().verify(userAttempt.password.toCharArray(), user.password).verified) { PasswordError }
        user
    }

    override suspend fun authenticate(user: User): UserToken {
        val token = JWT.create()
            .withAudience(jwtConfig.audience)
            .withIssuer(jwtConfig.issuer)
            .withClaim("id", user.id.value.toString())
            .withClaim("email", user.email)
            .withClaim("fullName", "${user.name} ${user.lastName}")
            .withClaim("scopes", user.role.permissions.value)
            .withClaim("role", "${user.role.id}-${user.role.name}")
            .withClaim("ccf", user.ccf.value)
            .withExpiresAt(Date(System.currentTimeMillis() + (60000 * 3600)))
            .sign(Algorithm.HMAC256(jwtConfig.secret))

        return UserToken(
            id = user.id.value.toString(),
            fullName = "${user.name} ${user.lastName}",
            token = token,
            permissionChain = user.role.permissions,
            ccf = user.ccf.value,
            roleName = user.role.name
        )
    }

    override suspend fun logout() {
        TODO("Not implemented")
    }
}