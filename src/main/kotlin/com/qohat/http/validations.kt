package com.qohat.http

import arrow.core.NonEmptyList
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.andThen
import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import arrow.core.validNel
import arrow.core.zip
import arrow.typeclasses.Semigroup
import com.qohat.domain.Document
import com.qohat.domain.ImportRecord
import com.qohat.domain.InvalidImportRecord
import com.qohat.domain.ListKey
import com.qohat.domain.NewPaymentRecord
import com.qohat.domain.NotPaidReason
import com.qohat.domain.NotPaidReportRecord
import com.qohat.domain.ProductId
import com.qohat.domain.ValidBillReturnRecord
import com.qohat.repo.ListRepo
import com.qohat.repo.ViewsRepo

suspend fun ValidatedNel<String, Pair<String, String>>.validatePeopleRequest(viewsRepo: ViewsRepo) =
    andThen {
        either<String, ValidBillReturnRecord> {
            val productId = ProductId.unApply(it.first)
            val document = Document(it.second)
            ensureNotNull(productId) { "El id del producto ${it.first} es inválido" }
            val billReturnId = viewsRepo.findBy(productId, document).mapLeft { e -> "Error inesperado $it, error: $e" }.bind()
            ensureNotNull(billReturnId) { "No existe una solicitud válida para la combinación " +
                    "Producto: ${productId.value}, Document: ${document.value}" }
            ValidBillReturnRecord(productId, document, billReturnId)
        }.toValidatedNel()
    }

suspend fun ValidatedNel<String, String>.validateReason(listRepo: ListRepo): Validated<NonEmptyList<String>, NotPaidReason> =
    andThen {
        either<String, NotPaidReason> {
            val reason = ensureNotNull(it.toInt()) { "La razón de no pago no es número válido" }
            val belongsToReasons = listRepo.findBy(ListKey("REJECT_REASON")).map { it.id }.contains(reason)
            ensure(belongsToReasons) { "La razón de no pago no pertenece a la lista de razones válidas" }
            NotPaidReason(reason)
        }.toValidatedNel()
    }

suspend fun validateLineV2(viewsRepo: ViewsRepo, list: List<String>, i: Int): ImportRecord {
    //O: Product Id - 1: Document
    val validation = Pair(list[0], list[1])
        .validNel()
        .validatePeopleRequest(viewsRepo)
        .zip(
            Semigroup.nonEmptyList(),
            list[2].validNel().validatePaymentDate(),
        ){validBillReturnRecord, date ->
            NewPaymentRecord(
                billReturnId = validBillReturnRecord.billReturnId,
                productId = validBillReturnRecord.productId,
                document = validBillReturnRecord.document,
                date = date,
                line = i,
                key = "${validBillReturnRecord.billReturnId.value}"
            )
        }

    return when(validation) {
        is Validated.Invalid -> InvalidImportRecord(validation.value.joinToString(prefix = "Error en fila ${i + 2}, Mensaje: "), "Error")
        is Validated.Valid -> validation.value
    }
}

suspend fun validateNotPaidLineV2(viewsRepo: ViewsRepo, listRepo: ListRepo, list: List<String>, i: Int): ImportRecord {
    val validation = Pair(list[0], list[1])
        .validNel()
        .validatePeopleRequest(viewsRepo)
        .zip(
            Semigroup.nonEmptyList(),
            list[2].validNel().validateReason(listRepo),
        ){validBillReturnRecord, reason ->
            NotPaidReportRecord(
                billReturnId = validBillReturnRecord.billReturnId,
                productId = validBillReturnRecord.productId,
                document = validBillReturnRecord.document,
                reasonId = reason,
                line = i,
                key = "${validBillReturnRecord.billReturnId.value}"
            )
        }

    return when(validation) {
        is Validated.Invalid -> InvalidImportRecord(validation.value.joinToString(prefix = "Error en fila ${i + 2}, Mensaje: "), "Error")
        is Validated.Valid -> validation.value
    }
}