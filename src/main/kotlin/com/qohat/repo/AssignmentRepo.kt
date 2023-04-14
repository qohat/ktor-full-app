package com.qohat.repo

import arrow.core.Either
import com.qohat.domain.Assignment
import com.qohat.domain.RequestAssignment
import com.qohat.domain.RoleName
import com.qohat.entities.Assignments
import com.qohat.entities.toAssignment
import com.qohat.error.DomainError
import org.jetbrains.exposed.sql.Database

interface AssignmentRepo {
    suspend fun findAll(): Either<DomainError, List<Assignment>>
    suspend fun findProspect(role: RoleName): Either<DomainError, Assignment?>
    suspend fun save(assignments: List<RequestAssignment>): Either<DomainError, Unit>
    suspend fun save(assignment: RequestAssignment): Either<DomainError, Unit>

}

class DefaultAssignmentRepo(private val db: Database): AssignmentRepo {
    override suspend fun findAll(): Either<DomainError, List<Assignment>> =
        transact(db) {
            Assignments.selectAll.map { it.toAssignment() }
        }

    override suspend fun findProspect(role: RoleName): Either<DomainError, Assignment?> =
        transact(db) {
            Assignments.selectBy(role).firstOrNull()?.toAssignment()
        }

    override suspend fun save(assignments: List<RequestAssignment>): Either<DomainError, Unit> =
        transact(db) {
            Assignments.insertBatch(assignments)
        }.void()

    override suspend fun save(assignment: RequestAssignment): Either<DomainError, Unit> =
        transact(db) {
            Assignments.insertQuery(assignment)
        }.void()
}
