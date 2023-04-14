package com.qohat.services

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import arrow.core.firstOrNone
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.identity
import arrow.core.leftIfNull
import arrow.core.right
import arrow.core.traverse
import arrow.fx.coroutines.parTraverseEither
import com.qohat.domain.Assignment
import com.qohat.domain.PeopleCompany
import com.qohat.domain.PeopleRequestId
import com.qohat.domain.RequestAssignment
import com.qohat.domain.RoleName
import com.qohat.domain.UserId
import com.qohat.error.DomainError
import com.qohat.error.UserAssignmentError
import com.qohat.repo.AssignmentRepo
import com.qohat.repo.UserRepo
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface AssignmentService {
    suspend fun assignV2(peopleRequestId: PeopleRequestId): Either<DomainError, Unit>

    suspend fun assign(): Option<Assignment>

}

class DefaultAssignmentService(private val assignmentRepo: AssignmentRepo, private val userRepo: UserRepo): AssignmentService {

    private val logger: Logger = LoggerFactory.getLogger(DefaultAssignmentService::class.java)

    override suspend fun assignV2(peopleRequestId: PeopleRequestId): Either<DomainError, Unit> = either {
        val allAssignments = assignmentRepo.findAll().bind()
        val userIds = allAssignments.map { assign -> assign.userId }
        listOf(RoleName.Fidu, RoleName.Agent, RoleName.Validator)
        .parTraverseEither {
            retrieveAssignment(userIds, it, peopleRequestId)
            .flatMap { assignment ->
                assignmentRepo.save(assignment)
                .tap { logger.info("Assignment Successfully User: ${assignment.userId.value} - PeopleRequestId: ${assignment.peopleRequestId}") }
            }
        }
        .tapLeft { e -> logger.error("There is not users with role to assign the PeopleRequest ${peopleRequestId.value}", e) }
        .bind()
    }

    override suspend fun assign(): Option<Assignment> = None

    private suspend fun retrieveAssignment(userIds: List<UserId>, roleName: RoleName, peopleRequestId: PeopleRequestId): Either<DomainError, RequestAssignment> = either {
        val maybeId = userRepo.findByV2(roleName, userIds).bind()
        val id = (maybeId?.let { id -> Either.Right(id) } ?: assignmentRepo.findProspect(roleName).map { it?.userId }).bind()
        ensureNotNull(id) { UserAssignmentError(roleName.name) }
        RequestAssignment(id, peopleRequestId)
    }

}

