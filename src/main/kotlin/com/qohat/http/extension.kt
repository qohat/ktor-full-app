package com.qohat.http

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import arrow.core.flatMap
import arrow.core.identity
import arrow.core.tail
import arrow.core.toOption
import arrow.core.traverse
import arrow.fx.coroutines.parTraverseEither
import com.qohat.config.FilesConfig
import com.qohat.domain.AttachmentState
import com.qohat.domain.InvalidImportRecord
import com.qohat.domain.Limit
import com.qohat.domain.Name
import com.qohat.domain.NewPaginationParams
import com.qohat.domain.NewPayment
import com.qohat.domain.NewPaymentRecord
import com.qohat.domain.NotPaidReport
import com.qohat.domain.NotPaidReportRecord
import com.qohat.domain.Offset
import com.qohat.domain.Pagination
import com.qohat.domain.PaginationParams
import com.qohat.domain.PeopleRequestExpiration
import com.qohat.domain.PeopleRequestId
import com.qohat.domain.RequestExpiration
import com.qohat.domain.RequestState
import com.qohat.domain.RequestType
import com.qohat.domain.ResponseExpiration
import com.qohat.domain.Text
import com.qohat.domain.UserId
import com.qohat.domain.requests.BillReturnId
import com.qohat.error.*
import com.qohat.features.AuthUser
import com.qohat.infra.EmailContent
import com.qohat.infra.EmailDestination
import com.qohat.infra.EmailMap
import com.qohat.infra.EmailRequest
import com.qohat.infra.EmailType
import com.qohat.infra.EmailVar
import com.qohat.infra.Files
import com.qohat.infra.MapEmailRequest
import com.qohat.infra.Path
import com.qohat.infra.S3ClientI
import com.qohat.infra.S3KeyObject
import com.qohat.infra.SESClient
import com.qohat.repo.ListRepo
import com.qohat.repo.PaymentRepo
import com.qohat.repo.PeopleRequestRepo
import com.qohat.repo.ViewsRepo
import com.qohat.services.AssignmentService
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


val logger: Logger = LoggerFactory.getLogger("Extension")

@Serializable
data class GenericErrorModel(val errors: String)

/*suspend inline fun <reified A : Any> Either<DomainError, A>.respond(status: HttpStatusCode): Unit =
    when (this) {
        is Either.Left -> respond(value)
        is Either.Right -> call.respond(status, value)
    }*/

@Suppress("ComplexMethod")
suspend inline fun PipelineContext<Unit, ApplicationCall>.respondWithFile(either: Either<DomainError, ByteArray>, status: HttpStatusCode, fileName: Name): Unit =
    when (either) {
        is Either.Left -> when(val error = either.value) {
            is ToPayReportIsEmpty -> notContent("There is not request ready to pay")
            else -> internal("Can not process this.")
        }
        is Either.Right -> {
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName,  fileName.value)
                    .toString()
            )
            call.respondBytes(either.value, ContentType.Application.OctetStream, status)
        }
    }

@Suppress("ComplexMethod")
suspend inline fun <reified A : Any> PipelineContext<Unit, ApplicationCall>.respond(either: Either<DomainError, A>, status: HttpStatusCode): Unit =
    when (either) {
        is Either.Left -> when(val error = either.value) {
            is UserNotFound -> notFound("User doesn't exists")
            is PeopleRequestNotFound -> notFound("PeopleRequest Not Found")
            is InvalidSignUp -> badReq(error.errors.joinToString(","))
            is UnableToRecover -> badReq(error.message)
            is Malformed -> badReq(error.message)
            is PeopleAlreadyExist -> conflict(error.message)
            is RepoTransactionError -> badReq(error.message)
            is RequestStateError  -> badReq(error.message)
            is RequestTypeError -> badReq(error.message)
            is RequestStateChangeError -> badReq(error.toString())
            is ProductNotFoundError -> notFound("Product NotFound")
            is SupplyNotFoundError -> notFound("Supplie with Id:${error.supplyId.value} NotFound")
            is ConfigValueNotFound -> notFound("Config Value Not Found")
            is PeopleNotFoundError -> notFound("Person was not found")
            is InvalidRoleError -> badReq("The role ${error.name} is not present in the table")
            is EmailError -> badReq("Invalid Email")
            is PasswordError -> badReq("Invalid Password")
            is EmptyOrInvalidHeaderFileError -> badReq("File to import is empty")
            is InvalidMeasurementUnit -> badReq("The measurement unit: ${error.unit} is invalid")
            is PersistConfigError -> badReq(error.message)
            is NumberOfRequestsExceeded -> conflict("Number of request are being exceeded or the product are not the same")
            is InvalidPermissionError -> badReq("The permission ${error.name} doesn't belong to the available permissions")
            is InvalidAttachmentState -> badReq("The attachment list states should match with the url path, list: ${error.states.joinToString(","){it.value}}, path: ${error.state.value}")
            is Unexpected ->
                internal(
                    """
        Unexpected failure occurred:
          - description: ${error.message}
          - cause: ${error.error}
        """.trimIndent()
                )
            else -> internal("Can not process this.")
        }
        is Either.Right -> call.respond(status, either.value)
    }
suspend inline fun PipelineContext<Unit, ApplicationCall>.conflict(message: String): Unit =
    call.respond(HttpStatusCode.Conflict, GenericErrorModel("Conflict: $message" ))
suspend inline fun PipelineContext<Unit, ApplicationCall>.notFound(message: String): Unit =
    call.respond(HttpStatusCode.NotFound, GenericErrorModel(message))
suspend inline fun PipelineContext<Unit, ApplicationCall>.notContent(message: String): Unit =
    call.respond(HttpStatusCode.NoContent, GenericErrorModel(message))
suspend inline fun PipelineContext<Unit, ApplicationCall>.badReq(error: String): Unit =
    call.respond(HttpStatusCode.BadRequest, GenericErrorModel(error))
suspend inline fun PipelineContext<Unit, ApplicationCall>.internal(error: String): Unit =
    call.respond(HttpStatusCode.InternalServerError, GenericErrorModel(error))
suspend inline fun PipelineContext<Unit, ApplicationCall>.unauthorized(error: String): Unit =
    call.respond(HttpStatusCode.Unauthorized, GenericErrorModel(error))
suspend inline fun <reified A : Any> PipelineContext<Unit, ApplicationCall>.receiveCatching(): Either<DomainError, A> =
    Either.catch { call.receive<A>() }
    .tapLeft { e -> logger.error("Failed parsing request body", e) }
    .mapLeft { e ->
        Malformed(e.message ?: "Received malformed JSON for ${A::class.simpleName} - cause: ${e.cause?.message}")
    }

fun <A : Any> PipelineContext<Unit, ApplicationCall>.receiveParamCatching(param: String, transform: (v: String) -> A?): Either<DomainError, A> =
    Either.fromNullable(call.parameters[param])
        .flatMap { transform(it).toOption().toEither { "Valid param $param but can not convert to Unapply Type" } }
        .tapLeft { e -> logger.error("Failed parsing request param", e) }
        .mapLeft { e -> Malformed( "Received malformed: or invalid param: $param - Error: $e") }

fun PipelineContext<Unit, ApplicationCall>.paginated() = run {
    val limit = call.parameters["limit"]?.toIntOrNull() ?: 20
    val offset = call.parameters["offset"]?.toLongOrNull() ?: 1
    val text = call.parameters["text"] ?: ""
    val params = PaginationParams(limit, offset, text)
    params
}
suspend fun PipelineContext<Unit, ApplicationCall>.paginated(block: suspend (p: NewPaginationParams) -> Unit) = runBlocking {
    val limit = call.parameters["limit"]?.toIntOrNull()?.let { Limit(it) } ?: Limit(20)
    val offset = call.parameters["offset"]?.toLongOrNull()?.let { Offset(it) } ?: Offset(0)
    val text = call.parameters["text"]?.let {
        val newText = it.replace("\\s+".toRegex(), " ").replace("'", "")
        if(newText.isNotBlank() and newText.isNotEmpty()) {
            Text(newText)
        } else null
    }
    val params = NewPaginationParams(Pagination(limit, offset), text)
    block.invoke(params)
}

suspend fun PipelineContext<Unit, ApplicationCall>.withPrincipal(block: suspend (p: AuthUser) -> Unit) = runBlocking {
    call.principal<AuthUser>()?.let {
        block.invoke(it)
    } ?: unauthorized("The user is not authorized for executing this action")
}

suspend fun launchUpdateAttempt(repo: PeopleRequestRepo, billReturnId: BillReturnId, requestState: RequestState): Either<DomainError, Unit> = either {
    coroutineScope {
        launch(Dispatchers.Default) {
            logger.info("Attempting update global request state to ${requestState.name} of billreturn: ${billReturnId.value}")
            when(requestState) {
                RequestState.InReview, RequestState.Rejected -> {
                    val id = repo.findIdBy(billReturnId).bind()
                    logger.info("Ensuring of peopleRequest: ${id?.value}, billreturn: ${billReturnId.value}")
                    ensureNotNull(id) { RequestBackgroundStateChangeError }
                    logger.info("Updating peopleRequest: ${id.value} to : ${requestState.name}")
                    repo.update(id, requestState).bind()
                    logger.info("loading expirations peopleRequest: ${id.value}")
                    val expirations = repo.findExpirations(id).bind()
                    if(expirations.isEmpty()) {
                        val newExpiration = PeopleRequestExpiration(RequestExpiration(LocalDate.now().plusDays(15)), null)
                        repo.save(id, newExpiration)
                    } else if((expirations.size == 1 && expirations.first().responseExpiration != null)) {
                        val newExpiration = PeopleRequestExpiration(RequestExpiration(LocalDate.now().plusWorkDays(5)), null)
                        repo.save(id, newExpiration)
                    }
                }
                RequestState.RequiresValidation -> {
                    val allAttachments = repo.findAttachmentsBy(billReturnId).bind()
                    ensure(allAttachments.none { at -> at.state == AttachmentState.Rejected }) { RequestStateChangeError }
                    val id = repo.findIdBy(billReturnId).bind()
                    logger.info("Ensuring of peopleRequest: ${id?.value}, billreturn: ${billReturnId.value}")
                    ensureNotNull(id) { RequestBackgroundStateChangeError }
                    logger.info("Updating peopleRequest: ${id.value} to : ${RequestState.RequiresValidation}")
                    repo.update(id, RequestState.RequiresValidation).bind()
                    logger.info("peopleRequest: ${id.value} updated to : ${RequestState.RequiresValidation}")
                    logger.info("loading expirations peopleRequest: ${id.value}")
                    val expirations = repo.findExpirations(id).bind()
                    if(expirations.isNotEmpty() && (expirations.size == 2 && expirations.first().responseExpiration == null)) {
                        val responseExpiration = ResponseExpiration(LocalDate.now().plusDays(7))
                        repo.update(id, expirations.first().requestExpiration, responseExpiration)
                    } else if(expirations.isNotEmpty() && (expirations.size == 1 && expirations.first().responseExpiration == null)) {
                        val responseExpiration = ResponseExpiration(LocalDate.now().plusWorkDays(10))
                        repo.update(id, expirations.first().requestExpiration, responseExpiration)
                    }
                }
                RequestState.Approved -> {
                    val allAttachments = repo.findAttachmentsBy(billReturnId).bind()
                    logger.info("Ensuring attachment validations size:${allAttachments.size}")
                    ensure(allAttachments
                                .map {at -> at.state }
                                .filter { st -> st == AttachmentState.Approved }
                                .size >= 5) { RequestStateChangeError }
                    val id = repo.findIdBy(billReturnId).bind()
                    logger.info("Ensuring of peopleRequest: ${id?.value}, billreturn: ${billReturnId.value}")
                    ensureNotNull(id) { RequestBackgroundStateChangeError }
                    logger.info("Updating peopleRequest: ${id.value} to : ${RequestState.NonPaid}")
                    repo.update(id, RequestState.NonPaid).bind()
                    logger.info("peopleRequest: ${id.value} updated to : ${RequestState.NonPaid}")
                }
                else -> logger.info("${requestState.name} is not valid")
            }
        }
    }
}

fun LocalDate.plusWorkDays(workDays: Int): LocalDate {
    var workDaysRemaining = workDays
    var resultDate = this

    while (workDaysRemaining > 0) {
        resultDate = resultDate.plusDays(1)

        if (isWorkingDay(resultDate)) {
            workDaysRemaining--
        }
    }
    return resultDate
}
fun isWorkingDay(date: LocalDate): Boolean {
    val dayOfWeek = date.dayOfWeek
    return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY
}

suspend fun launchAssignment(assignmentService: AssignmentService, peopleRequestId: PeopleRequestId): Either<DomainError, Unit> = either {
    coroutineScope {
        launch(Dispatchers.Default) {
            assignmentService.assignV2(peopleRequestId).bind()
        }
    }
}

suspend fun launchImportPayments(authUser: AuthUser, lines: List<String>, viewsRepo: ViewsRepo, paymentRepo: PaymentRepo, peopleRequestRepo: PeopleRequestRepo,
                                 s3Client: S3ClientI, filesConfig: FilesConfig, sesClient: SESClient): Unit =
    coroutineScope {
        launch(Dispatchers.Default) {
            val mapGroup = lines.tail().toSet().map { it.trim().split(",", ";") }
                    .filter { it.isNotEmpty() }
                    .mapIndexed { i, list -> validateLineV2(viewsRepo, list, i) }
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
                .mapValues { it.value.first() as NewPaymentRecord }
                .values

            val newPayments = singleImports.map { NewPayment(it.billReturnId, it.date, UserId(authUser.id)) }

            val saveImports = newPayments
                .parTraverseEither {payment ->
                    paymentRepo.findBy(payment.billReturnId)
                        .flatMap {
                            it?.let { Either.Left(PaymentAlreadyExits(it)) } ?:
                            peopleRequestRepo.findIdBy(payment.billReturnId)
                            .traverseNullable { maybePrId -> identity(maybePrId) }
                            ?.flatMap { prId ->
                                paymentRepo.save(payment)
                                    .flatMap { peopleRequestRepo.update(prId, RequestState.Paid) }
                                    .flatMap { attemptSendEmail(peopleRequestRepo, viewsRepo, prId, EmailType.RequestPaid, sesClient) }
                            } ?: Either.Left(PeopleRequestNotFound)
                        }
                }
                .fold(
                    { e -> "Error intentando importat pagos: Lineas: ${singleImports.map { it.line }.joinToString(",")}, error: $e"},
                    {"${newPayments.size} Pagos guardados exitósamente, solicitudes: ${newPayments.map { "${it.billReturnId.value}"}.joinToString(",")}"}
                )

            val duplicatedErrors = noErrors
                .filter { it.value.size > 1 }
                .mapValues { it.value.map { record -> record as NewPaymentRecord } }
                .values
                .flatten()
                .map { "Error de duplicidad de pago de solicitud en fila ${it.line + 2}" }

            val lists = (errors + listOf(saveImports).filter { it.contains("Error") } + duplicatedErrors).joinToString("\n") { it }

            if(lists.isNotEmpty()) {
                logger.info("Uploading to S3 the import report...")
                val localDate = LocalDate.now()
                val s3KeyObject = S3KeyObject("${filesConfig.imported}/${localDate.year}-${localDate.monthValue}-${localDate.dayOfMonth}/${authUser.email}/${LocalTime.now()}.txt")
                val content = lists.toByteArray(Charsets.UTF_8)
                s3Client.putS3Object(s3KeyObject, content)
            }
        }
    }


suspend fun launchImportNotPaidReport(authUser: AuthUser, lines: List<String>, viewsRepo: ViewsRepo, listRepo: ListRepo, paymentRepo: PaymentRepo, peopleRequestRepo: PeopleRequestRepo,
                                 s3Client: S3ClientI, filesConfig: FilesConfig): Unit =
    coroutineScope {
        launch(Dispatchers.Default) {
            val mapGroup = lines.tail().toSet().map { it.trim().split(",", ";") }
                .filter { it.isNotEmpty() }
                .mapIndexed { i, list -> validateNotPaidLineV2(viewsRepo, listRepo, list, i) }
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
                .mapValues { it.value.first() as NotPaidReportRecord }
                .values

            val notPaids = singleImports.map { NotPaidReport(it.billReturnId, UserId(authUser.id), it.reasonId) }

            val saveImports = notPaids
                .parTraverseEither {notPayment ->
                    paymentRepo.findBy(notPayment.billReturnId)
                    .flatMap {
                        it?.let { Either.Left(PaymentAlreadyExits(it)) } ?:
                        peopleRequestRepo.findIdBy(notPayment.billReturnId)
                        .traverseNullable { maybePrId -> identity(maybePrId) }
                        ?.flatMap { prId ->
                            paymentRepo.save(notPayment)
                                .flatMap { peopleRequestRepo.update(prId, RequestState.Frozen) }
                        } ?: Either.Left(PeopleRequestNotFound)
                    }
                }
                .fold(
                    { e -> "Error intentando importar pagos: Lineas: ${singleImports.map { it.line }.joinToString(",")}, error: $e"},
                    {"${notPaids.size} Pagos guardados exitósamente, solicitudes: ${notPaids.map { "${it.billReturnId.value}"}.joinToString(",")}"}
                )

            val duplicatedErrors = noErrors
                .filter { it.value.size > 1 }
                .mapValues { it.value.map { record -> record as NotPaidReportRecord } }
                .values
                .flatten()
                .map { "Error de duplicidad de registros no pagados en fila ${it.line + 2}" }

            val lists = (errors + listOf(saveImports).filter { it.contains("Error") } + duplicatedErrors).joinToString("\n") { it }

            if(lists.isNotEmpty()) {
                logger.info("Uploading to S3 the not paid report...")
                val localDate = LocalDate.now()
                val s3KeyObject = S3KeyObject("${filesConfig.imported}/${localDate.year}-${localDate.monthValue}-${localDate.dayOfMonth}/${authUser.email}/${LocalTime.now()}-not-paid-report.txt")
                val content = lists.toByteArray(Charsets.UTF_8)
                s3Client.putS3Object(s3KeyObject, content)
            }
        }
    }

suspend fun attemptSendEmail(peopleRequestRepo: PeopleRequestRepo, viewsRepo: ViewsRepo, peopleRequestId: PeopleRequestId, emailType: EmailType, sesClient: SESClient): Either<DomainError, Unit> = either {
    val pR = peopleRequestRepo.findBy(peopleRequestId, RequestType.BILL_RETURN_REQUEST).bind()
    ensureNotNull(pR) { PeopleRequestNotFound }
    val balance = viewsRepo.findBy(pR.billReturnId).bind()
    ensureNotNull(balance) { BalanceNotFound }
    val map = mapOf(
        EmailVar.Name to "${pR.people.name} ${pR.people.lastName}",
        EmailVar.Value to "${balance.valueToSubsidize.value}",
        EmailVar.PaymentType to balance.paymentType.value
    )
    val emailMap = EmailMap(map)
    val to = EmailDestination(pR.people.email.value)
    val content = EmailContent.empty
    val mapRequest = MapEmailRequest(emailMap, to, content, emailType)
    attemptSendEmail(mapRequest, sesClient).bind()
}

suspend fun attemptSendEmail(mapEmail: MapEmailRequest, sesClient: SESClient): Either<DomainError, Unit> = either {
    coroutineScope {
        launch(Dispatchers.Default) {
            val workDir = System.getProperty("user.dir")
            val path = Path("$workDir/templates/emails/${mapEmail.emailType.template}")
            Files.from(path)
            .map {
                it.value
                    .replace(EmailVar.Token.value, mapEmail.map.value.getOrDefault(EmailVar.Token, "NoValidToken"))
                    .replace(EmailVar.Name.value, mapEmail.map.value.getOrDefault(EmailVar.Name, "NoValidName"))
                    .replace(EmailVar.Value.value, mapEmail.map.value.getOrDefault(EmailVar.Value, "NoValidValue"))
                    .replace(EmailVar.PaymentType.value, mapEmail.map.value.getOrDefault(EmailVar.PaymentType, "NoValidValue"))
            }
            .map { EmailContent(it) }
            .map { EmailRequest(mapEmail.to, it, mapEmail.emailType) }
            .flatMap { sesClient.send(it) }
        }
    }
}

fun createZipArchive(files: List<Pair<Name, ByteArray>>): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val zipOutputStream = ZipOutputStream(outputStream)

    files.forEach { (name, file) ->
        zipOutputStream.putNextEntry(ZipEntry(name.value))
        file.inputStream().copyTo(zipOutputStream)
        zipOutputStream.closeEntry()
    }
    zipOutputStream.close()
    return outputStream.toByteArray()
}
