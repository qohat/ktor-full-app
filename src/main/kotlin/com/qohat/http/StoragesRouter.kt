package com.qohat.http

import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import com.qohat.domain.PermissionCode
import com.qohat.domain.Storage
import com.qohat.domain.StorageId
import com.qohat.error.StorageNotFoundError
import com.qohat.features.withPermission
import com.qohat.repo.StorageRepo
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.storagesRouter(repo: StorageRepo) {
    authenticate {
        route("/storages") {
            withPermission(PermissionCode.RdStg) {
                get {
                    paginated {
                        respond(
                            either {
                                repo.findAll(it).bind()
                            }, HttpStatusCode.OK
                        )
                    }
                }

                get("/{storageId}") {
                    respond(
                        either {
                            val storageId = receiveParamCatching("storageId") { StorageId.unApply(it) }.bind()
                            val maybeStorage = repo.findBy(storageId).bind()
                            ensureNotNull(maybeStorage) { StorageNotFoundError(storageId) }
                        }, HttpStatusCode.OK
                    )
                }
            }
            withPermission(PermissionCode.WtStg) {
                post {
                    respond(
                        either {
                            val storage = receiveCatching<Storage>().bind()
                            repo.save(storage).bind()
                        }, HttpStatusCode.Created
                    )
                }
            }
            withPermission(PermissionCode.UptStg) {
                put("/{storageId}") {
                    respond(
                        either {
                            val storageId = receiveParamCatching("storageId") { StorageId.unApply(it) }.bind()
                            val storage = receiveCatching<Storage>().bind()
                            val maybeStorage = repo.findBy(storageId).bind()
                            ensureNotNull(maybeStorage) { StorageNotFoundError(storageId) }
                            repo.update(storageId, storage).bind()
                        }, HttpStatusCode.Accepted
                    )
                }
            }
            withPermission(PermissionCode.DltStg) {
                delete("/{storageId}") {
                    respond(
                        either {
                            val storageId = receiveParamCatching("storageId") { StorageId.unApply(it) }.bind()
                            val maybeStorage = repo.findBy(storageId).bind()
                            ensureNotNull(maybeStorage) { StorageNotFoundError(storageId) }
                            repo.delete(storageId).bind()
                        }, HttpStatusCode.Accepted
                    )
                }
            }
        }
    }
}