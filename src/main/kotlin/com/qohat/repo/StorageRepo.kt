package com.qohat.repo

import arrow.core.Either
import com.qohat.domain.NewPaginationParams
import com.qohat.domain.Storage
import com.qohat.domain.StorageId
import com.qohat.domain.StorageShow
import com.qohat.entities.Storages
import com.qohat.entities.toStorageShow
import com.qohat.error.DomainError
import org.jetbrains.exposed.sql.Database

interface StorageRepo {
    suspend fun findAll(params: NewPaginationParams): Either<DomainError, List<StorageShow>>
    suspend fun findBy(storageId: StorageId): Either<DomainError, StorageShow?>
    suspend fun save(storage: Storage): Either<DomainError, StorageId>
    suspend fun update(storageId: StorageId, storage: Storage): Either<DomainError, Unit>
    suspend fun delete(storageId: StorageId): Either<DomainError, Unit>
}

class DefaultStorageRepo(private val db: Database): StorageRepo {
    override suspend fun findAll(params: NewPaginationParams): Either<DomainError, List<StorageShow>> =
        transact(db) {
            Storages.selectAll(params).map { it.toStorageShow() }
        }

    override suspend fun findBy(storageId: StorageId): Either<DomainError, StorageShow?> =
        transact(db) {
            Storages.selectBy(storageId)?.toStorageShow()
        }

    override suspend fun save(storage: Storage): Either<DomainError, StorageId> =
        transact(db) {
            val id = Storages.insertQuery(storage)
            StorageId(id)
        }

    override suspend fun update(storageId: StorageId, storage: Storage): Either<DomainError, Unit> =
        transact(db) {
            Storages.updateQuery(storageId, storage)
        }.void()

    override suspend fun delete(storageId: StorageId): Either<DomainError, Unit> =
        transact(db) {
            Storages.deleteQuery(storageId)
        }.void()

}