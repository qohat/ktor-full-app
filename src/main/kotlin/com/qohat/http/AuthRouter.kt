package com.qohat.http

import arrow.core.continuations.either
import com.qohat.domain.UserAttempt
import com.qohat.features.AuthUser
import com.qohat.services.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRouting(authService: AuthService) {
    route("/auth") {
        post("/login") {
            respond(
                either {
                    val userAttempt = receiveCatching<UserAttempt>().bind()
                    val user = authService.userAttempt(userAttempt).bind()
                    authService.authenticate(user)
                },
                HttpStatusCode.OK
            )
        }

        authenticate {
            post("/logout") {
                val principal = call.principal<AuthUser>()
                val username = principal?.fullName
                val expiresAt = principal?.expiresAt?.time?.minus(System.currentTimeMillis())
                call.respondText("Hello, $username! Token is expired at $expiresAt ms.")
            }
        }
    }
}