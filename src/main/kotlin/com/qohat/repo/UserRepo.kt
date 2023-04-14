package com.qohat.repo

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.singleOrNone
import arrow.core.toOption
import at.favre.lib.crypto.bcrypt.BCrypt
import com.qohat.domain.CcfId
import com.qohat.domain.EncryptedPassword
import com.qohat.domain.RecoverToken
import com.qohat.domain.RoleName
import com.qohat.domain.User
import com.qohat.domain.UserEmail
import com.qohat.domain.UserId
import com.qohat.domain.ValueList
import com.qohat.entities.Lists
import com.qohat.entities.Roles
import com.qohat.entities.Users
import com.qohat.entities.toRole
import com.qohat.error.DomainError
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.UUID

interface UserRepo {
    suspend fun findBy(id: UserId): User?
    suspend fun findBy(email: UserEmail): Either<DomainError, User?>
    suspend fun findAllBy(role: RoleName): List<User>
    suspend fun findByV2(role: RoleName, users: List<UserId>): Either<DomainError, UserId?>
    suspend fun findAll(): List<User>
    suspend fun save(user: User): UserId
    suspend fun save(users: List<User>)
    suspend fun update(id: UserId, user: User)
    suspend fun update(id: UserId, newPassword: EncryptedPassword)
    suspend fun askRecoverToken(email: UserEmail): RecoverToken
    suspend fun canRecoverPassword(id: UserId, recoverToken: RecoverToken): Boolean
    suspend fun delete(id: UserId)
}

class DefaultUserRepo(private val db: Database): UserRepo {
    override suspend fun findBy(id: UserId): User? =
        newSuspendedTransaction(Dispatchers.IO, db) {
            Users
                .join(Lists.DocumentType, JoinType.INNER, Users.document_type, Lists.DocumentType[Lists.id])
                .join(Roles, JoinType.INNER, Users.role_id, Roles.id)
                .select { Users.id.eq(id.value) and Users.active.eq(true) }
                .singleOrNull()?.let { toUser(it) }
        }

    override suspend fun findBy(email: UserEmail): Either<DomainError, User?> =
        transact(db) {
            Users
                .join(Lists.DocumentType, JoinType.INNER, Users.document_type, Lists.DocumentType[Lists.id])
                .join(Roles, JoinType.INNER, Users.role_id, Roles.id)
                .select { Users.email.lowerCase().eq(email.value) }
                .singleOrNull()?.let { toUser(it) }
        }

    override suspend fun findAllBy(role: RoleName): List<User> =
        newSuspendedTransaction(Dispatchers.IO, db) {
            Users
                .join(Lists.DocumentType, JoinType.INNER, Users.document_type, Lists.DocumentType[Lists.id])
                .join(Roles, JoinType.INNER, Users.role_id, Roles.id)
                .select { Roles.name.lowerCase().eq(role.value) }
                .orderBy(Users.created_at to SortOrder.DESC)
                .map { toUser(it) }
        }

    override suspend fun findByV2(role: RoleName, users: List<UserId>): Either<DomainError, UserId?> =
        transact(db) {
            Users
            .join(Roles, JoinType.INNER, Users.role_id, Roles.id)
            .slice(Users.id)
            .select {
                Roles.name.lowerCase().eq(role.value) and
                Users.active.eq(true) and
                Users.id.notInList(users.map { it.value })
            }
            .orderBy(Users.created_at to SortOrder.DESC)
            .firstOrNull()?.let { UserId(it[Users.id]) }
        }

    override suspend fun findAll(): List<User> =
        newSuspendedTransaction(Dispatchers.IO, db) {
            Users
                .join(Lists.DocumentType, JoinType.INNER, Users.document_type, Lists.DocumentType[Lists.id])
                .join(Roles, JoinType.INNER, Users.role_id, Roles.id)
                .select { Users.active.eq(true) }
                .map { toUser(it) }
        }

    override suspend fun save(user: User): UserId =
        newSuspendedTransaction(Dispatchers.IO, db) {
            val id = Users.insert {
                it[id] = UUID.randomUUID()
                it[name] = user.name
                it[last_name] = user.lastName
                it[email] = user.email
                it[password] = BCrypt.withDefaults().hashToString(12, user.password.toCharArray())
                it[document_type] = user.documentType.id
                it[document] = user.document
                it[address] = user.address.getOrElse { null }
                it[role_id] = user.role.id.value
                it[active] = true
                //TODO Review this
                it[ccf_id] = user.ccf.value
                it[created_at] = LocalDateTime.now()
                it[updated_at] = LocalDateTime.now()
            } get Users.id
            UserId(id)
        }

    override suspend fun save(users: List<User>) {
        newSuspendedTransaction(Dispatchers.IO, db) {
            Users.insertBatch(users)
            Unit
        }
    }

    override suspend fun update(id: UserId, user: User) =
        newSuspendedTransaction(Dispatchers.IO, db) {
            Users.update({ Users.id eq id.value }) {
                it[name] = user.name
                it[last_name] = user.lastName
                it[email] = user.email
                it[document_type] = user.documentType.id
                it[document] = user.document
                it[address] = user.address.getOrElse { null }
                it[role_id] = user.role.id.value
                it[active] = user.active
                it[updated_at] = LocalDateTime.now()
            }
            Unit
        }

    override suspend fun update(id: UserId, newPassword: EncryptedPassword) =
        newSuspendedTransaction(Dispatchers.IO, db) {
            Users.update({ Users.id eq id.value }) {
                it[password] = newPassword.value
                it[recovering_password] = false
                it[updated_at] = LocalDateTime.now()
            }
            Unit
        }

    override suspend fun askRecoverToken(email: UserEmail): RecoverToken {
        val recoverToken = UUID.randomUUID()
        return newSuspendedTransaction(Dispatchers.IO, db) {
            Users.update({ Users.email eq email.value }) {
                it[recover_token] = recoverToken
                it[recovering_password] = true
                it[recover_expiration] = LocalDateTime.now().plusMinutes(10)
            }
            RecoverToken(recoverToken)
        }
    }


    override suspend fun canRecoverPassword(id: UserId, recoverToken: RecoverToken): Boolean =
        newSuspendedTransaction(Dispatchers.IO, db) {
            Users
                .select { Users.id.eq(id.value) and
                        Users.recovering_password.eq(true) and
                        Users.recover_token.eq(recoverToken.value) and
                        Users.recover_expiration.greater(LocalDateTime.now())
                }
                .singleOrNone()
                .isDefined()
        }


    override suspend fun delete(id: UserId) =
        newSuspendedTransaction(Dispatchers.IO, db) {
            Users.update({ Users.id eq id.value }) {
                it[active] = false
                it[updated_at] = LocalDateTime.now()
            }
            Unit
        }

    private fun toUser(row: ResultRow): User =
        User(
            id = UserId(row[Users.id]),
            name = row[Users.name],
            lastName = row[Users.last_name],
            email = row[Users.email],
            emailConfirmation = null,
            password = row[Users.password],
            passwordConfirmation = null,
            documentType = ValueList(row[Lists.DocumentType[Lists.id]], row[Lists.DocumentType[Lists.name]], row[Lists.DocumentType[Lists.list]], row[Lists.DocumentType[Lists.active]]),
            document = row[Users.document],
            address = row[Users.address].toOption(),
            role = row.toRole(),
            active = row[Users.active],
            ccf = row[Users.ccf_id]?.let { CcfId(it) } ?: CcfId( 0),
            createdAt = row[Users.created_at],
            updatedAt = row[Users.updated_at]
        )
}