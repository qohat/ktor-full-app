package com.qohat.services

import com.qohat.domain.*
import com.qohat.repo.CompanyFileRepo
import com.qohat.repo.EventRepo
import com.qohat.repo.PeopleFileRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

interface EventService<I, E> {
    suspend fun findAll(id: I): List<E>
    suspend fun saveBulk(events: List<E>)
    suspend fun save(event: E): EventId
}

class CompanyValidationEventsService(
    private val companyValidationEventRepo: EventRepo<CompanyId, ValidationCompaniesAttachmentsEvent>,
    private val companyFileRepo: CompanyFileRepo
): EventService<CompanyId, ValidationCompaniesAttachmentsEvent> {

    private val logger: Logger = LoggerFactory.getLogger(CompanyValidationEventsService::class.java)

    override suspend fun findAll(id: CompanyId): List<ValidationCompaniesAttachmentsEvent> =
        companyValidationEventRepo.findAll(id)

    override suspend fun saveBulk(events: List<ValidationCompaniesAttachmentsEvent>): Unit =
        events
        .asFlow()
        //.map { save(it) }
        .flatMapConcat {
            event -> flow<CompanyId> { save(event) }
        }
        .flowOn(Dispatchers.Default)
        .collect()

    override suspend fun save(event: ValidationCompaniesAttachmentsEvent): EventId {
        logger.info("Trying to save event - AttachmentId: ${event.companyAttachment.id} - AttachmentName: ${event.companyAttachment.name} - UserName: ${event.userName}")
        return EventId(companyValidationEventRepo.save(event).toString())
            .also { updateCompanyAttachmentState(event) }
    }

    private suspend fun updateCompanyAttachmentState(event: ValidationCompaniesAttachmentsEvent): Unit =
        if(event.state != event.companyAttachment.state) {
            val companyAttachment = event.companyAttachment.copy(state = event.state)
            logger.info("Updating state - AttachmentId: ${companyAttachment.id} - AttachmentName: ${companyAttachment.name} - UserName: ${event.userName} - " +
                    "OldState: ${event.companyAttachment.state} - NewState ${companyAttachment.state}")
            companyFileRepo.update(UUID.fromString(companyAttachment.id), companyAttachment)
        } else Unit
}

class PeopleValidationEventsService(
    private val peopleValidationEventRepo: EventRepo<PeopleCompanyId, ValidationPeopleAttachmentsEvent>,
    private val peopleFileRepo: PeopleFileRepo
): EventService<PeopleCompanyId, ValidationPeopleAttachmentsEvent> {

    private val logger: Logger = LoggerFactory.getLogger(PeopleValidationEventsService::class.java)

    override suspend fun findAll(id: PeopleCompanyId): List<ValidationPeopleAttachmentsEvent> =
        peopleValidationEventRepo.findAll(id)

    override suspend fun saveBulk(events: List<ValidationPeopleAttachmentsEvent>): Unit =
        events
        .asFlow()
        //.map { save(it) }
        .flatMapConcat {
                event -> flow<CompanyId> { save(event) }
        }
        .flowOn(Dispatchers.Default)
        .collect()

    override suspend fun save(event: ValidationPeopleAttachmentsEvent): EventId {
        logger.info("Trying to save event - AttachmentId: ${event.peopleAttachment.id} - AttachmentName: ${event.peopleAttachment.name} - UserName: ${event.userName}")
        return EventId(peopleValidationEventRepo.save(event).toString())
            .also { updatePeopleAttachmentState(event) }
    }

    private suspend fun updatePeopleAttachmentState(event: ValidationPeopleAttachmentsEvent): Unit =
        if(event.state != event.peopleAttachment.state) {
            val peopleAttachment = event.peopleAttachment.copy(state = event.state)
            logger.info("Updating state - AttachmentId: ${peopleAttachment.id} - AttachmentName: ${peopleAttachment.name} - UserName: ${event.userName} - " +
                    "OldState: ${event.peopleAttachment.state} - NewState ${peopleAttachment.state}")
            peopleFileRepo.update(UUID.fromString(peopleAttachment.id), peopleAttachment)
        } else Unit
}

