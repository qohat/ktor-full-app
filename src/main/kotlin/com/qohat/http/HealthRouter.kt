package com.qohat.http

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.healthRouting() {
    get("/health") {
        call.respond(status = HttpStatusCode.OK, "Status Ok")
    }
}