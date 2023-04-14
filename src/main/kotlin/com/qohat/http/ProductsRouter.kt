package com.qohat.http

import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import com.qohat.domain.Name
import com.qohat.domain.PermissionCode
import com.qohat.domain.Product
import com.qohat.domain.ProductId
import com.qohat.error.ProductNotFoundError
import com.qohat.features.withPermission
import com.qohat.repo.ProductRepo
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.productRouter(repo: ProductRepo) {
    route("/products") {
        authenticate {
            withPermission(PermissionCode.RdPrd) {
                get {
                    paginated {
                        respond(
                            either {
                                repo.findAll(it).bind()
                            }, HttpStatusCode.OK
                        )
                    }
                }
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
                get("/{productId}") {
                    respond(
                        either {
                            val productId = receiveParamCatching("productId") { ProductId.unApply(it) }.bind()
                            val maybeProduct = repo.findBy(productId).bind()
                            ensureNotNull(maybeProduct) { ProductNotFoundError }
                        }, HttpStatusCode.OK
                    )
                }
            }
            withPermission(PermissionCode.WtPrd) {
                post {
                    respond(
                        either {
                            val product = receiveCatching<Product>().bind()
                            repo.save(product).bind()
                        }, HttpStatusCode.Created
                    )
                }
            }
            withPermission(PermissionCode.UptPrd) {
                put("/{productId}") {
                    respond(
                        either {
                            val productId = receiveParamCatching("productId") { ProductId.unApply(it) }.bind()
                            val product = receiveCatching<Product>().bind()
                            val maybeProduct = repo.findBy(productId).bind()
                            ensureNotNull(maybeProduct) { ProductNotFoundError }
                            repo.update(productId, product).bind()
                        }, HttpStatusCode.Accepted
                    )
                }
            }
            withPermission(PermissionCode.DltPrd) {
                delete("/{productId}") {
                    respond(
                        either {
                            val productId = receiveParamCatching("productId") { ProductId.unApply(it) }.bind()
                            val maybeProduct = repo.findBy(productId).bind()
                            ensureNotNull(maybeProduct) { ProductNotFoundError }
                            repo.delete(productId).bind()
                        }, HttpStatusCode.Accepted
                    )
                }
            }
        }
    }
}