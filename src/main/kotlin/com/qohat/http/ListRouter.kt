package com.qohat.http

import com.qohat.domain.ListKey
import com.qohat.services.ListService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

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