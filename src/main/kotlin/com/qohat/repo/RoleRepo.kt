package com.qohat.repo

import arrow.core.Either
import com.qohat.domain.Role
import com.qohat.domain.RoleId
import com.qohat.entities.Roles
import com.qohat.entities.toRole
import com.qohat.error.DomainError
import org.jetbrains.exposed.sql.Database

interface RoleRepo {
    suspend fun findAll(): Either<DomainError, List<Role>>
    suspend fun save(role: Role): Either<DomainError, RoleId>
    suspend fun findBy(roleId: RoleId): Either<DomainError, Role?>

}

class DefaultRoleRepo(private val db: Database): RoleRepo {
    override suspend fun findAll(): Either<DomainError, List<Role>> =
        transact(db) {
            Roles.selectAll.map { it.toRole() }
        }

    override suspend fun save(role: Role): Either<DomainError, RoleId> =
        transact(db) { Roles.insertQuery(role) }.map { RoleId(it) }

    override suspend fun findBy(roleId: RoleId): Either<DomainError, Role?> =
        transact(db) { Roles.selectQuery(roleId)?.toRole() }
}