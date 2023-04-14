package com.qohat.services

import arrow.core.Either
import arrow.core.continuations.either
import at.favre.lib.crypto.bcrypt.BCrypt
import com.qohat.domain.EncryptedPassword
import com.qohat.domain.Password
import com.qohat.domain.RecoverPasswordRequest
import com.qohat.domain.RecoverToken
import com.qohat.domain.User
import com.qohat.domain.UserEmail
import com.qohat.domain.UserId
import com.qohat.error.RecoverPasswordError
import com.qohat.error.UnableToRecover
import com.qohat.repo.UserRepo

interface UserService {
    suspend fun findBy(id: UserId): User?
    suspend fun findAll(): List<User>
    suspend fun save(user: User): UserId
    suspend fun update(id: UserId, user: User)
    suspend fun delete(id: UserId)

    suspend fun askRecoverToken(email: UserEmail): RecoverToken
    suspend fun recoverPassword(userId: UserId, recoverPasswordRequest: RecoverPasswordRequest): Either<RecoverPasswordError, Unit>
}

class DefaultUserService(val userRepo: UserRepo): UserService {
    override suspend fun findBy(id: UserId): User? =
        userRepo.findBy(id)

    override suspend fun findAll(): List<User> =
        userRepo.findAll().map { it.copy(password = "") }

    override suspend fun save(user: User): UserId {
        val newPassword = BCrypt.withDefaults().hashToString(12, user.password.toCharArray())
        return userRepo.save(user.copy(password = newPassword))
    }


    override suspend fun update(id: UserId, user: User): Unit =
        userRepo.update(id, user)

    override suspend fun delete(id: UserId): Unit =
        userRepo.delete(id)

    override suspend fun askRecoverToken(email: UserEmail): RecoverToken =
        userRepo.askRecoverToken(email)


    override suspend fun recoverPassword(userId: UserId, recoverPasswordRequest: RecoverPasswordRequest): Either<RecoverPasswordError, Unit> = either {
        ensure(userRepo.canRecoverPassword(userId, recoverPasswordRequest.token)) {
            UnableToRecover("The user:${userId} can't recover password with token: ${recoverPasswordRequest.token.value}")
        }
        val encryptedPassword = EncryptedPassword(encryptPassword(recoverPasswordRequest.password))
        userRepo.update(userId, encryptedPassword)
    }

    private fun encryptPassword(password: Password) =
        BCrypt.withDefaults().hashToString(12, password.value.toCharArray())
}