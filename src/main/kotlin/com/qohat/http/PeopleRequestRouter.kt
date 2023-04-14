package com.qohat.http

import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import arrow.core.traverse
import arrow.fx.coroutines.parTraverseEither
import com.qohat.domain.AttachmentId
import com.qohat.domain.AttachmentIds
import com.qohat.domain.AttachmentPath
import com.qohat.domain.AttachmentState
import com.qohat.domain.BillReturnRequest
import com.qohat.domain.NewPeopleId
import com.qohat.domain.PeopleRequestId
import com.qohat.domain.PermissionCode
import com.qohat.domain.RequestState
import com.qohat.domain.RequestType
import com.qohat.domain.UserId
import com.qohat.domain.ableToChange
import com.qohat.domain.canBeCreated
import com.qohat.domain.requests.BillReturnDetails
import com.qohat.domain.requests.BillReturnId
import com.qohat.domain.requests.UpdateBillReturn
import com.qohat.domain.requests.withPaymentProspect
import com.qohat.domain.validateUnit
import com.qohat.entities.ConfigName
import com.qohat.error.ConfigValueNotFound
import com.qohat.error.InvalidAttachmentState
import com.qohat.error.NumberOfRequestsExceeded
import com.qohat.error.PeopleRequestNotFound
import com.qohat.error.ProductNotFoundError
import com.qohat.error.RequestStateChangeError
import com.qohat.features.withPermission
import com.qohat.infra.S3ClientI
import com.qohat.repo.ConfigRepo
import com.qohat.repo.PeopleRequestRepo
import com.qohat.repo.ProductRepo
import com.qohat.services.AssignmentService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.peopleRequestRouting(peopleRequestRepo: PeopleRequestRepo,
                               productRepo: ProductRepo,
                               configRepo: ConfigRepo,
                               s3Client: S3ClientI,
                               assignmentService: AssignmentService) {
    authenticate {
        route("/people-requests/bill-return") {
            withPermission(PermissionCode.WtPR) {
                post {
                    respond(
                        either {
                            val request = receiveCatching<BillReturnRequest>().bind()
                            val supplies = request.billReturn.supplies.validateUnit().bind()
                            val config = configRepo.findBy(ConfigName("maximum_requests_per_product")).bind()
                            ensureNotNull(config) { ConfigValueNotFound("maximum_requests_per_product") }
                            val peopleRequests = peopleRequestRepo.findAllBy(request.peopleRequest.peopleId, RequestType.BILL_RETURN_REQUEST).bind()
                            val product = productRepo.findBy(request.billReturn.product).bind()
                            ensureNotNull(product) { ProductNotFoundError }
                            ensure(request.canBeCreated(config, peopleRequests, product)) { NumberOfRequestsExceeded }
                            val response = peopleRequestRepo.save(request).bind()
                            peopleRequestRepo.saveSupplies(response.billReturnId, supplies).bind()
                            val path = AttachmentPath("${request.peopleRequest.peopleId.value}/bill-returns/${response.billReturnId.value}")
                            val attachments = request.billReturn.attachments
                                .map { it.copy(path = path) }
                                .parTraverseEither { s3Client.upload(it) }
                                .bind()
                            peopleRequestRepo.saveAttachments(response.billReturnId, attachments).bind()
                            launchAssignment(assignmentService, response.peopleRequestId).bind()
                            response
                        }, HttpStatusCode.Created
                    )
                }
            }
            withPermission(PermissionCode.RdPR) {
                get("{requestType}/people/{peopleId}") {
                    respond(
                        either {
                            val peopleId = receiveParamCatching("peopleId") { NewPeopleId.unApply(it) }.bind()
                            val type = receiveParamCatching("requestType") { RequestType.unApply(it) }.bind()
                            val peopleRequests = peopleRequestRepo.findAllBy(peopleId, type).bind()
                            peopleRequests.traverse { pr ->
                                peopleRequestRepo.findExpirations(pr.id).map { pr.copy(expirations = it) }
                            }.bind()
                        }, HttpStatusCode.OK
                    )
                }
                get("{state}/{requestType}") {
                    paginated { params ->
                        withPrincipal {
                            respond(
                                either {
                                    val state = receiveParamCatching("state") { RequestState.unApply(it) }.bind()
                                    val type = receiveParamCatching("requestType") { RequestType.unApply(it) }.bind()
                                    val peopleRequests = peopleRequestRepo.findAll(state, type, params, UserId(it.id)).bind()
                                    peopleRequests.traverse { pr ->
                                        peopleRequestRepo.findExpirations(pr.id).map { pr.copy(expirations = it) }
                                    }.bind()
                                }, HttpStatusCode.OK
                            )
                        }
                    }
                }

                get("{id}/{requestType}/show") {
                    respond(
                        either {
                            val id = receiveParamCatching("id") { PeopleRequestId.unApply(it) }.bind()
                            val type = receiveParamCatching("requestType") { RequestType.unApply(it) }.bind()
                            val maybePeopleReq = peopleRequestRepo.findBy(id, type).bind()
                            ensureNotNull(maybePeopleReq) { PeopleRequestNotFound }
                            val expirations = peopleRequestRepo.findExpirations(maybePeopleReq.id).bind()
                            maybePeopleReq.copy(expirations = expirations)
                        }, HttpStatusCode.OK
                    )
                }

                get("{billReturnId}/details") {
                    respond(
                        either {
                            val id = receiveParamCatching("billReturnId") { BillReturnId.unApply(it) }.bind()
                            val product = peopleRequestRepo.findProductBy(id).bind()
                            ensureNotNull(product) { ProductNotFoundError }
                            val supplies = peopleRequestRepo.findSuppliesBy(id).bind()
                            val attachments = peopleRequestRepo.findAttachmentsBy(id).bind()
                            BillReturnDetails(product, supplies, attachments)
                        }, HttpStatusCode.OK
                    )
                }
            }
            withPermission(PermissionCode.RdPRTp) {
                get("{billReturnId}/to-pay") {
                    respond(
                        either {
                            val id = receiveParamCatching("billReturnId") { BillReturnId.unApply(it) }.bind()
                            val configs = configRepo.findAll().bind()
                            val product = peopleRequestRepo.findProductBy(id).bind()
                            ensureNotNull(product) { ProductNotFoundError }
                            val supplies = peopleRequestRepo.findSuppliesBy(id).bind()
                            val attachments = peopleRequestRepo.findAttachmentsBy(id).bind()
                            BillReturnDetails(product, supplies, attachments).withPaymentProspect(configs).bind()
                        }, HttpStatusCode.OK
                    )
                }
            }
            withPermission(PermissionCode.RdPRNf) {
                get("{state}/{requestType}/non-user-filtered") {
                    paginated { params ->
                        respond(
                            either {
                                val state = receiveParamCatching("state") { RequestState.unApply(it) }.bind()
                                val type = receiveParamCatching("requestType") { RequestType.unApply(it) }.bind()
                                val peopleRequests = peopleRequestRepo.findAll(state, type, params).bind()
                                peopleRequests.traverse { pr ->
                                    peopleRequestRepo.findExpirations(pr.id).map { pr.copy(expirations = it) }
                                }.bind()
                            }, HttpStatusCode.OK
                        )
                    }
                }
            }
            withPermission(PermissionCode.UptPRSt) {
                patch ("{id}/update/{newState}") {
                    respond(
                        either {
                            val id = receiveParamCatching("id") { PeopleRequestId.unApply(it) }.bind()
                            val newState = receiveParamCatching("newState") { RequestState.unApply(it) }.bind()
                            ensure(newState.ableToChange()) { RequestStateChangeError }
                            peopleRequestRepo.update(id, newState).bind()
                        }, HttpStatusCode.Accepted
                    )
                }
            }
            withPermission(PermissionCode.UptPR) {
                patch ("{billReturnId}/{attachmentId}/{attachmentState}") {
                    respond(
                        either {
                            val billReturnId = receiveParamCatching("billReturnId") { BillReturnId.unApply(it) }.bind()
                            val attachmentId = receiveParamCatching("attachmentId") { AttachmentId.unApply(it) }.bind()
                            val attachmentState = receiveParamCatching("attachmentState") { AttachmentState.unApply(it) }.bind()
                            peopleRequestRepo.update(billReturnId, attachmentId, attachmentState).bind()
                            launchUpdateAttempt(peopleRequestRepo, billReturnId, RequestState.from(attachmentState)).bind()
                        }, HttpStatusCode.Accepted
                    )
                }
                put ("{billReturnId}/{attachmentState}") {
                    respond(
                        either {
                            val billReturnId = receiveParamCatching("billReturnId") { BillReturnId.unApply(it) }.bind()
                            val attachmentState = receiveParamCatching("attachmentState") { AttachmentState.unApply(it) }.bind()
                            val attachmentIds = receiveCatching<AttachmentIds>().bind()
                            peopleRequestRepo.update(billReturnId, attachmentIds, attachmentState).bind()
                            launchUpdateAttempt(peopleRequestRepo, billReturnId, RequestState.from(attachmentState)).bind()
                        }, HttpStatusCode.Accepted
                    )
                }
                put ("{id}/{billReturnId}/fix-billreturn/{requestType}/{attachmentState}") {
                    respond(
                        either {
                            val peopleRequestId = receiveParamCatching("id") { PeopleRequestId.unApply(it) }.bind()
                            val billReturnId = receiveParamCatching("billReturnId") { BillReturnId.unApply(it) }.bind()
                            val type = receiveParamCatching("requestType") { RequestType.unApply(it) }.bind()
                            val attachmentState = receiveParamCatching("attachmentState") { AttachmentState.unApply(it) }.bind()
                            val peopleRequest = peopleRequestRepo.findBy(peopleRequestId, type).bind()
                            val updateBillReturn = receiveCatching<UpdateBillReturn>().bind()
                            ensureNotNull(peopleRequest) { PeopleRequestNotFound }
                            if(updateBillReturn.supplies.isNotEmpty()) {
                                val supplies = updateBillReturn.supplies.validateUnit().bind()
                                peopleRequestRepo.deleteSupplies(billReturnId).bind()
                                peopleRequestRepo.saveSupplies(billReturnId, supplies).bind()
                            }
                            if(updateBillReturn.attachments.isNotEmpty()) {
                                ensure(updateBillReturn.attachments.all { it.state == attachmentState }) { InvalidAttachmentState(updateBillReturn.attachments.map { it.state }, attachmentState) }
                                val path = AttachmentPath("${peopleRequest.people.id?.value}/bill-returns/${peopleRequest.billReturnId.value}")
                                val attachments = updateBillReturn.attachments
                                    .map { it.copy(path = path) }
                                    .parTraverseEither { s3Client.upload(it) }
                                    .bind()
                                peopleRequestRepo.saveAttachments(billReturnId, attachments).bind()
                                launchUpdateAttempt(peopleRequestRepo, billReturnId, RequestState.from(attachmentState)).bind()
                            }
                            peopleRequestRepo.update(billReturnId, updateBillReturn).bind()
                        }, HttpStatusCode.Accepted
                    )
                }
            }
        }
    }
}