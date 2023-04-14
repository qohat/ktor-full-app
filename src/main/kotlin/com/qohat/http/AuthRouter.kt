package com.qohat.http

import arrow.core.Either
import arrow.core.continuations.either
import com.qohat.domain.PermissionCode
import com.qohat.domain.UserAttempt
import com.qohat.features.AuthUser
import com.qohat.features.withPermission
import com.qohat.services.AuthService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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