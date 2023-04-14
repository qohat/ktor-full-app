package com.qohat

import com.qohat.config.AppConfig
import com.qohat.config.DBConfig
import com.qohat.config.dependencies
import com.qohat.data.user
import com.qohat.domain.RoleId
import com.qohat.domain.RoleName
import com.qohat.domain.UserAttempt
import com.qohat.domain.UserToken
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class AuthSpec: FunSpec({
    runPostgresContainer()
    val postgres = PostgresTestContainer.postgres
    val schema = "fiduciaria"

    test("Login") {
        val dbConfig = DBConfig(postgres.jdbcUrl, postgres.username, postgres.password, schema)
        val appConfig = AppConfig.create().copy(dbConfig = dbConfig)
        dependencies(appConfig).use {
            val user = user(RoleId(3))
            val id = it.userRepo.save(user)
            withService(it) {
                val response = client.post("/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(UserAttempt(user.email, user.password))
                }
                val userToken = response.body<UserToken>()
                userToken.id shouldBe id.value.toString()
                userToken.roleName shouldBe user.role.name
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})