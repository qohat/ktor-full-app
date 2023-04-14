package com.qohat.http

import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import com.qohat.domain.Name
import com.qohat.domain.PermissionCode
import com.qohat.domain.Supply
import com.qohat.domain.SupplyId
import com.qohat.error.SupplyNotFoundError
import com.qohat.features.withPermission
import com.qohat.repo.SupplieRepo
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.suppliesRouter(repo: SupplieRepo) {
    route("/supplies") {
        authenticate {
            withPermission(PermissionCode.RdSup) {
                get("/all/{cropGroup}") {
                    respond(
                        either {
                            val name = receiveParamCatching("cropGroup") { Name(it) }.bind()
                            repo.findAllBy(name).bind()
                        }, HttpStatusCode.OK
                    )
                }
                get("/all") {
                    respond(
                        either {
                            repo.findAll().bind()
                        }, HttpStatusCode.OK
                    )
                }
                get {
                    paginated {
                        respond(
                            either {
                                repo.findAll(it).bind()
                            }, HttpStatusCode.OK
                        )
                    }
                }
                get("/{supplyId}") {
                    respond(
                        either {
                            val supplyId = receiveParamCatching("supplyId") { SupplyId.unApply(it) }.bind()
                            val maybeSupply = repo.findBy(supplyId).bind()
                            ensureNotNull(maybeSupply) { SupplyNotFoundError(supplyId) }
                        }, HttpStatusCode.OK
                    )
                }
            }
            withPermission(PermissionCode.WtSup) {
                post {
                    respond(
                        either {
                            val supply = receiveCatching<Supply>().bind()
                            repo.save(supply).bind()
                        }, HttpStatusCode.Created
                    )
                }
            }
            withPermission(PermissionCode.UptSup) {
                put("/{supplyId}") {
                    respond(
                        either {
                            val supplyId = receiveParamCatching("supplyId") { SupplyId.unApply(it) }.bind()
                            val supply = receiveCatching<Supply>().bind()
                            val maybeSupply = repo.findBy(supplyId).bind()
                            ensureNotNull(maybeSupply) { SupplyNotFoundError(supplyId) }
                            repo.update(supplyId, supply).bind()
                        }, HttpStatusCode.Accepted
                    )
                }
            }
            withPermission(PermissionCode.DltSup) {
                delete("/{supplyId}") {
                    respond(
                        either {
                            val supplyId = receiveParamCatching("supplyId") { SupplyId.unApply(it) }.bind()
                            val maybeSupply = repo.findBy(supplyId).bind()
                            ensureNotNull(maybeSupply) { SupplyNotFoundError(supplyId) }
                            repo.delete(supplyId).bind()
                        }, HttpStatusCode.Accepted
                    )
                }
            }
        }
    }
}