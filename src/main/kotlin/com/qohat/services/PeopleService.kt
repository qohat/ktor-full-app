package com.qohat.services

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.qohat.domain.*
import com.qohat.repo.PeopleRepo
import java.util.*

interface PeopleService {
    suspend fun findBy(id: PeopleId): Option<People>
    suspend fun findBy(document: PersonDocument): Option<People>
    suspend fun findBy(email: PersonEmail): Option<People>
    suspend fun findAllBy(userId: UserId, params: PaginationParams): List<People>
    suspend fun save(people: People): PeopleId
    suspend fun update(id: PeopleId, people: People): Boolean
    suspend fun delete(id: PeopleId): Boolean
}

class DefaultPeopleService(private val peopleRepo: PeopleRepo): PeopleService {

    override suspend fun findBy(id: PeopleId): Option<People> =
        peopleRepo.findBy(UUID.fromString(id.value))

    override suspend fun findBy(document: PersonDocument): Option<People> =
        peopleRepo.findBy(document)

    override suspend fun findBy(email: PersonEmail): Option<People>  =
        peopleRepo.findBy(email)

    override suspend fun findAllBy(userId: UserId, params: PaginationParams): List<People> =
        peopleRepo.findAllBy(userId, params)

    override suspend fun save(people: People): PeopleId =
        PeopleId(peopleRepo.save(people).toString())

    override suspend fun update(id: PeopleId, people: People): Boolean =
        when(findBy(id)) {
            is None -> false
            is Some -> {
                peopleRepo.update(UUID.fromString(id.value), people)
                true
            }
        }

    override suspend fun delete(id: PeopleId): Boolean =
        when(findBy(id)) {
            is None -> false
            is Some -> {
                peopleRepo.delete(UUID.fromString(id.value))
                true
            }
        }
}