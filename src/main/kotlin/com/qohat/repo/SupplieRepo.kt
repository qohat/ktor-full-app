package com.qohat.repo

import arrow.core.Either
import com.qohat.domain.Name
import com.qohat.domain.NewPaginationParams
import com.qohat.domain.Supply
import com.qohat.domain.SupplyId
import com.qohat.domain.SupplyShow
import com.qohat.entities.Supplies
import com.qohat.entities.toSupplyShow
import com.qohat.error.DomainError
import org.jetbrains.exposed.sql.Database

interface SupplieRepo {
    suspend fun findAll(params: NewPaginationParams): Either<DomainError, List<SupplyShow>>
    suspend fun findAll(): Either<DomainError, List<SupplyShow>>
    suspend fun findAllBy(cropGroup: Name): Either<DomainError, List<SupplyShow>>
    suspend fun findBy(supplyId: SupplyId): Either<DomainError, SupplyShow?>
    suspend fun save(supply: Supply): Either<DomainError, SupplyId>
    suspend fun update(supplyId: SupplyId, supply: Supply): Either<DomainError, Unit>
    suspend fun delete(supplyId: SupplyId): Either<DomainError, Unit>
}

class DefaultSupplieRepo(private val db: Database): SupplieRepo {
    override suspend fun findAll(params: NewPaginationParams): Either<DomainError, List<SupplyShow>> =
        transact(db) {
            Supplies.selectAll(params).map { it.toSupplyShow() }
        }

    override suspend fun findAll(): Either<DomainError, List<SupplyShow>> =
        transact(db) {
            Supplies.selectAll().map { it.toSupplyShow() }
        }

    override suspend fun findAllBy(cropGroup: Name): Either<DomainError, List<SupplyShow>> =
        transact(db) {
            Supplies.selectAll(cropGroup).map { it.toSupplyShow() }
        }

    override suspend fun findBy(supplyId: SupplyId): Either<DomainError, SupplyShow?> =
        transact(db) {
            Supplies.selectBy(supplyId)?.toSupplyShow()
        }

    override suspend fun save(supply: Supply): Either<DomainError, SupplyId> =
        transact(db) {
            val id = Supplies.insertQuery(supply)
            SupplyId(id)
        }

    override suspend fun update(supplyId: SupplyId, supply: Supply): Either<DomainError, Unit> =
        transact(db) {
            Supplies.updateQuery(supplyId, supply)
        }.void()

    override suspend fun delete(supplyId: SupplyId): Either<DomainError, Unit> =
        transact(db) {
            Supplies.deleteQuery(supplyId)
        }.void()

}