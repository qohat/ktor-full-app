package com.qohat.repo

import arrow.core.Either
import com.qohat.domain.Name
import com.qohat.domain.NewPaginationParams
import com.qohat.domain.Product
import com.qohat.domain.ProductId
import com.qohat.domain.ProductShow
import com.qohat.entities.Products
import com.qohat.entities.toProductShow
import com.qohat.error.DomainError
import org.jetbrains.exposed.sql.Database

interface ProductRepo {
    suspend fun findAll(params: NewPaginationParams): Either<DomainError, List<ProductShow>>
    suspend fun findAll(): Either<DomainError, List<ProductShow>>
    suspend fun findAllBy(cropGroup: Name): Either<DomainError, List<ProductShow>>
    suspend fun findBy(productId: ProductId): Either<DomainError, ProductShow?>
    suspend fun save(product: Product): Either<DomainError, ProductId>
    suspend fun update(productId: ProductId, product: Product): Either<DomainError, Unit>
    suspend fun delete(productId: ProductId): Either<DomainError, Unit>
}

class DefaultProductRepo(private val db: Database): ProductRepo {
    override suspend fun findAll(params: NewPaginationParams): Either<DomainError, List<ProductShow>> =
        transact(db) {
            Products.selectAll(params).map { it.toProductShow() }
        }

    override suspend fun findAll(): Either<DomainError, List<ProductShow>> =
        transact(db) {
            Products.selectAll().map { it.toProductShow() }
        }

    override suspend fun findAllBy(cropGroup: Name): Either<DomainError, List<ProductShow>> =
        transact(db) {
            Products.selectAll(cropGroup).map { it.toProductShow() }
        }

    override suspend fun findBy(productId: ProductId): Either<DomainError, ProductShow?> =
        transact(db) {
            Products.selectBy(productId)?.toProductShow()
        }

    override suspend fun save(product: Product): Either<DomainError, ProductId> =
        transact(db) {
            val id = Products.insertQuery(product)
            ProductId(id)
        }

    override suspend fun update(productId: ProductId, product: Product): Either<DomainError, Unit> =
        transact(db) {
            Products.updateQuery(productId, product)
        }.void()

    override suspend fun delete(productId: ProductId): Either<DomainError, Unit> =
        transact(db) {
            Products.deleteQuery(productId)
        }.void()

}