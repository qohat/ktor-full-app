package com.qohat.infra

import arrow.core.Either
import arrow.core.continuations.either
import com.qohat.config.FilesConfig
import com.qohat.config.S3Config
import com.qohat.domain.NewAttachment
import com.qohat.error.DomainError
import com.qohat.error.UploadError
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.retry.RetryPolicy
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy
import software.amazon.awssdk.core.retry.conditions.SdkRetryCondition
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.time.Duration

data class S3KeyObject(val value: String)

interface S3ClientI {
    suspend fun upload(attachment: NewAttachment): Either<DomainError, NewAttachment>
    fun putS3Object(keyObject: S3KeyObject, content: ByteArray): Either<DomainError, PutObjectResponse>
}

class S3Client(private val s3Config: S3Config, private val filesConfig: FilesConfig): S3ClientI {

    val logger: Logger = LoggerFactory.getLogger("S3Client")

    val retryPolicy = RetryPolicy.builder()
        .numRetries(3) // Retry up to 3 times
        .backoffStrategy(FullJitterBackoffStrategy.builder().baseDelay(Duration.ofMillis(500)).maxBackoffTime(Duration.ofSeconds(5)).build())
        .retryCondition(SdkRetryCondition.DEFAULT)
        .build()

    val clientConfiguration = ClientOverrideConfiguration.builder()
        .apiCallTimeout(Duration.ofSeconds(60)) // Increase timeout to 30 seconds
        .retryPolicy(retryPolicy)
        .build()

    override suspend fun upload(attachment: NewAttachment): Either<DomainError, NewAttachment> = either {
        logger.info("Uploading to S3 --- type: ${attachment.fileTypeId.name}, path/name ${attachment.path.value}/${attachment.name.value}...")
        val s3KeyObject = S3KeyObject("${filesConfig.people}/${attachment.path.value}/${attachment.name.value}")
        val content = Files.decode(attachment.content.value)
        putS3Object(s3KeyObject, content).bind()
        attachment
    }

    override fun putS3Object(keyObject: S3KeyObject, content: ByteArray): Either<DomainError, PutObjectResponse> {
        val request = PutObjectRequest
            .builder()
            .bucket(s3Config.bucketName)
            .key(keyObject.value).build()

        val client = S3AsyncClient.builder()
            .region(Region.of(s3Config.region))
            .overrideConfiguration(clientConfiguration)
            .build()

        return Either.catch {
            client.use {
                it.putObject(request, AsyncRequestBody.fromBytes(content))
                .whenComplete { resp, err ->
                    if(resp != null) { logger.info("Object was uploaded to S3 correctly") }
                    else { logger.error("Failed putting object", err)
                        throw err
                    }
                }.join()
            }
        }
        .tapLeft { e -> logger.error("Failed uploading to S3, keyObject = ${keyObject.value}", e) }
        .mapLeft {
            UploadError(keyObject)
        }
    }

    /*fun getObjectBytes(keyObject: S3KeyObject): GetObjectRequest {
        return GetObjectRequest {
            key = keyObject.value
            bucket = s3Config.bucketName
        }
    }*/

}