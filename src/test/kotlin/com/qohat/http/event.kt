package com.qohat

import com.qohat.config.AppConfig
import com.qohat.config.DBConfig
import com.qohat.config.dependencies
import com.qohat.data.user
import com.qohat.domain.BillReturnResponse
import com.qohat.domain.NewPeopleId
import com.qohat.domain.RoleId
import com.qohat.domain.RoleName
import com.qohat.domain.UserAttempt
import com.qohat.domain.UserToken
import com.qohat.domain.requests.BillReturnObservation
import com.qohat.domain.requests.BillReturnObservationId
import com.qohat.stub.S3Stub
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
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
import java.util.UUID

class EventSpec: FunSpec({
    runPostgresContainer()
    val postgres = PostgresTestContainer.postgres
    val schema = "fiduciaria"

    test("Seve get observations") {
        val dbConfig = DBConfig(postgres.jdbcUrl, postgres.username, postgres.password, schema)
        val appConfig = AppConfig.create().copy(dbConfig = dbConfig)
        val users = listOf(user(RoleId(2)))
        dependencies(appConfig).use {
            it.userRepo.save(users)
            withService(it.copy(s3Client = S3Stub())) {
                val login = client.post("/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(UserAttempt(users.first().email, users.first().password))
                }.body<UserToken>()

                val peopleId = client.post("/people/new") {
                    contentType(ContentType.Application.Json)
                    setBody(data.newPeople)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${login.token}")
                    }
                }.body<NewPeopleId>()
                val request = data.billReturnRequest.copy(
                    peopleRequest = data.billReturnRequest.peopleRequest.copy(
                        peopleId = peopleId
                    )
                )
                // 2. Save BillReturn
                val response = client.post("/people-requests/bill-return") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${login.token}")
                    }
                }
                val billreturnResponse = response.body<BillReturnResponse>()

                val observationId = client.post("/events/bill-returns/${billreturnResponse.billReturnId.value}/observations") {
                    contentType(ContentType.Application.Json)
                    setBody(data.billReturnObservation)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${login.token}")
                    }
                }.body<BillReturnObservationId>()

                val observations = client.get("/events/bill-returns/${billreturnResponse.billReturnId.value}/observations") {
                    contentType(ContentType.Application.Json)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${login.token}")
                    }
                }.body<List<BillReturnObservation>>()

                observations.size shouldBeGreaterThan 0
            }
        }
    }
})