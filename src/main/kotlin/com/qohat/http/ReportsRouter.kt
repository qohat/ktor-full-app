package com.qohat.http

import CompaniesAmountDrawn
import PaymentsApprovals
import PaymentsCommissionReport
import PaymentsMade
import PeopleAmountsDrawn
import PeopleRelatedToCompanies
import PeopleWithPaymentsCount
import UsersDoingProcess
import arrow.core.None
import arrow.core.Some
import arrow.core.continuations.either
import arrow.core.firstOrNone
import arrow.core.singleOrNone
import com.qohat.domain.CompanyAmountDrawn
import com.qohat.domain.Name
import com.qohat.domain.PaymentApprovals
import com.qohat.domain.PaymentCommission
import com.qohat.domain.PaymentMade
import com.qohat.domain.PeopleAmountDrawn
import com.qohat.domain.PeopleRelatedToCompany
import com.qohat.domain.PeopleWithPaymentCount
import com.qohat.domain.PermissionCode
import com.qohat.domain.RequestState
import com.qohat.domain.UserDoingProcess
import com.qohat.entities.bankHeader
import com.qohat.entities.toBankCsv
import com.qohat.entities.toBranchCsv
import com.qohat.error.ExportErrors
import com.qohat.error.ToPayReportIsEmpty
import com.qohat.features.withPermission
import com.qohat.repo.ViewsRepo
import com.qohat.services.CompanyService
import com.qohat.services.PeopleCompanyService
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.io.File
import java.util.UUID

fun Route.reportsRouting(peopleCompanyService: PeopleCompanyService,
                         companyService: CompanyService, viewsRepo: ViewsRepo) {
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

                get("people-with-payment-count") {
                    val peopleWithPaymentCount = newSuspendedTransaction(Dispatchers.IO) {
                        PeopleWithPaymentsCount.selectAll().singleOrNone().map { toPeopleWithPaymentCount(it) }
                    }

                    when(peopleWithPaymentCount) {
                        is None -> call.respond(PeopleWithPaymentCount(0, 0, 0))
                        is Some -> call.respond(peopleWithPaymentCount.value)
                    }
                }

                get("companies-amount-drawns") {
                    coroutineScope {
                        val csvTempPath = "csv"
                        val companiesAmountDrawn = newSuspendedTransaction(Dispatchers.IO) {
                            CompaniesAmountDrawn.selectAll().map { toCompanyAmountDrawn(it) }
                        }
                        if(companiesAmountDrawn.isNotEmpty()) {
                            val file = async(Dispatchers.Default) {
                                val header = "Nombre,Total Girado\n"
                                val rows = companiesAmountDrawn.joinToString("\n") { it.toString() }
                                val tempFileName = "${UUID.randomUUID()}.csv"
                                val directory = File(csvTempPath)
                                if (!directory.exists()) {
                                    directory.mkdirs()
                                }
                                File("${csvTempPath}/${tempFileName}")
                                    .writeBytes("${header}${rows}".toByteArray())
                                    .run {
                                        tempFileName
                                    }
                            }

                            file.await().run {
                                val file = File("$csvTempPath/${this}")
                                call.response.header(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "Monto Girado a Empresas.csv")
                                        .toString()
                                )
                                call.respondFile(file).also {
                                    file.delete()
                                }
                            }
                        } else {
                            call.respond(status = HttpStatusCode.BadRequest, ExportErrors.NotFoundDataError.gen("There are not data to export"))
                        }
                    }
                }

                get("people-amount-drawns") {
                    coroutineScope {
                        val csvTempPath = "csv"
                        val peopleAmountDrawn = newSuspendedTransaction(Dispatchers.IO) {
                            PeopleAmountsDrawn.selectAll().map { toPeopleAmountDrawn(it) }
                        }
                        if(peopleAmountDrawn.isNotEmpty()) {
                            val file = async(Dispatchers.Default) {
                                val header = "Nombre,Documento,Total Girado\n"
                                val rows = peopleAmountDrawn.joinToString("\n") { it.toString() }
                                val tempFileName = "${UUID.randomUUID()}.csv"
                                val directory = File(csvTempPath)
                                if (!directory.exists()) {
                                    directory.mkdirs()
                                }
                                File("${csvTempPath}/${tempFileName}")
                                    .writeBytes("${header}${rows}".toByteArray())
                                    .run {
                                        tempFileName
                                    }
                            }

                            file.await().run {
                                val file = File("$csvTempPath/${this}")
                                call.response.header(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "Monto Girado a Jovenes.csv")
                                        .toString()
                                )
                                call.respondFile(file).also {
                                    file.delete()
                                }
                            }
                        } else {
                            call.respond(status = HttpStatusCode.BadRequest, ExportErrors.NotFoundDataError.gen("There are not data to export"))
                        }
                    }
                }

                get("people-related-to-companies") {
                    coroutineScope {
                        val csvTempPath = "csv"
                        val peopleRelatedToCompany = newSuspendedTransaction(Dispatchers.IO) {
                            PeopleRelatedToCompanies.selectAll().map { toPeopleRelatedToCompanies(it) }
                        }
                        if(peopleRelatedToCompany.isNotEmpty()) {
                            val file = async(Dispatchers.Default) {
                                val header = "Nombre Joven,Documento,Empresa\n"
                                val rows = peopleRelatedToCompany.joinToString("\n") { it.toString() }
                                val tempFileName = "${UUID.randomUUID()}.csv"
                                val directory = File(csvTempPath)
                                if (!directory.exists()) {
                                    directory.mkdirs()
                                }
                                File("${csvTempPath}/${tempFileName}")
                                    .writeBytes("${header}${rows}".toByteArray())
                                    .run {
                                        tempFileName
                                    }
                            }

                            file.await().run {
                                val file = File("$csvTempPath/${this}")
                                call.response.header(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "Jóvenes Asociados a Empresas.csv")
                                        .toString()
                                )
                                call.respondFile(file).also {
                                    file.delete()
                                }
                            }
                        } else {
                            call.respond(status = HttpStatusCode.BadRequest, ExportErrors.NotFoundDataError.gen("There are not data to export"))
                        }
                    }
                }

                get("users-doing-process") {
                    coroutineScope {
                        val csvTempPath = "csv"
                        val userDoingProcess = newSuspendedTransaction(Dispatchers.IO) {
                            UsersDoingProcess.selectAll().map { toUserDoingProcess(it) }
                        }
                        if(userDoingProcess.isNotEmpty()) {
                            val file = async(Dispatchers.Default) {
                                val header = "Creador del Proceso, Caja de Compensación, Asignado a, Nombre Joven, Documento, Empresa\n"
                                val rows = userDoingProcess.joinToString("\n") { it.toString() }
                                val tempFileName = "${UUID.randomUUID()}.csv"
                                File("${csvTempPath}/${tempFileName}")
                                    .writeBytes("${header}${rows}".toByteArray())
                                    .run {
                                        tempFileName
                                    }
                            }

                            file.await().run {
                                val file = File("$csvTempPath/${this}")
                                call.response.header(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "Usuarios Que Realizan Procesos.csv")
                                        .toString()
                                )
                                call.respondFile(file).also {
                                    file.delete()
                                }
                            }
                        } else {
                            call.respond(status = HttpStatusCode.BadRequest, ExportErrors.NotFoundDataError.gen("There are not data to export"))
                        }
                    }
                }

                get("payments-made") {
                    val paymentsMade = newSuspendedTransaction(Dispatchers.IO) {
                        PaymentsMade.selectAll().firstOrNone().map { toPaymentMade(it) }
                    }
                    when(paymentsMade) {
                        is None -> call.respond(PaymentMade(0))
                        is Some -> call.respond(paymentsMade.value)
                    }
                }

                get("payments-approvals") {
                    coroutineScope {
                        val csvTempPath = "csv"
                        val paymentsApprovals = newSuspendedTransaction(Dispatchers.IO) {
                            PaymentsApprovals.selectAll().map { toPaymentApproval(it) }
                        }
                        if(paymentsApprovals.isNotEmpty()) {
                            val file = async(Dispatchers.Default) {
                                val header = "Número de identificación Empresa,Nombre de la Empresa,Tipo documento joven,Número de identificación joven,Primer Nombre,Segundo Nombre,Primer apellido,Segundo apellido,Fecha de aprobación del Joven Fiducia,Nivel Riesgo ARL,Valor del giro,Numero de giro,Mes calendario del giro\n"
                                val rows = paymentsApprovals.joinToString("\n") { it.toString() }
                                val tempFileName = "${UUID.randomUUID()}.csv"
                                val directory = File(csvTempPath)
                                if (!directory.exists()) {
                                    directory.mkdirs()
                                }
                                File("${csvTempPath}/${tempFileName}")
                                    .writeBytes("${header}${rows}".toByteArray())
                                    .run {
                                        tempFileName
                                    }
                            }

                            file.await().run {
                                val file = File("$csvTempPath/${this}")
                                call.response.header(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "Monto Girado a Empresas.csv")
                                        .toString()
                                )
                                call.respondFile(file).also {
                                    file.delete()
                                }
                            }
                        } else {
                            call.respond(status = HttpStatusCode.BadRequest, ExportErrors.NotFoundDataError.gen("There are not data to export"))
                        }
                    }
                }

                get("payments-commision") {
                    coroutineScope {
                        val csvTempPath = "csv"
                        val paymentCommission = newSuspendedTransaction(Dispatchers.IO) {
                            PaymentsCommissionReport.selectAll().map { toPaymentCommission(it) }
                        }
                        if(paymentCommission.isNotEmpty()) {
                            val file = async(Dispatchers.Default) {
                                val header = "Número de identificación Empresa,Nombre de la Empresa,Tipo documento joven,Número de identificación joven,Primer Nombre,Segundo Nombre,Primer apellido,Segundo apellido,Fecha de pago,Nivel Riesgo ARL,Valor del giro,Numero de giro,Mes calendario del giro\n"
                                val rows = paymentCommission.joinToString("\n") { it.toString() }
                                val tempFileName = "${UUID.randomUUID()}.csv"
                                val directory = File(csvTempPath)
                                if (!directory.exists()) {
                                    directory.mkdirs()
                                }
                                File("${csvTempPath}/${tempFileName}")
                                    .writeBytes("${header}${rows}".toByteArray())
                                    .run {
                                        tempFileName
                                    }
                            }

                            file.await().run {
                                val file = File("$csvTempPath/${this}")
                                call.response.header(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "Monto Girado a Empresas.csv")
                                        .toString()
                                )
                                call.respondFile(file).also {
                                    file.delete()
                                }
                            }
                        } else {
                            call.respond(status = HttpStatusCode.BadRequest, ExportErrors.NotFoundDataError.gen("There are not data to export"))
                        }
                    }
                }

                get("paid-spins") {
                    coroutineScope {
                        val csvTempPath = "csv"
                        val peopleCompany = peopleCompanyService.findAllValidationAtLeastOnePaid()
                        if(peopleCompany.isNotEmpty()) {
                            val file = async(Dispatchers.IO) {
                                val header = "Número de identificación Empresa,Nombre de la Empresa,Tipo documento joven,Número de identificación joven,Nombres,Apellidos,Nivel Riesgo ARL,Fecha de aprobación fiducia del Joven,Fecha de primer pago,Valor del 1er pago,Fecha de segundo pago,Valor del 2do pago,Fecha del tercer pago,Valor del 3er pago,Total pagado\n"
                                val rows = peopleCompany.joinToString("\n") { it.toPaidSpin() }
                                val tempFileName = "${UUID.randomUUID()}.csv"
                                val directory = File(csvTempPath)
                                if (!directory.exists()) {
                                    directory.mkdirs()
                                }
                                File("${csvTempPath}/${tempFileName}")
                                    .writeBytes("${header}${rows}".toByteArray())
                                    .run {
                                        tempFileName
                                    }
                            }

                            file.await().run {
                                val file = File("$csvTempPath/${this}")
                                call.response.header(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "Giros Pagados.csv")
                                        .toString()
                                )
                                call.respondFile(file).also {
                                    file.delete()
                                }
                            }
                        } else {
                            call.respond(status = HttpStatusCode.BadRequest, ExportErrors.NotFoundDataError.gen("There are not data to export"))
                        }
                    }
                }

                get("full-companies") {
                    coroutineScope {
                        val csvTempPath = "csv"
                        val fullCompanies = companyService.findAll()
                        if(fullCompanies.isNotEmpty()) {
                            val file = async(Dispatchers.IO) {
                                val header = "Número de identificación Empresa,Nombre de la Empresa,Tipo de Empres,Dirección,Localidad,Barrio,Teléfono,Celular,Correo Electrónico,Número de empleados,Tamaño de Empresa,Actividad Económica,Caja de compensación,Representante Legal,Número Documento,Responsable de postulación,Cargo,Teléfono,Correo Electrónico,Entidad Bancaria,Tipo de Cuenta,N° de Cuenta\n"
                                val rows = fullCompanies.joinToString("\n") { it.toString() }
                                val tempFileName = "${UUID.randomUUID()}.csv"
                                val directory = File(csvTempPath)
                                if (!directory.exists()) {
                                    directory.mkdirs()
                                }
                                File("${csvTempPath}/${tempFileName}")
                                    .writeBytes("${header}${rows}".toByteArray())
                                    .run {
                                        tempFileName
                                    }
                            }

                            file.await().run {
                                val file = File("$csvTempPath/${this}")
                                call.response.header(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "Consolidado Empresas.csv")
                                        .toString()
                                )
                                call.respondFile(file).also {
                                    file.delete()
                                }
                            }
                        } else {
                            call.respond(status = HttpStatusCode.BadRequest, ExportErrors.NotFoundDataError.gen("There are not data to export"))
                        }
                    }
                }
            }
        }
    }
}

private fun toPeopleWithPaymentCount(row: ResultRow): PeopleWithPaymentCount =
    PeopleWithPaymentCount(
        onePayment = row[PeopleWithPaymentsCount.one_payment],
        twoPayments = row[PeopleWithPaymentsCount.two_payments],
        threePayments = row[PeopleWithPaymentsCount.three_payments]
    )

private fun toCompanyAmountDrawn(row: ResultRow): CompanyAmountDrawn =
    CompanyAmountDrawn(
        name = row[CompaniesAmountDrawn.name],
        total = row[CompaniesAmountDrawn.total]
    )

private fun toPeopleAmountDrawn(row: ResultRow): PeopleAmountDrawn =
    PeopleAmountDrawn(
        fullName = row[PeopleAmountsDrawn.fullname],
        document = row[PeopleAmountsDrawn.document],
        total = row[PeopleAmountsDrawn.total]
    )

private fun toPeopleRelatedToCompanies(row: ResultRow): PeopleRelatedToCompany =
    PeopleRelatedToCompany(
        peopleName = row[PeopleRelatedToCompanies.people_name],
        document = row[PeopleRelatedToCompanies.document],
        companyName = row[PeopleRelatedToCompanies.company_name]
    )

private fun toUserDoingProcess(row: ResultRow): UserDoingProcess =
    UserDoingProcess(
        createdBy = row[UsersDoingProcess.created_by],
        ccf = row[UsersDoingProcess.ccf],
        assignedTo = row[UsersDoingProcess.assigned_to],
        peopleName = row[UsersDoingProcess.people_name],
        document = row[UsersDoingProcess.document],
        companyName = row[UsersDoingProcess.company_name],
    )

private fun toPaymentMade(row: ResultRow): PaymentMade =
    PaymentMade(
        paymentsMade = row[PaymentsMade.payments_made]
    )

private fun toPaymentApproval(row: ResultRow): PaymentApprovals =
    PaymentApprovals(
        nit = row[PaymentsApprovals.nit],
        companyName = row[PaymentsApprovals.company_name],
        docType = row[PaymentsApprovals.doc_type],
        document = row[PaymentsApprovals.document],
        name = row[PaymentsApprovals.name],
        lastName = row[PaymentsApprovals.last_name],
        approvalDate = row[PaymentsApprovals.approval_date],
        arlLevel = row[PaymentsApprovals.arl_level],
        value = row[PaymentsApprovals.value],
        paymentNumber = row[PaymentsApprovals.payment_number].toString(),
        monthApplied = row[PaymentsApprovals.month_applied]
    )

private fun toPaymentCommission(row: ResultRow): PaymentCommission =
    PaymentCommission(
        nit = row[PaymentsCommissionReport.nit],
        companyName = row[PaymentsCommissionReport.company_name],
        docType = row[PaymentsCommissionReport.doc_type],
        document = row[PaymentsCommissionReport.document],
        name = row[PaymentsCommissionReport.name],
        lastName = row[PaymentsCommissionReport.last_name],
        paymentDate = row[PaymentsCommissionReport.payment_date],
        arlLevel = row[PaymentsCommissionReport.arl_level],
        value = row[PaymentsCommissionReport.value],
        paymentNumber = row[PaymentsCommissionReport.payment_number].toString(),
        monthApplied = row[PaymentsCommissionReport.month_applied]
    )