package com.qohat.services

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.qohat.config.FilesConfig
import com.qohat.domain.*
import com.qohat.error.S3Errors
import com.qohat.error.ServiceError
import com.qohat.infra.Files
import com.qohat.infra.S3Client
import com.qohat.infra.S3KeyObject
import com.qohat.repo.PeopleCompanyRepo
import com.qohat.repo.PeopleFileRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

interface PeopleCompanyService {
    suspend fun findBy(id: PeopleCompanyId): Option<PeopleCompany>
    suspend fun findBy(id: PeopleCompanyValidationId): Option<PeopleCompanyValidation>
    suspend fun findAllBy(peopleId: PeopleId): List<PeopleCompany>
    suspend fun findAllLite(state: AttachmentState, params: PaginationParams): List<PeopleCompanyLite>
    suspend fun findAllLite(state: AttachmentState, userId: UserId, params: PaginationParams): List<PeopleCompanyLite>
    suspend fun findAllLite(companyId: CompanyId): List<PeopleCompanyLite>
    suspend fun findAllArchived(userId: UserId): List<PeopleCompanyLite>
    suspend fun findAllValidationBy(state: AttachmentState, userId: UserId): List<PeopleCompanyValidation>
    suspend fun findAllValidationBy(state: AttachmentState): List<PeopleCompanyValidation>
    suspend fun findAllValidationAtLeastOnePaid(): List<PeopleCompanyValidation>
    suspend fun save(peopleCompany: PeopleCompany): PeopleCompanyId
    suspend fun update(id: PeopleCompanyId, peopleCompany: PeopleCompany): Boolean
    suspend fun update(id: PeopleCompanyId, state: AttachmentState): Boolean
    suspend fun delete(id: PeopleCompanyId): Boolean
    suspend fun attachFile(peopleCompany: PeopleCompany, attachFileToPeopleCompany: AttachFileToPeopleCompany): Either<ServiceError, Unit>
    suspend fun anyFileRejected(peopleCompanyId: PeopleCompanyId): Boolean
}

class DefaultPeopleCompanyService(
    private val filesConfig: FilesConfig,
    private val peopleFileRepo: PeopleFileRepo,
    private val peopleCompanyRepo: PeopleCompanyRepo,
    private val s3Client: S3Client): PeopleCompanyService {
    private val logger: Logger = LoggerFactory.getLogger(DefaultPeopleService::class.java)

    override suspend fun findBy(id: PeopleCompanyId): Option<PeopleCompany> =
        peopleCompanyRepo.findBy(UUID.fromString(id.value))

    override suspend fun findBy(id: PeopleCompanyValidationId): Option<PeopleCompanyValidation> =
        peopleCompanyRepo.findBy(id)

    override suspend fun findAllBy(peopleId: PeopleId): List<PeopleCompany> =
        peopleCompanyRepo.findAllBy(peopleId)

    override suspend fun findAllLite(state: AttachmentState, params: PaginationParams): List<PeopleCompanyLite> =
        peopleCompanyRepo.findAllLite(state, params)

    override suspend fun findAllLite(state: AttachmentState, userId: UserId, params: PaginationParams): List<PeopleCompanyLite> =
        peopleCompanyRepo.findAllLite(state, userId, params)

    override suspend fun findAllLite(companyId: CompanyId): List<PeopleCompanyLite> =
        peopleCompanyRepo.findAllLite(companyId)

    override suspend fun findAllArchived(userId: UserId): List<PeopleCompanyLite> =
        peopleCompanyRepo.findAllArchived(userId)

    override suspend fun findAllValidationBy(state: AttachmentState, userId: UserId): List<PeopleCompanyValidation> =
        peopleCompanyRepo.findAllValidationBy(state, userId)

    override suspend fun findAllValidationBy(state: AttachmentState): List<PeopleCompanyValidation> =
        peopleCompanyRepo.findAllValidationBy(state)

    override suspend fun findAllValidationAtLeastOnePaid(): List<PeopleCompanyValidation> =
        peopleCompanyRepo.findAllValidationAtLeastOnePaid()

    override suspend fun save(peopleCompany: PeopleCompany): PeopleCompanyId =
        PeopleCompanyId(peopleCompanyRepo.save(peopleCompany).toString())

    override suspend fun update(id: PeopleCompanyId, peopleCompany: PeopleCompany): Boolean =
        when(findBy(id)) {
            is None -> false
            is Some -> {
                peopleCompanyRepo.update(UUID.fromString(id.value), peopleCompany)
                true
            }
        }

    override suspend fun update(id: PeopleCompanyId, state: AttachmentState): Boolean =
        when(findBy(id)) {
            is None -> false
            is Some -> {
                peopleCompanyRepo.update(UUID.fromString(id.value), state)
                true
            }
        }

    override suspend fun delete(id: PeopleCompanyId): Boolean =
        when(findBy(id)) {
            is None -> false
            is Some -> {
                peopleCompanyRepo.delete(UUID.fromString(id.value))
                true
            }
        }

    override suspend fun attachFile(peopleCompany: PeopleCompany, attachFileToPeopleCompany: AttachFileToPeopleCompany): Either<ServiceError, Unit> =
        coroutineScope {
            val upload = async(Dispatchers.Default) {
                when(val peopleFile = peopleFileRepo.findBy(peopleCompany.id, attachFileToPeopleCompany.attachment.fileId.id)) {
                    is None -> {
                        when(val attempt = uploadToS3(peopleCompany.peopleId, peopleCompany.companyId, attachFileToPeopleCompany.attachment)) {
                            is Either.Left -> attempt
                            is Either.Right -> {
                                logger.info("Saving PeopleAttachment Record - peopleCompanyId ${peopleCompany.id.value} ...")
                                val peopleAttachment = toPeopleAttachment(peopleCompany, attempt.value)
                                peopleFileRepo.save(peopleAttachment)
                                Either.Right(Unit)
                            }
                        }
                    }

                    is Some -> {
                        logger.info("File ${attachFileToPeopleCompany.attachment.fileId.name} already exist for - peopleCompany ${peopleCompany.id.value}...")
                        logger.info("Trying to save the file - peopleCompanyId ${peopleCompany.id.value}...")
                        when(val attempt = uploadToS3(peopleCompany.peopleId, peopleCompany.companyId, attachFileToPeopleCompany.attachment)) {
                            is Either.Left -> attempt
                            is Either.Right -> {
                                peopleFileRepo.update(UUID.fromString(peopleFile.value.id), peopleFile.value.copy(name = attachFileToPeopleCompany.attachment.name, state = AttachmentState.InReview))
                                Either.Right(Unit)
                            }
                        }
                    }
                }
            }
            upload.await()
        }

    override suspend fun anyFileRejected(peopleCompanyId: PeopleCompanyId): Boolean =
        peopleFileRepo.findAllBy(peopleCompanyId, AttachmentState.Rejected)
            .isNotEmpty()

    private fun toPeopleAttachment(peopleCompany: PeopleCompany, attachment: Attachment): PeopleAttachment =
        PeopleAttachment(
            id = UUID.randomUUID().toString(),
            peopleCompanyId = peopleCompany.id.value,
            name = attachment.name,
            path = attachment.path,
            state = AttachmentState.InReview,
            active = true,
            fileType = attachment.fileId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

    private fun uploadToS3(peopleId: PeopleId, companyId: CompanyId, attachment: Attachment): Either<ServiceError, Attachment> {
        logger.info("Uploading to S3 the file for People: ${peopleId.value} and Company: ${companyId.value}...")
        val s3KeyObject = S3KeyObject("${filesConfig.people}/${peopleId.value}/${companyId.value}/${attachment.name}")
        val content = Files.decode(attachment.content)
        return s3Client.putS3Object(s3KeyObject, content)
        .map { attachment.copy(path = "${filesConfig.people}/${peopleId.value}/${companyId.value}") }
        .mapLeft { e ->
            S3Errors.S3UploadError.gen("Failed uploading the file for People: ${peopleId.value} and Company: ${companyId.value}")
        }
    }
}