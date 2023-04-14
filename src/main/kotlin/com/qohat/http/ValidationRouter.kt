package com.qohat.http

import arrow.core.None
import arrow.core.Some
import arrow.core.toOption
import com.qohat.domain.*
import com.qohat.features.AuthUser
import com.qohat.features.withPermission
import com.qohat.services.CompanyValidationEventsService
import com.qohat.services.PeopleValidationEventsService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.Route

fun Route.validationRouting(
    companyValidationEventService: CompanyValidationEventsService,
    peopleValidationEventsService: PeopleValidationEventsService) {
    authenticate {
        route("/validations") {
            route("/company") {
                withPermission(PermissionCode.RdVal) {
                    get("{companyId}") {
                        val id = call.parameters["companyId"] ?: return@get call.respondText(
                            "Missing or malformed companyId",
                            status = HttpStatusCode.BadRequest
                        )

                        val events = companyValidationEventService.findAll(CompanyId(id))
                        call.respond(events)
                    }
                }
                withPermission(PermissionCode.WtVal) {
                    post {
                        when(val principal = call.principal<AuthUser>().toOption()) {
                            is None -> call.respond(
                                status = HttpStatusCode.Unauthorized,
                                "Principal is not authorized for doing this action"
                            )
                            is Some<AuthUser> -> {
                                val event = call.receive<ValidationCompaniesAttachmentsEvent>()
                                val eventId = companyValidationEventService.save(event.copy(userId = principal.value.id.toString(), userName = principal.value.fullName))
                                call.respond(status = HttpStatusCode.Created, mapOf("value" to "Validation event was created correctly with Id: $eventId"))
                            }
                        }
                    }
                    post("/bulk") {
                        when(val principal = call.principal<AuthUser>().toOption()) {
                            is None -> call.respond(
                                status = HttpStatusCode.Unauthorized,
                                "Principal is not authorized for doing this action"
                            )
                            is Some<AuthUser> -> {
                                val events = call.receive<List<ValidationCompaniesAttachmentsEvent>>().map { it.copy(userId = principal.value.id.toString(), userName = principal.value.fullName) }
                                companyValidationEventService.saveBulk(events)
                                call.respond(status = HttpStatusCode.Accepted, mapOf("value" to "Validation events were created correctly"))
                            }
                        }
                    }
                }
            }
            route("/people") {
                withPermission(PermissionCode.RdVal) {
                    get("{peopleCompanyId}") {
                        val id = call.parameters["peopleCompanyId"] ?: return@get call.respondText(
                            "Missing or malformed peopleCompanyId",
                            status = HttpStatusCode.BadRequest
                        )

                        val events = peopleValidationEventsService.findAll(PeopleCompanyId(id))
                        call.respond(events)
                    }
                }
                withPermission(PermissionCode.WtVal) {
                    post {
                        when(val principal = call.principal<AuthUser>().toOption()) {
                            is None -> call.respond(status = HttpStatusCode.Unauthorized, "Principal is not authorized for doing this action")
                            is Some<AuthUser> -> {
                                val event = call.receive<ValidationPeopleAttachmentsEvent>()
                                val eventId = peopleValidationEventsService.save(event.copy(userId = principal.value.id.toString(), userName = principal.value.fullName))
                                call.respond(status = HttpStatusCode.Created, mapOf("value" to "Validation event was created correctly with Id: $eventId"))
                            }
                        }
                    }
                    post("/bulk") {
                        when(val principal = call.principal<AuthUser>().toOption()) {
                            is None -> call.respond(status = HttpStatusCode.Unauthorized, "Principal is not authorized for doing this action")
                            is Some<AuthUser> -> {
                                val events = call.receive<List<ValidationPeopleAttachmentsEvent>>().map { it.copy(userId = principal.value.id.toString(), userName = principal.value.fullName) }
                                peopleValidationEventsService.saveBulk(events)
                                call.respond(status = HttpStatusCode.Accepted, mapOf("value" to "Validation events were created correctly"))
                            }
                        }
                    }
                }
            }
        }
    }
}