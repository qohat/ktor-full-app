package com.qohat.http

import com.qohat.PostgresTestContainer
import com.qohat.config.AppConfig
import com.qohat.config.dependencies
import com.qohat.data
import com.qohat.data.user
import com.qohat.domain.AttachmentIds
import com.qohat.domain.AttachmentState
import com.qohat.domain.BillReturnResponse
import com.qohat.domain.NewPeopleId
import com.qohat.domain.PeopleRequestBillReturn
import com.qohat.domain.RequestState
import com.qohat.domain.RequestType
import com.qohat.domain.RoleId
import com.qohat.domain.RoleName
import com.qohat.domain.UserAttempt
import com.qohat.domain.UserToken
import com.qohat.domain.requests.Amount
import com.qohat.domain.requests.BillReturnDetails
import com.qohat.json
import com.qohat.stub.S3Stub
import com.qohat.withService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import kotlinx.serialization.encodeToString
import java.math.BigDecimal
import java.util.UUID

class PeopleRequestSpec: FunSpec({
    //runPostgresContainer()
    val postgres = PostgresTestContainer.postgres
    val schema = "fiduciaria"

    test("Save PeopleRequest - Save BillReturn - Get PeopleRequest created - Get BillReturn Details - Execute Batch Update State") {
        //val dbConfig = DBConfig(postgres.jdbcUrl, postgres.username, postgres.password, schema)
        val appConfig = AppConfig.create()//.copy(dbConfig = dbConfig)
        val users = listOf(user(RoleId(3)), user(RoleId(1)), user(RoleId(4)))
        dependencies(appConfig).use {
            it.userRepo.save(users)
            withService(it.copy(s3Client = S3Stub())) {
                val login = client.post("/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(UserAttempt(users.first().email, users.first().password))
                }.body<UserToken>()
                // 1. Save people
                val peopleId = client.post("/people/new") {
                    contentType(ContentType.Application.Json)
                    setBody(data.newPeople)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${login.token}")
                    }
                }.body<NewPeopleId>()
                val request = data.billReturnRequest.copy(
                    peopleRequest = data.billReturnRequest.peopleRequest.copy(
                        peopleId = NewPeopleId(UUID.fromString("9328b285-0dcf-40e8-8051-778cbf9d2f8d"))
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
                // 3. Lists the created ones
                val listResponse = client.get("/people-requests/bill-return/${RequestState.Created.value}/${RequestType.BILL_RETURN_REQUEST.value}") {
                    contentType(ContentType.Application.Json)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${login.token}")
                    }
                }.body<List<PeopleRequestBillReturn>>()

                // 4. Get Details
                val billReturnId = listResponse.first().billReturnId
                val details = client.get("/people-requests/bill-return/${billReturnId.value}/details") {
                    contentType(ContentType.Application.Json)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${login.token}")
                    }
                }.body<BillReturnDetails>()

                // 5. Execute batch update
                client.put("/people-requests/bill-return/${billReturnId.value}/${AttachmentState.Approved.value}") {
                    contentType(ContentType.Application.Json)
                    setBody(AttachmentIds(details.attachments.map { attChId -> attChId.id }))
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${login.token}")
                    }
                }

                val details1 = client.get("/people-requests/bill-return/${billReturnId.value}/details") {
                    contentType(ContentType.Application.Json)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${login.token}")
                    }
                }.body<BillReturnDetails>()

                val listApprovedResponse = client.get("/people-requests/bill-return/${RequestState.Approved.value}/${RequestType.BILL_RETURN_REQUEST.value}") {
                    contentType(ContentType.Application.Json)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${login.token}")
                    }
                }.body<List<PeopleRequestBillReturn>>()

                details1.attachments.filter { attCh -> attCh.state == AttachmentState.RequiresValidation }.size shouldBe details1.attachments.size
                billreturnResponse.peopleRequestId shouldNotBe data.peopleRequest.id
                billreturnResponse.billReturnId shouldNotBe data.billReturn.id
                listApprovedResponse.size shouldBeGreaterThan 0
                listResponse.size shouldBeGreaterThan 0
                details.supplies.size shouldBeGreaterThan 0
                response.status shouldBe HttpStatusCode.Created
            }
        }
    }

    test("Save PeopleRequest - Save BillReturn") {
        //val dbConfig = DBConfig(postgres.jdbcUrl, postgres.username, postgres.password, schema)
        val appConfig = AppConfig.create()
        dependencies(appConfig).use {
            withService(it.copy(s3Client = S3Stub())) {
                // 1. Save people
                val peopleId = client.post("/people/new") {
                    contentType(ContentType.Application.Json)
                    setBody(data.newPeople)
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
                        append(HttpHeaders.Authorization, "Bearer ${data.token}")
                    }
                }
                val billreturnResponse = response.body<BillReturnResponse>()

                val details = client.get("/people-requests/bill-return/${billreturnResponse.billReturnId.value}/to-pay") {
                    contentType(ContentType.Application.Json)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${data.token}")
                    }
                }.body<BillReturnDetails>()

                response.status shouldBe HttpStatusCode.Created
                details.paymentProspect shouldNotBe null
                details.paymentProspect?.amount?.value shouldBe BigDecimal(4000000.00) + BigDecimal(1000.00)
                details.paymentProspect?.prospectSubsidyValue shouldBe Amount(BigDecimal.valueOf((1000 + 4000000) * 0.2))
                details.paymentProspect?.subsidyValue shouldBe Amount(BigDecimal.valueOf((1000 + 4000000) * 0.2))
            }
        }
    }

    test("should encode to stirng") {
        val data2 = json.encodeToString(data.billReturnRequest)
        println(data2)
    }
})