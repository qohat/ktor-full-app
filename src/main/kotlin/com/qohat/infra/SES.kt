package com.qohat.infra

import arrow.core.Either
import com.qohat.config.SmtpConfig
import com.qohat.error.DomainError
import com.qohat.error.SendEmailError
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.retry.RetryPolicy
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy
import software.amazon.awssdk.core.retry.conditions.SdkRetryCondition
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.Body
import software.amazon.awssdk.services.ses.model.Content
import software.amazon.awssdk.services.ses.model.Destination
import software.amazon.awssdk.services.ses.model.Message
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import java.time.Duration

data class EmailRequest(val to: EmailDestination, val content: EmailContent, val emailType: EmailType)
data class MapEmailRequest(val map: EmailMap, val to: EmailDestination, val content: EmailContent, val emailType: EmailType)

@JvmInline
value class EmailContent(val value: String) {
    companion object {
        val empty = EmailContent("Empty")
    }
}
@JvmInline
value class EmailDestination(val value: String)
data class EmailMap(val value: Map<EmailVar, String>)

enum class EmailVar(val value: String) {
    Token("%token%"),
    Host("%host%"),
    Name("%name%"),
    Value("%value%"),
    PaymentType("%paymentType%")
}

enum class EmailType(val template: String, val subject: String) {
    RecoverPassword("recover-password.html", "Recuperación de Contraseña"),
    RequestPaid("request-paid.html", "Pago Confirmado")
}

interface SESClient {
    suspend fun send(email: EmailRequest): Either<DomainError, Unit>
}

class EmailClient(private val smtpConfig: SmtpConfig): SESClient {
    private val charset = "UTF-8"

    private val logger: Logger = LoggerFactory.getLogger("SESClient")

    private val retryPolicy = RetryPolicy.builder()
        .numRetries(3) // Retry up to 3 times
        .backoffStrategy(
            FullJitterBackoffStrategy
                .builder()
                .baseDelay(Duration.ofMillis(500))
                .maxBackoffTime(Duration.ofSeconds(5))
                .build()
        )
        .retryCondition(SdkRetryCondition.DEFAULT)
        .build()

    private val clientConfiguration = ClientOverrideConfiguration.builder()
        .apiCallTimeout(Duration.ofSeconds(60)) // Increase timeout to 30 seconds
        .retryPolicy(retryPolicy)
        .build()

    private val sesClient = SesClient.builder()
        .region(Region.US_EAST_1)
        .overrideConfiguration(clientConfiguration)
        .build()

    override suspend fun send(email: EmailRequest): Either<DomainError, Unit> =
        Either.catch {
            sesClient.use {
                it.sendEmail(email.toSendEmailRequest())
            }.also { logger.info("Email sent correctly") }
        }
        .tapLeft { e -> logger.error("Failed sending email", e) }
        .mapLeft { SendEmailError }
        .void()

    private fun EmailRequest.toSendEmailRequest(): SendEmailRequest {
        val htmlContent = Content.builder()
            .data(content.value.replace(EmailVar.Host.value, smtpConfig.host))
            .charset(charset)
            .build()

        val subjectContent = Content.builder()
            .data(emailType.subject)
            .build()

        val body = Body
            .builder()
            .html(htmlContent)
            .build()

        val message = Message.builder()
            .subject(subjectContent)
            .body(body)
            .build()

        val destination = Destination.builder()
            .toAddresses(to.value)
            .build()

        return SendEmailRequest.builder()
            .destination(destination)
            .message(message)
            .source(smtpConfig.from)
            .build()
    }
}