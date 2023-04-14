package com.qohat.repo

import arrow.core.Either
import com.qohat.domain.Document
import com.qohat.domain.ProductId
import com.qohat.domain.RequestState
import com.qohat.domain.requests.BillReturnId
import com.qohat.entities.SpecificBalance
import com.qohat.entities.SpecificBalanceView
import com.qohat.entities.toSpecificBalance
import com.qohat.error.DomainError
import org.jetbrains.exposed.sql.Database
interface ViewsRepo {
    suspend fun specificBalance(requestState: RequestState): Either<DomainError, List<SpecificBalance>>
    suspend fun findBy(productId: ProductId, document: Document): Either<DomainError, BillReturnId?>
    suspend fun findBy(billReturnId: BillReturnId): Either<DomainError, SpecificBalance?>
}
class DefaultViewsRepo(private val db: Database): ViewsRepo {
    override suspend fun specificBalance(requestState: RequestState): Either<DomainError, List<SpecificBalance>> =
        transact(db) {
            SpecificBalanceView.selectBy(requestState).map { it.toSpecificBalance() }
        }

    override suspend fun findBy(productId: ProductId, document: Document): Either<DomainError, BillReturnId?> =
        transact(db) {
            SpecificBalanceView.selectBillReturnIdBy(productId, document)
        }

    override suspend fun findBy(billReturnId: BillReturnId): Either<DomainError, SpecificBalance?> =
        transact(db) {
            SpecificBalanceView.selectBy(billReturnId)?.toSpecificBalance()
        }
}