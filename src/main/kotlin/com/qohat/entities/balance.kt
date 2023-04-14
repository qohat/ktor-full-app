package com.qohat.entities

import arrow.core.identity
import com.qohat.domain.Document
import com.qohat.domain.Name
import com.qohat.domain.PeopleAccountNumber
import com.qohat.domain.ProductId
import com.qohat.domain.RequestState
import com.qohat.domain.requests.Amount
import com.qohat.domain.requests.BillReturnId
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

@Serializable
data class SpecificBalance(
    val product: Name,
    val peopleName: Name,
    val document: Document,
    val paymentType: Name,
    val branch: Name?,
    val bank: Name?,
    val accountType: Name?,
    val accountNumber: PeopleAccountNumber?,
    val totalSuppliesAmount: Amount,
    val valueToSubsidize: Amount
)
fun List<SpecificBalance>.bankHeader() =
    "Producto,Nombre,Documento,Tipo de Pago,Banco,Tipo de Cuenta,Número,Total Monto Insumos,Valor a Subsidiar"
fun List<SpecificBalance>.brachHeader() =
    "Producto,Nombre,Documento,Tipo de Pago,Banco,Sucursal,Total Monto Insumos,Valor a Subsidiar"
fun List<SpecificBalance>.postalHeader() =
    "Producto,Nombre,Documento,Tipo de Pago,Total Monto Insumos,Valor a Subsidiar"
fun SpecificBalance.toBankCsv() =
    "${product.value},${peopleName.value},${document.value},${paymentType.value},${bank?.value},${accountType?.value},${accountNumber?.value},${totalSuppliesAmount.value},${valueToSubsidize.value}"
fun SpecificBalance.toBranchCsv() =
    "${product.value},${peopleName.value},${document.value},${paymentType.value},${bank?.value},${branch?.value},${totalSuppliesAmount.value},${valueToSubsidize.value}"
fun SpecificBalance.toPostalCsv() =
    "${product.value},${peopleName.value},${document.value},${paymentType.value},${totalSuppliesAmount.value},${valueToSubsidize.value}"
fun ResultRow.toSpecificBalance() =
    SpecificBalance(
        product = Name(this[SpecificBalanceView.product]),
        peopleName = Name(this[SpecificBalanceView.people_name]),
        document = Document(this[SpecificBalanceView.document]),
        paymentType = Name(this[SpecificBalanceView.payment_type]),
        branch = this[SpecificBalanceView.branch]?.let { Name(it) },
        bank = this[SpecificBalanceView.bank]?.let { Name(it) },
        accountType = this[SpecificBalanceView.account_type]?.let { Name(it) },
        accountNumber = this[SpecificBalanceView.account_number]?.let { PeopleAccountNumber(it) },
        totalSuppliesAmount = Amount(this[SpecificBalanceView.total_supplies_amount]),
        valueToSubsidize = Amount(this[SpecificBalanceView.value_to_subsidize]),
    )

object SpecificBalanceView: Table("specific_balance_view") {
    val id = uuid("id")
    val product_id = long("product_id")
    val product = varchar("product", 100)
    val people_name = varchar("people_name", 100)
    val document = varchar("document", 100)
    val payment_type = varchar("payment_type", 100)
    val branch = varchar("branch", 100).nullable()
    val bank = varchar("bank", 100).nullable()
    val account_type = varchar("account_type", 100).nullable()
    val account_number = varchar("account_number", 100).nullable()
    val total_supplies_amount = decimal("total_supplies_amount", 2, 2)
    val prospect_value_to_subsidize = decimal("prospect_value_to_subsidize", 2, 2)
    val value_to_subsidize = decimal("value_to_subsidize", 2, 2)
    val state = customEnumeration("state", "RequestState", {value -> RequestState.valueOf(value as String)}, { PGEnum("RequestState", it) })
    
    val query = slice(
        product,
        people_name,
        document,
        payment_type,
        branch,
        bank,
        account_type,
        account_number,
        total_supplies_amount,
        value_to_subsidize
    )
    
    fun selectBy(requestState: RequestState): List<ResultRow> =
        query.select { state.eq(requestState) }.map { identity(it) }

    fun selectBillReturnIdBy(productId: ProductId, doc: Document): BillReturnId? =
        select { product_id.eq(productId.value) and document.eq(doc.value) }
            .singleOrNull()?.let { BillReturnId(it[id]) }

    fun selectBy(billReturnId: BillReturnId): ResultRow? =
        select { id.eq(billReturnId.value) }
            .singleOrNull()
}

object GlobalBalance: Table("global_balance_view") {
    val department = varchar("department", 50)
    val requests = integer("requests")
    val chain = varchar("chain", 50)
    val dept_chain_budget = decimal("dept_chain_budget", 2, 2)
    val total_value_to_subsidize = decimal("total_value_to_subsidize", 2, 2)
    val available_budget = decimal("available_budget", 2, 2)
    
    val select: List<ResultRow> = selectAll().map { identity(it) }
    fun selectBy(chainName: Name, departmentName: Name): List<ResultRow> =
        select { department.eq(departmentName.value) and chain.eq(chainName.value) }
            .map { identity(it) }
}