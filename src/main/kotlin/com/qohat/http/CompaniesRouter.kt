package com.qohat.http

import arrow.core.*
import com.qohat.domain.*
import com.qohat.error.CompanyErrors
import com.qohat.features.AuthUser
import com.qohat.features.withPermission
import com.qohat.services.CompanyService
import com.qohat.services.PeopleCompanyService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun Route.companiesRouting(service: CompanyService, peopleCompanyService: PeopleCompanyService) {
    authenticate {
        route("/companies") {
            withPermission(PermissionCode.RdC) {
                get {
                    when(val principal = call.principal<AuthUser>().toOption()) {
                        is None -> call.respond(
                            status = HttpStatusCode.Unauthorized,
                            "Principal is not authorized for doing this action"
                        )
                        is Some -> call.respond(service.findAllBy(CcfId(principal.value.ccf)))
                    }
                }
                get("{id}") {
                    //TODO VALIDATE UUID Structure
                    val id = call.parameters["id"] ?: return@get call.respondText(
                        "Missing or malformed id",
                        status = HttpStatusCode.BadRequest
                    )
                    when(val company = service.findBy(CompanyId(id))) {
                        is None -> call.respond(
                            status = HttpStatusCode.NotFound,
                            "No company with id $id"
                        )
                        is Some -> call.respond(company.value)
                    }
                }
            }
            withPermission(PermissionCode.WtC) {
                post {
                    val company = call.receive<Company>()
                    when(val dbDocumentCompany = service.findBy(CompanyDocument(company.document))){
                        is Some -> call.respond(
                            HttpStatusCode.BadRequest,
                            CompanyErrors.DuplicatedDocument.gen("Already exists an ${ if (dbDocumentCompany.value.active) "active" else "inactive"} " +
                                    "company with the document: ${company.document}")
                        )
                        is None -> when(val dbEmailCompany = service.findBy(CompanyEmail(company.email))) {
                            is Some -> call.respond(
                                HttpStatusCode.BadRequest,
                                CompanyErrors.DuplicatedEmail.gen("Already exists an ${ if (dbEmailCompany.value.active) "active" else "inactive"} " +
                                        "company with the email: ${company.email}")
                            )
                            is None -> when(val dbNameCompany = service.findBy(CompanyName(company.name))) {
                                is Some -> call.respond(
                                    HttpStatusCode.BadRequest,
                                    CompanyErrors.DuplicatedName.gen("Already exists an ${ if (dbNameCompany.value.active) "active" else "inactive"} " +
                                            "company with the name: ${company.name}")
                                )
                                is None -> {
                                    when(val principal = call.principal<AuthUser>().toOption()) {
                                        is None -> call.respond(
                                            status = HttpStatusCode.Unauthorized,
                                            "Principal is not authorized for doing this action"
                                        )
                                        is Some -> call.respond(status = HttpStatusCode.Created, service.save(company.copy(createdBy = UserId(principal.value.id))))
                                    }
                                }
                            }
                        }
                    }
                }
                post("attach-file") {
                    val attachFileToCompany = call.receive<AttachFileToCompany>()
                    val companyId = CompanyId(attachFileToCompany.companyId.value)
                    when(val company = service.findBy(companyId)) {
                        is None -> call.respond(
                            status = HttpStatusCode.NotFound,
                            "No company with id ${attachFileToCompany.companyId.value}"
                        )
                        is Some -> {
                            when(val attempt = service.attachFile(company.value, attachFileToCompany)) {
                                is Either.Left -> call.respond(status = HttpStatusCode.Conflict, attempt.value)
                                is Either.Right -> call.respond(status = HttpStatusCode.OK, mapOf("value" to "File was uploaded succesfully"))
                            }
                            coroutineScope {
                                launch(Dispatchers.IO) {
                                    if(!service.anyFileRejected(companyId)) {
                                        println("There are not rejected files")
                                        peopleCompanyService.findAllLite(companyId).filterNot { peopleCompanyService.anyFileRejected(it.id) }
                                            .map { peopleCompanyService.update(it.id, AttachmentState.InReview) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            withPermission(PermissionCode.UptC) {
                put("{id}") {
                    val id = call.parameters["id"] ?: return@put call.respondText(
                        "Missing or malformed id",
                        status = HttpStatusCode.BadRequest
                    )
                    val company = call.receive<Company>()
                    if(service.update(CompanyId(id), company))
                        call.respond(status = HttpStatusCode.Accepted, "User updated correctly")
                    else
                        call.respond(
                            status = HttpStatusCode.NotFound,
                            "Unable to update because there is no company with id $id"
                        )
                }
            }
            withPermission(PermissionCode.DltC) {
                delete("{id}") {
                    val id = call.parameters["id"] ?: return@delete call.respondText(
                        "Missing or malformed id",
                        status = HttpStatusCode.BadRequest
                    )
                    if(service.delete(CompanyId(id)))
                        call.respond(status = HttpStatusCode.Accepted, "Company removed correctly")
                    else
                        call.respond(status = HttpStatusCode.NotFound,
                            "Unable to delete because there is no company with id $id"
                        )
                }
            }
        }
    }
}