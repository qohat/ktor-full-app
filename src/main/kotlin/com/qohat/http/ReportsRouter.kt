package com.qohat.http

import arrow.core.continuations.either
import com.qohat.domain.Name
import com.qohat.domain.PermissionCode
import com.qohat.domain.RequestState
import com.qohat.entities.bankHeader
import com.qohat.entities.toBankCsv
import com.qohat.entities.toBranchCsv
import com.qohat.error.ToPayReportIsEmpty
import com.qohat.features.withPermission
import com.qohat.repo.ViewsRepo
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.reportsRouting(viewsRepo: ViewsRepo) {
    authenticate {
        withPermission(PermissionCode.RdRpts) {
            route("reports") {
                get("requests-to-pay") {
                    respondWithFile(
                        either {
                            val balances = viewsRepo.specificBalance(RequestState.NonPaid).bind()
                            val bankHeader = balances.bankHeader()
                            val bankRows = balances.filter { it.accountNumber != null }.joinToString("\n") { it.toBankCsv() }
                            val branchHeader = balances.bankHeader()
                            val branchRows = balances.filter { it.branch != null }.joinToString("\n") { it.toBranchCsv() }
                            ensure(bankRows.isNotEmpty() || branchRows.isNotEmpty()) { ToPayReportIsEmpty }
                            val mutableList = mutableListOf<Pair<Name, ByteArray>>()
                            if(bankRows.isNotEmpty()) {
                                mutableList.add(
                                    Pair(
                                        Name("liquidacion por transferencia.csv"),
                                        "${bankHeader}\n${bankRows}".toByteArray(Charsets.UTF_8)
                                    )
                                )
                            }
                            if(branchRows.isNotEmpty()) {
                                mutableList.add(
                                    Pair(
                                        Name("liquidacion por sucursal.csv"),
                                        "${branchHeader}\n${branchRows}".toByteArray(Charsets.UTF_8)
                                    )
                                )
                            }
                            createZipArchive(files = mutableList.toList())
                        }, HttpStatusCode.OK, Name("liquidaciones.zip")
                    )
                }
            }
        }
    }
}