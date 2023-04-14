package com.qohat.http

import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import at.favre.lib.crypto.bcrypt.BCrypt
import com.qohat.domain.EncryptedPassword
import com.qohat.domain.Password
import com.qohat.domain.PermissionChain
import com.qohat.domain.PermissionCode
import com.qohat.domain.RecoverPasswordRequest
import com.qohat.domain.RoleName
import com.qohat.domain.User
import com.qohat.domain.UserEmail
import com.qohat.domain.UserId
import com.qohat.domain.confirmEmailAndPassword
import com.qohat.error.InvalidRoleError
import com.qohat.error.UnableToRecover
import com.qohat.error.UserNotFound
import com.qohat.features.withPermission
import com.qohat.infra.EmailContent
import com.qohat.infra.EmailDestination
import com.qohat.infra.EmailMap
import com.qohat.infra.EmailType
import com.qohat.infra.EmailVar
import com.qohat.infra.MapEmailRequest
import com.qohat.infra.SESClient
import com.qohat.repo.RoleRepo
import com.qohat.repo.UserRepo
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.userRouting(userRepo: UserRepo, emailClient: SESClient, roleRepo: RoleRepo) {
    route("/user") {
        post("recover-password/{email}") {
            respond(either {
                val email = receiveParamCatching("email") { UserEmail(it) }.bind()
                ensureNotNull(userRepo.findBy(email)) { UserNotFound }
                val recoverToken = userRepo.askRecoverToken(email)
                val mapEmailRequest = MapEmailRequest(
                    map = EmailMap(mapOf(EmailVar.Token to recoverToken.value.toString())),
                    to = EmailDestination(email.value),
                    content = EmailContent.empty,
                    emailType = EmailType.RecoverPassword
                )
                attemptSendEmail(mapEmailRequest, emailClient).bind()
            }, HttpStatusCode.Accepted)
        }
        post("renew-password/{email}") {
            respond(
                either {
                    val email = receiveParamCatching("email") { UserEmail(it) }.bind()
                    val request = receiveCatching<RecoverPasswordRequest>().bind()
                    val maybeUser = userRepo.findBy(email).bind()
                    val user = ensureNotNull(maybeUser) { UserNotFound }
                    ensure(userRepo.canRecoverPassword(user.id, request.token)) {
                        UnableToRecover("The user:${user.id} can't recover password with token: ${request.token.value}")
                    }
                    val encryptedPassword = EncryptedPassword(encryptPassword(request.password))
                    userRepo.update(user.id, encryptedPassword)
                }, HttpStatusCode.OK)
        }
        authenticate {
            withPermission(PermissionCode.RdU) {
                get {
                    call.respond(userRepo.findAll())
                }
                get("{id}") {
                    respond(
                        either {
                            val id =  receiveParamCatching("id"){ UserId.unApply(it) }.bind()
                            ensureNotNull(userRepo.findBy(id)) { UserNotFound }
                        }, HttpStatusCode.OK)
                }
            }
            withPermission(PermissionCode.WtU) {
                post {
                    respond(
                        either {
                            val user = receiveCatching<User>().bind()
                            val confirmedUser = user.confirmEmailAndPassword().bind()
                            val validRole = roleRepo.findBy(confirmedUser.role.id).bind()
                            ensureNotNull(validRole) { InvalidRoleError(confirmedUser.role.name.value) }
                            userRepo.save(confirmedUser)
                        }, HttpStatusCode.Created)
                }
            }
            withPermission(PermissionCode.UptU) {
                put("{id}") {
                    respond(
                        either {
                            val id =  receiveParamCatching("id"){ UserId.unApply(it) }.bind()
                            ensureNotNull(userRepo.findBy(id)) { UserNotFound }
                            val user = receiveCatching<User>().bind()
                            userRepo.update(id, user)
                        }, HttpStatusCode.Accepted
                    )
                }
            }

            withPermission(PermissionCode.DltU) {
                delete("{id}") {
                    respond(
                        either {
                            val id =  receiveParamCatching("id"){ UserId.unApply(it) }.bind()
                            ensureNotNull(userRepo.findBy(id)) { UserNotFound }
                            userRepo.delete(id)
                        }, HttpStatusCode.Accepted
                    )
                }
            }
        }
    }
}
private fun encryptPassword(password: Password) =
    BCrypt.withDefaults().hashToString(12, password.value.toCharArray())