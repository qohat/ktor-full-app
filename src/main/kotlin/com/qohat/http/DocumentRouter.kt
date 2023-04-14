package com.qohat.http

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.andThen
import arrow.core.continuations.either
import arrow.core.invalidNel
import arrow.core.tail
import arrow.core.toOption
import arrow.core.valid
import arrow.core.validNel
import arrow.core.zip
import arrow.typeclasses.Semigroup
import com.qohat.config.FilesConfig
import com.qohat.domain.AttachmentState
import com.qohat.domain.ImportPayment
import com.qohat.domain.ImportRecord
import com.qohat.domain.InvalidImportRecord
import com.qohat.domain.Name
import com.qohat.domain.NewImportPayment
import com.qohat.domain.NewPaymentRecord
import com.qohat.domain.NotPaidReportRecord
import com.qohat.domain.PaymentDate
import com.qohat.domain.PaymentId
import com.qohat.domain.PaymentImportRecord
import com.qohat.domain.PaymentMonth
import com.qohat.domain.PaymentNumber
import com.qohat.domain.PaymentYear
import com.qohat.domain.PermissionCode
import com.qohat.domain.UserId
import com.qohat.error.EmptyOrInvalidHeaderFileError
import com.qohat.error.ImportErrors
import com.qohat.error.ServiceError
import com.qohat.features.AuthUser
import com.qohat.features.withPermission
import com.qohat.infra.Files
import com.qohat.infra.S3ClientI
import com.qohat.infra.S3KeyObject
import com.qohat.infra.SESClient
import com.qohat.repo.ListRepo
import com.qohat.repo.PaymentRepo
import com.qohat.repo.PeopleRequestRepo
import com.qohat.repo.ViewsRepo
import com.qohat.services.PaymentService
import com.qohat.services.PeopleCompanyService
import com.qohat.services.ValuesService
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID

fun Route.docsRouting(
    peopleCompanyService: PeopleCompanyService,
    valuesService: ValuesService,
    paymentService: PaymentService,
    s3Client: S3ClientI,
    filesConfig: FilesConfig,
    viewsRepo: ViewsRepo,
    paymentRepo: PaymentRepo,
    peopleRequestRepo: PeopleRequestRepo,
    sesClient: SESClient,
    listRepo: ListRepo) {
    val logger: Logger = LoggerFactory.getLogger("DocumentsRouting")
    authenticate {
        route("/document") {
            withPermission(PermissionCode.ExptRpts) {
                get("/people-company-report") {
                    when(val principal = call.principal<AuthUser>().toOption()) {
                        is None -> call.respond(
                            status = HttpStatusCode.Unauthorized,
                            "Principal is not authorized for doing this action"
                        )
                        is Some -> coroutineScope {
                                val csvTempPath = "csv"
                                val file = async(Dispatchers.IO) {
                                    val peopleCompanies = peopleCompanyService.findAllValidationBy(AttachmentState.NonPaid, UserId(principal.value.id))
                                    if(peopleCompanies.isNotEmpty()) {
                                        val header = "Tercero,Nombre,Valor giro a la empresa,Bco_Benef,Cta_Benef,Tpo_Cta_Benef,Observaciones,Valor giro joven,Nombres,Apellidos,Tipo De Documento,Número de Documento,CCF\n"
                                        val rows = peopleCompanies.joinToString("\n") { it.toPayment(valuesService) }
                                        val tempFileName = "${UUID.randomUUID()}.csv"
                                        val directory = File(csvTempPath)
                                        if (!directory.exists()) {
                                            directory.mkdirs()
                                        }
                                        File("${csvTempPath}/${tempFileName}")
                                            .writeBytes("${header}${rows}".toByteArray())
                                            .run {
                                                Either.Right(tempFileName)
                                            }
                                    } else {
                                        Either.Left(Unit)
                                    }
                                }

                                file.await().run {
                                    when(this) {
                                        is Either.Right -> {
                                            val file = File("$csvTempPath/${this.value}")
                                            call.response.header(
                                                HttpHeaders.ContentDisposition,
                                                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "Liquidación Fiduciaria.csv")
                                                    .toString()
                                            )
                                            call.respondFile(file).also {
                                                file.delete()
                                            }
                                        }
                                        is Either.Left -> call.respondText("There are not data to export", status = HttpStatusCode.NotFound)
                                    }
                                }
                            }
                    }
                }
                get("/all-people-company-report") {
                    coroutineScope {
                        val csvTempPath = "csv"
                        val file = async(Dispatchers.IO) {
                            val peopleCompanies = peopleCompanyService.findAllValidationBy(AttachmentState.NonPaid)
                            if(peopleCompanies.isNotEmpty()) {
                                val header = "Tercero,Nombre,Valor giro a la empresa,Bco_Benef,Cta_Benef,Tpo_Cta_Benef,Observaciones,Valor giro joven,Nombres,Apellidos,Tipo De Documento,Número de Documento,CCF\n"
                                val rows = peopleCompanies.joinToString("\n") { it.toPayment(valuesService) }
                                val tempFileName = "${UUID.randomUUID()}.csv"
                                val directory = File(csvTempPath)
                                if (!directory.exists()) {
                                    directory.mkdirs()
                                }
                                File("${csvTempPath}/${tempFileName}")
                                    .writeBytes("${header}${rows}".toByteArray())
                                    .run {
                                        Either.Right(tempFileName)
                                    }
                            } else {
                                Either.Left(Unit)
                            }
                        }

                        file.await().run {
                            when(this) {
                                is Either.Right -> {
                                    val file = File("$csvTempPath/${this.value}")
                                    call.response.header(
                                        HttpHeaders.ContentDisposition,
                                        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "Liquidación Fiduciaria.csv")
                                            .toString()
                                    )
                                    call.respondFile(file).also {
                                        file.delete()
                                    }
                                }
                                is Either.Left -> call.respondText("There are not data to export", status = HttpStatusCode.NotFound)
                            }
                        }
                    }
                }
                get("/people-company-validation-report") {
                    when(val principal = call.principal<AuthUser>().toOption()) {
                        is None -> call.respond(
                            status = HttpStatusCode.Unauthorized,
                            "Principal is not authorized for doing this action"
                        )
                        is Some -> coroutineScope {
                            val csvTempPath = "csv"
                            val file = async(Dispatchers.IO) {
                                val peopleCompanies = peopleCompanyService.findAllValidationBy(AttachmentState.InReview, UserId(principal.value.id)) + peopleCompanyService.findAllValidationBy(AttachmentState.Rejected, UserId(principal.value.id))
                                if(peopleCompanies.isNotEmpty()) {
                                    val header = "Empresa,Nit,Nombres,Apellidos,Tipo De Documento,Documento,Fecha Expedición,Mes que aplica la solicitud,Nivel de riesgo ARL,Salario,Banco,No. Cuenta,Tipo,Valor a Girar\n"
                                    val rows = peopleCompanies.joinToString("\n") { it.toString(valuesService) }
                                    val tempFileName = "${UUID.randomUUID()}.csv"
                                    val directory = File(csvTempPath)
                                    if (!directory.exists()) {
                                        directory.mkdirs()
                                    }
                                    File("${csvTempPath}/${tempFileName}")
                                        .writeBytes("${header}${rows}".toByteArray())
                                        .run {
                                            Either.Right(tempFileName)
                                        }
                                } else {
                                    Either.Left(Unit)
                                }
                            }

                            file.await().run {
                                when(this) {
                                    is Either.Right -> {
                                        val file = File("$csvTempPath/${this.value}")
                                        call.response.header(
                                            HttpHeaders.ContentDisposition,
                                            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "Validaciones Fiduciaria.csv")
                                                .toString()
                                        )
                                        call.respondFile(file).also {
                                            file.delete()
                                        }
                                    }
                                    is Either.Left -> call.respondText("There are not data to export", status = HttpStatusCode.NotFound)
                                }
                            }
                        }
                    }
                }
            }
            withPermission(PermissionCode.ImptRpts) {
                post("payments") {
                    val importPayment = either<ServiceError, ImportPayment> {
                        Either.catch { call.receive<ImportPayment>() }
                            .mapLeft { ImportErrors.InvalidImportRequest.gen("The request is invalid") }
                            .bind()
                    }

                    when(importPayment) {
                        is Either.Left -> call.respond(status = HttpStatusCode.BadRequest, importPayment.value)
                        is Either.Right -> {
                            val stringList = String(Files.decode(importPayment.value.content), Charsets.UTF_8).lines()
                            if(stringList.isEmpty()) {
                                call.respond(status = HttpStatusCode.BadRequest, ImportErrors.EmptyFileError.gen("The file is empty"))
                            } else if(stringList.first().trim() != "Nit,Documento,Mes,Anualidad,Fecha,Nomina") {
                                call.respond(status = HttpStatusCode.BadRequest, ImportErrors.HeaderFileError.gen("The file header is not valid"))
                            } else {
                                call.respond(status = HttpStatusCode.Accepted, mapOf("value" to "File accepted and start being processed"))
                                coroutineScope {
                                    launch(Dispatchers.Unconfined) {
                                        val mapGroup = stringList.tail().toSet().map { it.trim().split(",", ";") }
                                            .filter { it.isNotEmpty() }
                                            .mapIndexed { i, list -> validateLine(list, i) }
                                            .groupBy { it.key }

                                        val errors = mapGroup
                                            .filter { it.key == "Error" }
                                            .mapValues { it.value.map { error -> error as InvalidImportRecord } }
                                            .values
                                            .flatten()
                                            .map { it.message }

                                        val noErrors = mapGroup.filterNot { it.key.contains("Error") }

                                        val singleImports = noErrors
                                            .filter { it.value.size == 1 }
                                            .mapValues { it.value.first() as PaymentImportRecord }
                                            .values
                                            .asFlow()
                                            .map {
                                                val payment: Either<String, PaymentId> = either {
                                                    val action = paymentService.save(it).bind()
                                                    val pc = peopleCompanyService.findBy(action.first)
                                                        .toEither { "Error updating state, PeopleCompany doesn't exists" }
                                                        .bind()
                                                    if(pc.state != AttachmentState.Completed) {
                                                        paymentService.toPaid(action.first)
                                                    }
                                                    action.second
                                                }
                                                when(payment) {
                                                    is Either.Left -> "Error en fila ${it.line + 2}, ${payment.value}"
                                                    is Either.Right -> "El pago fue guardado exitósamente con PaymentId = ${payment.value}"
                                                }
                                            }
                                            .filter { it.contains("Error") }
                                            .flowOn(Dispatchers.Default)
                                            .toList()

                                        val multipleImports = noErrors
                                            .filter { it.value.size > 1 }
                                            .mapValues { it.value.map { record -> record as PaymentImportRecord } }
                                            .values
                                            .flatten()
                                            .asFlow()
                                            .map {
                                                val payment: Either<String, PaymentId> = either {
                                                    val action = paymentService.save(it).bind()
                                                    paymentService.updateCurrentMonth(action.first).bind()
                                                    action.second
                                                }

                                                when(payment) {
                                                    is Either.Left -> "Error en fila ${it.line + 2}, ${payment.value}"
                                                    is Either.Right -> "El pago fue guardado exitósamente con PaymentId = ${payment.value.value}"
                                                }
                                            }
                                            .filter { it.contains("Error") }
                                            .flowOn(Dispatchers.Default)
                                            .toList()

                                        val lists = (errors + singleImports + multipleImports).joinToString("\n") { it }

                                        if(lists.isNotEmpty()) {
                                            println(lists)
                                            when(val principal = call.principal<AuthUser>().toOption()) {
                                                is None -> logger.error("The pricipal does not exists")
                                                is Some -> {
                                                    logger.info("Uploading to S3 the import report...")
                                                    val localDate = LocalDate.now()
                                                    val s3KeyObject = S3KeyObject("${filesConfig.imported}/${localDate.year}-${localDate.monthValue}-${localDate.dayOfMonth}/${principal.value.email}/${LocalTime.now()}.txt")
                                                    val content = lists.toByteArray(Charsets.UTF_8)
                                                    s3Client.putS3Object(s3KeyObject, content)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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

fun ValidatedNel<String, String>.validatePaymentYear(): ValidatedNel<String, PaymentYear> =
    andThen {
        try {
            PaymentYear(it.toIntOrNull()?: 0).valid()
        } catch (e: Exception) {
            "Año: $it no es un número o no está entre 2021 y 2022.".invalidNel()
        }
    }

fun ValidatedNel<String, String>.validatePaymentMonth(): ValidatedNel<String, PaymentMonth> =
    andThen {
        try {
            PaymentMonth(it).valid()
        } catch (e: Exception) {
            "Mes: $it no es un mes válido.".invalidNel()
        }
    }

fun ValidatedNel<String, String>.validatePaymentDate(): ValidatedNel<String, PaymentDate> =
    andThen {
        try {
            val date = LocalDate.parse(it, DateTimeFormatter.ISO_DATE)
            PaymentDate(date).valid()
        } catch (e: Exception) {
            "Fecha: $it no es una fecha válida.".invalidNel()
        }
    }

fun ValidatedNel<String, String>.validatePaymentNumber(): ValidatedNel<String, PaymentNumber> =
    andThen {
        try {
            PaymentNumber(it.toIntOrNull()?: 0).valid()
        } catch (e: Exception) {
            "Número de nómina: $it no es un valor válido.".invalidNel()
        }
    }

fun ValidatedNel<String, String>.validateNotPresent(
    notPresent: () -> String
): ValidatedNel<String, String> =
    andThen {
        if(it.isEmpty()) notPresent().invalidNel() else it.valid()
    }

data class DeclaJur(val path: String, val nombre: String, val documento: String, val nit: String)

fun saveOnDownloaded(d: DeclaJur) {
    File("logs/downloaded.txt").appendText("${d.documento},${d.nit}\n")
}

fun validateLine(list: List<String>, i: Int): ImportRecord {
    val validation =
        list[0].validNel().validateNotPresent { "Documento de la empresa es vacío" }
        .zip(Semigroup.nonEmptyList(),
            list[1].validNel().validateNotPresent { "Documento de joven es vacío" },
            list[2].validNel().validatePaymentMonth(),
            list[3].validNel().validatePaymentYear(),
            list[4].validNel().validatePaymentDate(),
            list[5].validNel().validatePaymentNumber()
        ){nit, document, paymentMonth, paymentYear, paymentDate, paymentNumber ->
            PaymentImportRecord(nit, document, paymentMonth, paymentYear, paymentDate, paymentNumber, i, "$nit-$document") }

    return when(validation) {
        is Validated.Invalid -> InvalidImportRecord(validation.value.joinToString(prefix = "Error en fila ${i + 2}, Mensaje: "), "Error")
        is Validated.Valid -> validation.value
    }
}