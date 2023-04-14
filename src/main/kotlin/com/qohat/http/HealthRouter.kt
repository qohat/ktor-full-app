package com.qohat.http

import com.qohat.domain.PaginationParams
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.healthRouting() {
    get("/health") {
        call.respond(status = HttpStatusCode.OK, "Status Ok")
    }
}