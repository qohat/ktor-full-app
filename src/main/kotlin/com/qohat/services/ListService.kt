package com.qohat.services

import arrow.core.Option
import com.qohat.domain.ListKey
import com.qohat.domain.ValueList
import com.qohat.repo.ListRepo

interface ListService {
    suspend fun findBy(name: String, key: ListKey): Option<ValueList>
    suspend fun findBy(key: ListKey): List<ValueList>
}

class DefaultListService(private val listRepo: ListRepo): ListService {
    override suspend fun findBy(name: String, key: ListKey): Option<ValueList> =
        listRepo.findBy(name, key)


    override suspend fun findBy(key: ListKey): List<ValueList> =
        listRepo.findBy(key)

}