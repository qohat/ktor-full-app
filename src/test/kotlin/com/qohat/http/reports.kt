package com.qohat.http

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.continuations.either
import arrow.core.flatMap
import arrow.core.nonEmptyListOf
import arrow.core.traverse
import arrow.fx.coroutines.parTraverse
import arrow.fx.coroutines.parTraverseEither
import arrow.fx.coroutines.parTraverseValidated
import arrow.typeclasses.Semigroup
import com.qohat.PostgresTestContainer
import com.qohat.config.AppConfig
import com.qohat.config.DBConfig
import com.qohat.config.dependencies
import com.qohat.data
import com.qohat.domain.FileContent
import com.qohat.domain.NewImportPayment
import com.qohat.domain.ProductShow
import com.qohat.runPostgresContainer
import com.qohat.stub.S3Stub
import com.qohat.withService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class Reports: FunSpec({
    runPostgresContainer()
    val postgres = PostgresTestContainer.postgres
    val schema = "fiduciaria"

    test("Should import") {
        val dbConfig = DBConfig(postgres.jdbcUrl, postgres.username, postgres.password, schema)
        val appConfig = AppConfig.create().copy(dbConfig = dbConfig)
        dependencies(appConfig).use {
            withService(it.copy(s3Client = S3Stub())) {
                val respose = client.post("/billreturns-payments") {
                    contentType(ContentType.Application.Json)
                    setBody(NewImportPayment(content = FileContent("UHJvZHVjdG8sRG9jdW1lbnRvLEZlY2hhCjEsMTA4Mjk4NTk2NSwyMDIzLTAzLTAxCjEsMzY1NDMyMjEsMjAyMy0wMy0wMg==")))
                }
                respose.status shouldBe HttpStatusCode.Accepted
            }
        }
    }

    test("Test Failure Email") {
        val ids = listOf("Hi", "From", "The", "List5", "Ho")

        suspend fun eitherB(value: String): Either<String, Int> = either {
            value.length
        }
        suspend fun eitherLaunch(list: List<String>): Either<NonEmptyList<String>, List<String>> =
            list.traverse {
                if(it.length == 4) {
                    Either.Left(nonEmptyListOf(it))
                } else {
                    println("Hi There : $it")
                    Either.Right(it)
                }
            }.map {
                println("Qohat $it")
                it
            }

        when(val either = eitherLaunch(ids)) {
            is Either.Left -> either.value shouldBe "Error"
            is Either.Right -> either.value shouldBe Unit
        }
    }
})