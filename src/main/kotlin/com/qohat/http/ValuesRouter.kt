package com.qohat.http

import arrow.core.continuations.either
import arrow.core.flatMap
import com.qohat.domain.PermissionCode
import com.qohat.entities.ConfigType
import com.qohat.entities.Configs
import com.qohat.entities.validate
import com.qohat.features.withPermission
import com.qohat.repo.ConfigRepo
import com.qohat.services.ValuesService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.valuesRouting(configRepo: ConfigRepo) {
    authenticate {
        route("values") {
            get {
                call.respond(
                    mapOf("baseSalary" to ValuesService.baseSalary.flatMap { mapOf(it.key to it.value.toString()) },
                        "arlRisk" to ValuesService.arlRisk.flatMap { mapOf(it.key to it.value.toString()) },
                        "deductiblePerc" to ValuesService.deductiblePercentage.flatMap { mapOf(it.key to it.value.toString()) })
                )
            }
            get("SMLMV") {
                call.respond(ValuesService.baseSalary)
            }
            get("ARL-RISK-LEVEL") {
                call.respond(ValuesService.arlRisk)
            }
            get("DEDUCTIBLE-PERCENTAGE") {
                call.respond(ValuesService.deductiblePercentage)
            }


            withPermission(PermissionCode.RdCf) {
                get("/config/{type}") {
                    respond(
                        either {
                            val type = receiveParamCatching("type") { ConfigType.unApply(it) }.bind()
                            configRepo.findBy(type).bind()
                        }, HttpStatusCode.OK
                    )
                }
                put("config/{type}") {
                    respond(
                        either {
                            val type = receiveParamCatching("type") { ConfigType.unApply(it) }.bind()
                            val configs = receiveCatching<Configs>().bind()
                            val configsValidated = configs.validate(type).bind()
                            configRepo.save(configsValidated).bind()
                        }, HttpStatusCode.OK
                    )
                }
            }
        }
    }
}