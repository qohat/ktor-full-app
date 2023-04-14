package com.qohat.http

import com.qohat.domain.ListKey
import com.qohat.repo.ListRepo
import com.qohat.services.ListService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.listRouting(listService: ListService) {
    route("/list") {
        get("{list}") {
            val listKey = call.parameters["list"] ?: return@get call.respondText(
                "Missing or malformed id",
                status = HttpStatusCode.BadRequest
            )
            call.respond(listService.findBy(ListKey(listKey)))
        }
    }
}