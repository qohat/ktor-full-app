package com.qohat.http

import arrow.core.continuations.either
import com.qohat.domain.Name
import com.qohat.domain.NewPaymentRecord
import com.qohat.domain.NotPaidReportRecord
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.docsRouting() {
    authenticate {
        route("/document") {
            get("/template") {
                respondWithFile(
                    either {
                        "${NewPaymentRecord.header}\n${NewPaymentRecord.samples}".toByteArray(Charsets.UTF_8)
                    }, HttpStatusCode.OK, Name("template.txt")
                )
            }
            get("/template-not-paid-report") {
                respondWithFile(
                    either {
                        "${NotPaidReportRecord.header}\n${NotPaidReportRecord.samples}".toByteArray(Charsets.UTF_8)
                    }, HttpStatusCode.OK, Name("template-not-paid-report.txt")
                )
            }
        }
    }
}