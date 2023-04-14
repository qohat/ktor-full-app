package com.qohat.http

import arrow.core.continuations.either
import com.qohat.domain.PermissionCode
import com.qohat.domain.Role
import com.qohat.domain.isValid
import com.qohat.repo.RoleRepo
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.Route

fun Route.roleRouter(roleRepo: RoleRepo) {
    authenticate {
        route("/roles") {
            get {
                respond(
                    either {
                        roleRepo.findAll().bind()
                    }, HttpStatusCode.OK
                )
            }

            get("/permissions") {
                respond(
                    either { PermissionCode.values().filterNot { it == PermissionCode.Empty  } }, HttpStatusCode.OK
                )
            }

            post {
                respond(
                    either {
                        val role = receiveCatching<Role>().bind()
                        val validRole = role.isValid().bind()
                        roleRepo.save(validRole).bind()
                    }, HttpStatusCode.Created
                )
            }
        }
    }
}