package com.qohat.http

import arrow.core.continuations.either
import com.qohat.domain.AttachmentState
import com.qohat.domain.AttachmentsValidationEvent
import com.qohat.domain.AttachmentsValidationEvents
import com.qohat.domain.NewUserId
import com.qohat.domain.PermissionCode
import com.qohat.domain.UserId
import com.qohat.domain.requests.BillReturnId
import com.qohat.domain.requests.BillReturnObservation
import com.qohat.features.withPermission
import com.qohat.repo.EventRepoV2
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.eventRouting(eventRepoV2: EventRepoV2) {
    authenticate {
        route("/events/bill-returns") {
            withPermission(PermissionCode.WtPR) {
                post("{attachmentState}") {
                    withPrincipal {
                        respond(
                            either {
                                val attachmentState = receiveParamCatching("attachmentState") { AttachmentState.unApply(it) }.bind()
                                val event = receiveCatching<AttachmentsValidationEvent>().bind()
                                val eventWithAuthUser = event.copy(userId = NewUserId(it.id), state = attachmentState)
                                eventRepoV2.saveBillReturnsEvent(eventWithAuthUser).bind()
                            }, HttpStatusCode.Created
                        )
                    }
                }
                post("{attachmentState}/bulk") {
                    withPrincipal { authUser ->
                        respond(
                            either {
                                val attachmentState = receiveParamCatching("attachmentState") { AttachmentState.unApply(it) }.bind()
                                val events = receiveCatching<AttachmentsValidationEvents>().bind()
                                val eventsWithAuthUser = events.value.map { it.copy(userId = NewUserId(authUser.id), state = attachmentState) }
                                eventRepoV2.saveBillReturnsEvents(eventsWithAuthUser).bind()
                            }, HttpStatusCode.Created
                        )
                    }
                }
            }
            withPermission(PermissionCode.RdPR) {
                get("{billReturnId}") {
                    respond(
                        either {
                            val billReturnId = receiveParamCatching("billReturnId") {BillReturnId.unApply(it) }.bind()
                            eventRepoV2.findAllBy(billReturnId).bind()
                        }, HttpStatusCode.OK
                    )
                }
            }
            withPermission(PermissionCode.RdObs) {
                get("{billReturnId}/observations") {
                    respond(
                        either {
                            val billReturnId = receiveParamCatching("billReturnId") {BillReturnId.unApply(it) }.bind()
                            eventRepoV2.findAllObservations(billReturnId).bind()
                        }, HttpStatusCode.OK
                    )
                }
            }
            withPermission(PermissionCode.WtObs) {
                post("{billReturnId}/observations") {
                    withPrincipal { authUser ->
                        respond(
                            either {
                                val billReturnId = receiveParamCatching("billReturnId") {BillReturnId.unApply(it) }.bind()
                                val request = receiveCatching<BillReturnObservation>().bind()
                                val observation = request.copy(billReturnId = billReturnId, userId = UserId(authUser.id))
                                eventRepoV2.save(observation).bind()
                            }, HttpStatusCode.OK
                        )
                    }
                }
            }
        }
    }

}