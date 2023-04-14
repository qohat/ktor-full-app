package com.qohat.http

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import arrow.core.singleOrNone
import arrow.core.toOption
import com.qohat.domain.AttachFileToPeopleCompany
import com.qohat.domain.AttachmentState
import com.qohat.domain.ListKey
import com.qohat.domain.PeopleCompany
import com.qohat.domain.PeopleCompanyId
import com.qohat.domain.PeopleCompanyValidationId
import com.qohat.domain.PermissionCode
import com.qohat.domain.UserId
import com.qohat.domain.findNext
import com.qohat.domain.hasEnoughEmployees
import com.qohat.domain.minusGender
import com.qohat.domain.plusGender
import com.qohat.error.PeopleCompanyErrors
import com.qohat.features.AuthUser
import com.qohat.features.withPermission
import com.qohat.services.AssignmentService
import com.qohat.services.CompanyService
import com.qohat.services.ListService
import com.qohat.services.PeopleCompanyService
import com.qohat.services.PeopleService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Route.peopleCompanyRouting(peopleCompanyService: PeopleCompanyService, listService: ListService, assignmentService: AssignmentService, companyService: CompanyService, peopleService: PeopleService) {
    val logger: Logger = LoggerFactory.getLogger("PeopleCompanyRouting")
    authenticate {
        route("/people-companies") {
            withPermission(PermissionCode.RdPC) {
                get("/in-review") {
                    when(val principal = call.principal<AuthUser>().toOption()) {
                        is None -> call.respond(
                            status = HttpStatusCode.Unauthorized,
                            "Principal is not authorized for doing this action"
                        )
                        is Some -> call.respond(peopleCompanyService.findAllLite(AttachmentState.InReview, UserId(principal.value.id), paginated()))
                    }
                }
                get("{peopleCompanyId}") {
                    val id = call.parameters["peopleCompanyId"] ?: return@get call.respondText(
                        "Missing or malformed peopleCompanyId",
                        status = HttpStatusCode.BadRequest
                    )
                    when(val peopleCompany = peopleCompanyService.findBy(PeopleCompanyValidationId(id))) {
                        is None -> call.respond(status = HttpStatusCode.NotFound, "There is not peopleCompany with the id: ${id}")
                        is Some -> call.respond(peopleCompany.value)
                    }
                }
                get("/rejected") {
                    when(val principal = call.principal<AuthUser>().toOption()) {
                        is None -> call.respond(
                            status = HttpStatusCode.Unauthorized,
                            "Principal is not authorized for doing this action"
                        )
                        is Some -> call.respond(peopleCompanyService.findAllLite(AttachmentState.Rejected, UserId(principal.value.id), paginated()))
                    }
                }
                get("/non-state") {
                    when(val principal = call.principal<AuthUser>().toOption()) {
                        is None -> call.respond(
                            status = HttpStatusCode.Unauthorized,
                            "Principal is not authorized for doing this action"
                        )
                        is Some -> call.respond(peopleCompanyService.findAllLite(AttachmentState.NonState, UserId(principal.value.id), paginated()))
                    }
                }
                get("/archived") {
                    when(val principal = call.principal<AuthUser>().toOption()) {
                        is None -> call.respond(
                            status = HttpStatusCode.Unauthorized,
                            "Principal is not authorized for doing this action"
                        )
                        is Some -> call.respond(peopleCompanyService.findAllArchived(UserId(principal.value.id)))
                    }
                }
            }
            withPermission(PermissionCode.RdNPaid) {
                get("/non-paid") {
                    when(val principal = call.principal<AuthUser>().toOption()) {
                        is None -> call.respond(
                            status = HttpStatusCode.Unauthorized,
                            "Principal is not authorized for doing this action"
                        )
                        is Some -> call.respond(peopleCompanyService.findAllLite(AttachmentState.NonPaid, UserId(principal.value.id), paginated()))
                    }
                }
            }
            withPermission(PermissionCode.RdAllNPaid) {
                get("/all-non-paid") {
                    call.respond(peopleCompanyService.findAllLite(AttachmentState.NonPaid, paginated()))
                }
            }
            withPermission(PermissionCode.RdCompleted) {
                get("/completed") {
                    when(val principal = call.principal<AuthUser>().toOption()) {
                        is None -> call.respond(
                            status = HttpStatusCode.Unauthorized,
                            "Principal is not authorized for doing this action"
                        )
                        is Some -> call.respond(peopleCompanyService.findAllLite(AttachmentState.Completed, UserId(principal.value.id), paginated()))
                    }
                }
            }
            withPermission(PermissionCode.RdPaid) {
                get("/paid") {
                    when(val principal = call.principal<AuthUser>().toOption()) {
                        is None -> call.respond(
                            status = HttpStatusCode.Unauthorized,
                            "Principal is not authorized for doing this action"
                        )
                        is Some -> call.respond(peopleCompanyService.findAllLite(AttachmentState.Paid, UserId(principal.value.id), paginated()))
                    }
                }
            }
            withPermission(PermissionCode.WtPC) {
                post {
                    val peopleCompany = call.receive<PeopleCompany>()
                    val peopleCompanies = peopleCompanyService.findAllBy(peopleCompany.peopleId)
                    val peopleCompanyStarDateConflict = peopleCompanies.filter {
                        it.startDate.isEqual(peopleCompany.startDate) ||
                                (peopleCompany.startDate.isAfter(it.startDate.minusMonths(1)) &&
                                        peopleCompany.startDate.isBefore(it.startDate.plusMonths(1)))
                    }

                    if (peopleCompanyStarDateConflict.isNotEmpty()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            PeopleCompanyErrors.StartDateConflict.gen(
                                "PeopleCompany StartDate conflict: The startDate difference must be at least by one month: " +
                                        "${peopleCompanyStarDateConflict.map { "Company: ${it.companyId.value} - CurrentStartDate: ${it.startDate} - RequestStartDate: ${peopleCompany.startDate}" }}"
                            )
                        )
                    } else {
                        when(val company = companyService.findBy(peopleCompany.companyId)
                            .filterNot { it.hasEnoughEmployees() }) {
                            is None -> call.respond(
                                status = HttpStatusCode.BadRequest,
                                PeopleCompanyErrors.WomenMenQuantityConflict.gen(
                                    "Company does not exist or Company employees exceeds the maximum value - You can't add more employees to this company: ${peopleCompany.companyId}")
                            )
                            is Some -> when(val principal = call.principal<AuthUser>().toOption()) {
                                is None -> call.respond(
                                    status = HttpStatusCode.Unauthorized,
                                    "Principal is not authorized for doing this action"
                                )
                                is Some -> {
                                    when(val assignment = assignmentService.assign()){
                                        is None -> call.respond(
                                            HttpStatusCode.BadRequest,PeopleCompanyErrors.NonAssignableError.gen("Unable to find a FIDU user to assign the validation")
                                        )
                                        is Some -> {
                                            val peopleCompanyId = peopleCompanyService.save(peopleCompany.copy(createdBy = UserId(principal.value.id), assignedTo = assignment.value.userId))
                                            call.respond(HttpStatusCode.Created, peopleCompanyId)

                                            coroutineScope {
                                                launch(Dispatchers.IO) {
                                                    peopleService.findBy(peopleCompany.peopleId)
                                                        .flatMap { company.value.plusGender(it) }
                                                        .fold({ logger.error("People: ${peopleCompany.peopleId} does not exist... Imposible or Gender is not valid!!!") },
                                                            { companyService.update(company.value.id, it)})
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                post("attach-file") {
                    val attachFileToPeopleCompany = call.receive<AttachFileToPeopleCompany>()
                    val peopleCompanyId = PeopleCompanyId(attachFileToPeopleCompany.peopleCompanyId.value)
                    when (val peopleCompany = peopleCompanyService.findBy(peopleCompanyId)) {
                        is None -> call.respond(
                            status = HttpStatusCode.NotFound,
                            "No peopleCompany with id ${attachFileToPeopleCompany.peopleCompanyId.value}"
                        )
                        is Some -> {
                            when(val attempt = peopleCompanyService.attachFile(peopleCompany.value, attachFileToPeopleCompany)) {
                                is Either.Left -> call.respond(status = HttpStatusCode.Conflict, attempt.value)
                                is Either.Right -> call.respond(status = HttpStatusCode.OK, mapOf("value" to "File was uploaded succesfully"))
                            }
                            coroutineScope {
                                launch(Dispatchers.Default) {
                                    println("There are not rejected files")
                                    if(!peopleCompanyService.anyFileRejected(peopleCompanyId) && !companyService.anyFileRejected(peopleCompany.value.companyId)) {
                                        peopleCompanyService.update(peopleCompanyId, AttachmentState.InReview)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            withPermission(PermissionCode.UptPC) {
                put("{peopleCompanyId}") {
                    val id = call.parameters["peopleCompanyId"] ?: return@put call.respondText(
                        "Missing or malformed peopleCompanyId",
                        status = HttpStatusCode.BadRequest
                    )
                    val peopleCompany = call.receive<PeopleCompany>()

                    when(peopleCompanyService.findBy(PeopleCompanyId(id))) {
                        is None -> call.respond(HttpStatusCode.BadRequest,  PeopleCompanyErrors.PeopleCompanyDoesNotExists.gen("PeopleCompany with id: ${id} does not exist"))
                        is Some -> {
                            val peopleCompanies = peopleCompanyService.findAllBy(peopleCompany.peopleId)
                            val peopleCompanyStarDateConflict = peopleCompanies.filter { it.id.value != id && (it.startDate.isEqual(peopleCompany.startDate) ||
                                    (peopleCompany.startDate.isAfter(it.startDate.minusMonths(1)) &&
                                            peopleCompany.startDate.isBefore(it.startDate.plusMonths(1)))) }

                                if(peopleCompanyStarDateConflict.isNotEmpty()) {
                                    call.respond(HttpStatusCode.BadRequest,
                                        PeopleCompanyErrors.StartDateConflict.gen(
                                            "PeopleCompany StartDate conflict: The startDate difference must be at least by one month: " +
                                                    "${peopleCompanyStarDateConflict.map{ "Company: ${it.companyId.value} - CurrentStartDate: ${it.startDate} - RequestStartDate: ${peopleCompany.startDate}" }}")
                                    )
                                } else {
                                    peopleCompanyService.update(PeopleCompanyId(id), peopleCompany)
                                    call.respond(HttpStatusCode.Accepted)
                                }
                            }
                        }
                    }
                }
            withPermission(PermissionCode.WtVal) {
                put("{peopleCompanyId}/{state}") {
                    val id = call.parameters["peopleCompanyId"] ?: return@put call.respondText(
                        "Missing or malformed peopleCompanyId",
                        status = HttpStatusCode.BadRequest
                    )

                    val stateValue = call.parameters["state"] ?: return@put call.respondText(
                        "Missing or malformed peopleCompanyId",
                        status = HttpStatusCode.BadRequest
                    )

                    if(stateValue != AttachmentState.NonPaid.name && stateValue != AttachmentState.InReview.name && stateValue != AttachmentState.Rejected.name) {
                        call.respond(HttpStatusCode.BadRequest,  PeopleCompanyErrors.PeopleCompanyInvalidState.gen("The state: ${stateValue} is invalid, please provide the valid one"))
                    } else {
                        val state = AttachmentState.valueOf(stateValue)
                        val peopleCompanyId = PeopleCompanyId(id)
                        when(peopleCompanyService.findBy(PeopleCompanyId(id))) {
                            is None -> call.respond(HttpStatusCode.BadRequest,  PeopleCompanyErrors.PeopleCompanyDoesNotExists.gen("PeopleCompany with id: ${id} does not exist"))
                            is Some -> {
                                peopleCompanyService.update(peopleCompanyId, state)
                                call.respond(HttpStatusCode.Accepted,  mapOf("value" to "State was updated correctly"))
                            }
                        }
                    }
                }
                put("{peopleCompanyId}/reopen") {
                    val id = call.parameters["peopleCompanyId"] ?: return@put call.respondText(
                        "Missing or malformed peopleCompanyId",
                        status = HttpStatusCode.BadRequest
                    )

                    when(val peopleCompany = peopleCompanyService.findBy(PeopleCompanyId(id))) {
                        is None -> call.respond( status = HttpStatusCode.BadRequest, PeopleCompanyErrors.PeopleCompanyDoesNotExists.gen("PeopleCompany with id: ${id} does not exist"))
                        is Some -> {
                            when (val valueList = listService.findBy(ListKey(peopleCompany.value.currentMonthApplied.list))
                                .filter { it.name == peopleCompany.value.currentMonthApplied.findNext() }
                                .singleOrNone()) {
                                is None -> call.respond( status = HttpStatusCode.BadRequest, PeopleCompanyErrors.ReopenErrors.gen("Reopen is not possible"))
                                is Some -> {
                                    val updated = peopleCompanyService.update(peopleCompany.value.id, peopleCompany.value.copy(currentMonthApplied = valueList.value))
                                        .also {
                                            peopleCompanyService.update(peopleCompany.value.id, AttachmentState.InReview)
                                        }
                                    if(updated)
                                        call.respond(status = HttpStatusCode.Accepted, mapOf("value" to "Reopen was successful executed"))
                                    else
                                        call.respond(status = HttpStatusCode.Accepted, PeopleCompanyErrors.ReopenErrors.gen("Reopen is not possible, because people company could not update"))
                                }
                            }
                        }
                    }
                }
            }
            withPermission(PermissionCode.DltPC) {
                delete("archive/{peopleCompanyId}") {
                    val id = call.parameters["peopleCompanyId"] ?: return@delete call.respondText(
                        "Missing or malformed peopleCompanyId",
                        status = HttpStatusCode.BadRequest
                    )
                    coroutineScope {
                        launch(Dispatchers.Default) {
                            when(val pc = peopleCompanyService.findBy(PeopleCompanyId(id))) {
                                is None -> logger.error("There is not peopleCompany for this id: $id")
                                is Some -> peopleService.findBy(pc.value.peopleId)
                                    .flatMap { p ->
                                        companyService
                                            .findBy(pc.value.companyId)
                                            .flatMap { c -> c.minusGender(p) }
                                    }
                                    .fold({ logger.error("People: ${pc.value.peopleId} does not exist... Imposible or Gender is not valid!!!") },
                                        { companyService.update(pc.value.companyId, it)})
                            }
                        }
                    }
                    call.respond(status = HttpStatusCode.Accepted, peopleCompanyService.delete(PeopleCompanyId(id)))
                }
            }


        }
    }
}