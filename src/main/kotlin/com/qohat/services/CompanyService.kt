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
import com.qohat.repo.CompanyFileRepo
import com.qohat.repo.CompanyRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

interface CompanyService {
    suspend fun findBy(id: CompanyId): Option<Company>
    suspend fun findBy(document: CompanyDocument): Option<Company>
    suspend fun findBy(name: CompanyName): Option<Company>
    suspend fun findBy(email: CompanyEmail): Option<Company>
    suspend fun findAllBy(ccfId: CcfId): List<Company>
    suspend fun findAll(): List<Company>
    suspend fun save(company: Company): CompanyId
    suspend fun update(id: CompanyId, company: Company): Boolean
    suspend fun delete(id: CompanyId): Boolean
    suspend fun attachFile(company: Company, attachFileToCompany: AttachFileToCompany): Either<ServiceError, Unit>
    suspend fun anyFileRejected(id: CompanyId): Boolean
}

class DefaultCompanyService(
    private val filesConfig: FilesConfig,
    private val companyRepo: CompanyRepo,
    private val companyFileRepo: CompanyFileRepo,
    private val s3Client: S3Client
): CompanyService {

    private val logger: Logger = LoggerFactory.getLogger(DefaultCompanyService::class.java)

    override suspend fun findBy(id: CompanyId): Option<Company> = companyRepo.findBy(UUID.fromString(id.value))

    override suspend fun findBy(document: CompanyDocument): Option<Company> = companyRepo.findBy(document)

    override suspend fun findBy(name: CompanyName): Option<Company> =
        companyRepo.findBy(name)

    override suspend fun findBy(email: CompanyEmail): Option<Company> = companyRepo.findBy(email)

    override suspend fun findAllBy(ccfId: CcfId): List<Company> = companyRepo.findAllBy(ccfId)
    override suspend fun findAll(): List<Company> =
        companyRepo.findAll()

    override suspend fun save(company: Company): CompanyId = CompanyId(companyRepo.save(company).toString())

    override suspend fun update(id: CompanyId, company: Company): Boolean =
        when(findBy(id)) {
            is None -> false
            is Some -> {
                companyRepo.update(UUID.fromString(id.value), company)
                true
            }
        }

    override suspend fun delete(id: CompanyId): Boolean =
        when(findBy(id)) {
            is None -> false
            is Some -> {
                companyRepo.delete(UUID.fromString(id.value))
                true
            }
        }

    override suspend fun attachFile(company: Company, attachFileToCompany: AttachFileToCompany): Either<ServiceError, Unit> =
        coroutineScope {
            val upload = async (Dispatchers.Default) {
                when(val companyFile = companyFileRepo.findBy(company.id, attachFileToCompany.attachment.fileId.id)) {
                    is None -> {
                        when(val attempt = uploadToS3(CompanyDocument(company.document), attachFileToCompany.attachment)) {
                            is Either.Left -> attempt
                            is Either.Right -> {
                                logger.info("Saving CompanyAttachment Record - company: ${company.document} ...")
                                val companyAttachment = toCompanyAttachment(company, attempt.value)
                                companyFileRepo.save(companyAttachment)
                                Either.Right(Unit)
                            }
                        }
                    }

                    is Some -> {
                        logger.info("File ${attachFileToCompany.attachment.fileId.name} already exist for - company ${company.document}...")
                        logger.info("Trying to replace old record for new one - company ${company.document}...")
                        when(val attempt = uploadToS3(CompanyDocument(company.document), attachFileToCompany.attachment)) {
                            is Either.Left -> attempt
                            is Either.Right -> {
                                companyFileRepo.update(UUID.fromString(companyFile.value.id), companyFile.value.copy(name = attachFileToCompany.attachment.name, state = AttachmentState.InReview))
                                Either.Right(Unit)
                            }
                        }
                    }
                }
            }
            upload.await()
        }

    override suspend fun anyFileRejected(id: CompanyId): Boolean =
        companyFileRepo.findAllBy(id, AttachmentState.Rejected)
            .isNotEmpty()

    private fun toCompanyAttachment(company: Company, attachment: Attachment): CompanyAttachment =
        CompanyAttachment(
            id = UUID.randomUUID().toString(),
            companyId = company.id.value,
            name = attachment.name,
            path = attachment.path,
            state = AttachmentState.InReview,
            active = true,
            fileType = attachment.fileId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

    private fun uploadToS3(document: CompanyDocument, attachment: Attachment): Either<ServiceError, Attachment> {
        logger.info("Uploading to S3 the file for company: ${document.value}...")
        val s3KeyObject = S3KeyObject("${filesConfig.companies}/${document.value}/${attachment.name}")
        val content = Files.decode(attachment.content)
        return s3Client.putS3Object(s3KeyObject, content)
            .map { attachment.copy(path = "${filesConfig.companies}/${document.value}") }
            .mapLeft {e ->
                S3Errors.S3UploadError.gen("Failed uploading file for company: ${document.value}")
            }
    }
}