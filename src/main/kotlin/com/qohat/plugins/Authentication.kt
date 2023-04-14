package com.qohat.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.qohat.config.AppConfig
import com.qohat.features.AuthUser
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import java.util.UUID

fun Application.configureAuthentication(appConfig: AppConfig) {
    install(Authentication) {
        jwt {
            realm = appConfig.jwtConfig.realm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(appConfig.jwtConfig.secret))
                    .withAudience(appConfig.jwtConfig.audience)
                    .withIssuer(appConfig.jwtConfig.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("scopes").asString() != "") {
                    AuthUser(
                        id = UUID.fromString(credential.payload.getClaim("id").asString()),
                        email = credential.payload.getClaim("email").asString(),
                        fullName = credential.payload.getClaim("fullName").asString(),
                        scopes = credential.payload.getClaim("scopes").asString(),
                        role = credential.payload.getClaim("role").asString(),
                        ccf = credential.payload.getClaim("ccf").asInt(),
                        expiresAt = credential.expiresAt
                    )
                } else {
                    null
                }
            }
        }
    }
}