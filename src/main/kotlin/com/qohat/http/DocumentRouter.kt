package com.qohat.http

import arrow.core.continuations.either
import com.qohat.config.FilesConfig
import com.qohat.domain.Name
import com.qohat.domain.NewImportPayment
import com.qohat.domain.NewPaymentRecord
import com.qohat.domain.NotPaidReportRecord
import com.qohat.domain.PermissionCode
import com.qohat.error.EmptyOrInvalidHeaderFileError
import com.qohat.features.withPermission
import com.qohat.infra.Files
import com.qohat.infra.S3ClientI
import com.qohat.infra.SESClient
import com.qohat.repo.ListRepo
import com.qohat.repo.PaymentRepo
import com.qohat.repo.PeopleRequestRepo
import com.qohat.repo.ViewsRepo
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.docsRouting(s3Client: S3ClientI,
                      filesConfig: FilesConfig,
                      viewsRepo: ViewsRepo,
                      paymentRepo: PaymentRepo,
                      peopleRequestRepo: PeopleRequestRepo,
                      sesClient: SESClient,
                      listRepo: ListRepo
) {
    authenticate {
        route("/document") {
            withPermission(PermissionCode.ImptRpts) {
                post("/billreturns-payments") {
                    withPrincipal { authUser ->
                        respond(
                            either {
                                val file = receiveCatching<NewImportPayment>().bind()
                                val lines = String(Files.decode(file.content.value), Charsets.UTF_8).lines()
                                ensure(lines.isNotEmpty() && lines.first().trim() == NewPaymentRecord.header) { EmptyOrInvalidHeaderFileError }
                                launchImportPayments(authUser, lines, viewsRepo, paymentRepo, peopleRequestRepo, s3Client, filesConfig, sesClient)
                            }, HttpStatusCode.Accepted
                        )
                    }
                }
                post("/billreturns-not-paid-report") {
                    withPrincipal { authUser ->
                        respond(
                            either {
                                val file = receiveCatching<NewImportPayment>().bind()
                                val lines = String(Files.decode(file.content.value), Charsets.UTF_8).lines()
                                ensure(lines.isNotEmpty() && lines.first().trim() == NotPaidReportRecord.header) { EmptyOrInvalidHeaderFileError }
                                launchImportNotPaidReport(authUser, lines, viewsRepo, listRepo, paymentRepo, peopleRequestRepo, s3Client, filesConfig)
                            }, HttpStatusCode.Accepted
                        )
                    }
                }
            }
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