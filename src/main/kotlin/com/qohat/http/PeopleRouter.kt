package com.qohat.http

import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import com.qohat.domain.CreatedById
import com.qohat.domain.NewPeople
import com.qohat.domain.NewPeopleId
import com.qohat.domain.PeopleId
import com.qohat.domain.PermissionCode
import com.qohat.domain.UserId
import com.qohat.error.PeopleAlreadyExist
import com.qohat.error.PeopleNotFoundError
import com.qohat.features.withPermission
import com.qohat.repo.PeopleRepo
import com.qohat.services.PeopleService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Route.peopleRouting(peopleRepo: PeopleRepo, peopleService: PeopleService) {
    val logger: Logger = LoggerFactory.getLogger("PeopleRouting")
    authenticate {
        route("/people") {
            withPermission(PermissionCode.RdP) {
                get {
                    paginated { params ->
                        withPrincipal { authUser ->
                            respond(
                                either {
                                    val userId = UserId(authUser.id)
                                    peopleRepo.findAllBy(userId, params).bind()
                                }, HttpStatusCode.OK
                            )
                        }
                    }
                }
                get("{id}") {
                    respond(
                        either {
                            val peopleId = receiveParamCatching("id") { NewPeopleId.unApply(it) }.bind()
                            val people = peopleRepo.findBy(peopleId).bind()
                            ensureNotNull(people) { PeopleNotFoundError }
                            people
                        }, HttpStatusCode.OK
                    )
                }
            }
            withPermission(PermissionCode.WtP) {
                post("/new") {
                    withPrincipal { authUser ->
                        respond(
                            either {
                                val newPeople = receiveCatching<NewPeople>().bind()
                                val notExists = peopleRepo.findBy(newPeople.email).bind()?.let { false } ?: true
                                ensure(notExists) { PeopleAlreadyExist("People duplicated email: ${newPeople.email}") }
                                peopleRepo.save(newPeople.copy(createdBy = CreatedById(authUser.id))).bind()
                            }, HttpStatusCode.Created
                        )
                    }
                }
            }
            withPermission(PermissionCode.UptP) {
                put("{id}") {
                    respond(
                        either {
                            val id = receiveParamCatching("id") { NewPeopleId.unApply(it) }.bind()
                            val newPeople = receiveCatching<NewPeople>().bind()
                            val people = peopleRepo.findBy(id).bind()
                            ensureNotNull(people) { PeopleNotFoundError }
                            if(people.email != newPeople.email) {
                                val notEmailExists = peopleRepo.findBy(newPeople.email).bind()?.let { false } ?: true
                                ensure(notEmailExists) { PeopleAlreadyExist("People duplicated email: ${newPeople.email}") }
                            }
                            if(people.document != newPeople.document) {
                                val notDocumentExists = peopleRepo.findBy(newPeople.document).bind()?.let { false } ?: true
                                ensure(notDocumentExists) { PeopleAlreadyExist("People duplicated document: ${newPeople.document}") }
                            }
                            peopleRepo.update(id, newPeople).bind()
                        }, HttpStatusCode.Accepted
                    )
                }
            }
            withPermission(PermissionCode.DltP) {
                delete("{id}") {
                    val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    if (peopleService.delete(PeopleId(id)))
                        call.respond(status = HttpStatusCode.Accepted, "Person removed correctly")
                    else
                        call.respond(
                            status = HttpStatusCode.NotFound,
                            "Unable to delete because there is no people with id $id"
                        )
                }
            }
        }
    }
}