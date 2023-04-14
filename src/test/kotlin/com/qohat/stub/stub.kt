package com.qohat.stub

import arrow.core.Either
import com.qohat.domain.NewAttachment
import com.qohat.error.DomainError
import com.qohat.infra.S3ClientI
import com.qohat.infra.S3KeyObject
import software.amazon.awssdk.services.s3.model.PutObjectResponse

class S3Stub: S3ClientI {
    override suspend fun upload(attachment: NewAttachment): Either<DomainError, NewAttachment> =
        Either.Right(attachment)

    override fun putS3Object(keyObject: S3KeyObject, content: ByteArray): Either<DomainError, PutObjectResponse> =
        Either.Right(PutObjectResponse.builder().build())
}