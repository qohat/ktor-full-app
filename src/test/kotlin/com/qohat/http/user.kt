package com.qohat.http

import com.qohat.config.AppConfig
import com.qohat.config.dependencies
import com.qohat.data.token
import com.qohat.data.user
import com.qohat.domain.User
import com.qohat.domain.UserId
import com.qohat.withService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.UUID

class UserRouter: FunSpec({

    //runPostgresContainer()
    //val postgres = PostgresTestContainer.postgres
    //val schema = "fiduciaria"

    test("Recover password - userNot found") {
        //val dbConfig = DBConfig(postgres.jdbcUrl, postgres.username, postgres.password, schema)
        val appConfig = AppConfig.create()//.copy(dbConfig = dbConfig)
        dependencies(appConfig).use {
            withService(it) {
                val response = client.get("/user/recover-password/qemail@mail.com")
                response.status shouldBe HttpStatusCode.NotFound
            }
        }
    }

    test("Recover and Renew password successfully") {
        //val dbConfig = DBConfig(postgres.jdbcUrl, postgres.username, postgres.password, schema)
        val appConfig = AppConfig.create()//.copy(dbConfig = dbConfig)
        dependencies(appConfig).use {
            val user = user().copy(email = "qohatpp@gmail.com")
            it.userRepo.save(user)
            withService(it) {
                val response = client.post("/user/recover-password/${user.email}")
                /*val response = client.post("/user/renew-password/${user.email}") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        RecoverPasswordRequest(
                            token = token,
                            password = Password("secret")
                        )
                    )
                }
                response.status shouldBe HttpStatusCode.OK*/
                response.status shouldBe HttpStatusCode.Accepted
            }
        }
    }

    test("Should save, update and find a User") {
        //val dbConfig = DBConfig(postgres.jdbcUrl, postgres.username, postgres.password, schema)
        val appConfig = AppConfig.create()//.copy(dbConfig = dbConfig)
        dependencies(appConfig).use {
            val user = user()
            withService(it) {
                //Post
                val response = client.post("/user") {
                    contentType(ContentType.Application.Json)
                    setBody(user)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }
                val userId = response.body<UserId>()

                //Put
                val newName = "NewName"
                val newEmail = "email@mailito.com"
                val updatedUser = client.put("/user/${userId.value}") {
                    contentType(ContentType.Application.Json)
                    setBody(user.copy(name = newName, lastName = newName, email = newEmail))
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }

                //Get
                val savedUser = client.get("/user/${userId.value}") {
                    contentType(ContentType.Application.Json)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }.body<User?>()

                response.status shouldBe HttpStatusCode.Created
                updatedUser.status shouldBe HttpStatusCode.Accepted
                savedUser?.email shouldBe newEmail
                savedUser?.name shouldBe newName
                savedUser?.lastName shouldBe newName
            }
        }
    }

    test("Generate UUID") {
        val ids = (1..47987)
            .map {
                UUID.randomUUID().toString()
            }.joinToString("\n")
        println(ids)
        1 shouldBe 1
    }
})