package com.qohat.http

import com.qohat.PostgresTestContainer
import com.qohat.config.AppConfig
import com.qohat.config.DBConfig
import com.qohat.config.dependencies
import com.qohat.data
import com.qohat.domain.RoleId
import com.qohat.domain.RoleName
import com.qohat.domain.UserAttempt
import com.qohat.domain.UserToken
import com.qohat.entities.ConfigName
import com.qohat.entities.ConfigValue
import com.qohat.runPostgresContainer
import com.qohat.withService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class Configs: FunSpec({
    runPostgresContainer()
    val postgres = PostgresTestContainer.postgres
    val schema = "fiduciaria"

    test("Get dept percentage") {
        val dbConfig = DBConfig(postgres.jdbcUrl, postgres.username, postgres.password, schema)
        val appConfig = AppConfig.create().copy(dbConfig = dbConfig)
        val users = listOf(data.user(RoleId(1)))
        dependencies(appConfig).use {
            it.userRepo.save(users)
            withService(it) {
                val login = client.post("/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(UserAttempt(users.first().email, users.first().password))
                }.body<UserToken>()
                // 1. Save people
                val response = client.get("/values/config/dept-percentage") {
                    contentType(ContentType.Application.Json)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${login.token}")
                    }
                }
                val configs = response.body<Map<ConfigName, ConfigValue>>()
                configs.size shouldNotBe 0
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})