package com.qohat.http

import com.qohat.PostgresTestContainer
import com.qohat.config.AppConfig
import com.qohat.config.DBConfig
import com.qohat.config.dependencies
import com.qohat.data
import com.qohat.domain.ProductShow
import com.qohat.runPostgresContainer
import com.qohat.withService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class ProductsRouter: FunSpec({
    runPostgresContainer()
    val postgres = PostgresTestContainer.postgres
    val schema = "fiduciaria"

    test("Save Product - Get Products") {
        val dbConfig = DBConfig(postgres.jdbcUrl, postgres.username, postgres.password, schema)
        val appConfig = AppConfig.create().copy(dbConfig = dbConfig)
        dependencies(appConfig).use {
            withService(it) {
                val respose = client.get("/products?limit=5&offset=15") {
                    contentType(ContentType.Application.Json)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${data.token}")
                    }
                }
                val list = respose.body<List<ProductShow>>()

                list.size shouldBeGreaterThan 0
            }
        }
    }
})