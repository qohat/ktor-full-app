package com.qohat.http

import com.qohat.PostgresTestContainer
import com.qohat.config.AppConfig
import com.qohat.config.DBConfig
import com.qohat.config.dependencies
import com.qohat.domain.NewPeopleId
import com.qohat.runPostgresContainer
import com.qohat.withService
import com.qohat.data.newPeople
import com.qohat.data.user
import com.qohat.domain.CreatedById
import com.qohat.domain.NewPeople
import com.qohat.json
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.decodeFromString

class PeopleRouter: FunSpec({

    runPostgresContainer()
    val postgres = PostgresTestContainer.postgres
    val schema = "fiduciaria"

    test("Save new people") {
        val dbConfig = DBConfig(postgres.jdbcUrl, postgres.username, postgres.password, schema)
        val appConfig = AppConfig.create().copy(dbConfig = dbConfig)
        dependencies(appConfig).use {
            val user = user()
            val userId = it.userRepo.save(user)
            withService(it) {
                val response = client.post("/people/new") {
                    contentType(ContentType.Application.Json)
                    setBody(newPeople.copy(createdBy = CreatedById(userId.value)))
                }
                val id = response.body<NewPeopleId>()
                id shouldNotBe newPeople.id
                response.status shouldBe HttpStatusCode.Created
            }
        }
    }

    test("Decoder") {
        val data = json.decodeFromString<NewPeople>("""
        {
            "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            "name": "string",
            "lastName": "string",
            "documentType": {
                "id": 1,
                "name": "string",
                "list": "string",
                "active": true
            },
            "document": "string",
            "issueDocumentDate": "2123-12-22",
            "birthday": "2123-12-22",
            "address": "2123-12-22",
            "sex": {
                "id": 1,
                "name": "string",
                "list": "string",
                "active": true
            },
            "gender": {
                "id": 1,
                "name": "string",
                "list": "string",
                "active": true
            },
            "phone": 1,
            "cellPhone": 1,
            "email": "user@example.com",
            "populationGroup": {
                "id": 1,
                "name": "string",
                "list": "string",
                "active": true
            },
            "ethnicGroup": {
                "id": 1,
                "name": "string",
                "list": "string",
                "active": true
            },
            "disability": {
                "id": 1,
                "name": "string",
                "list": "string",
                "active": true
            },
            "armedConflictVictim": true,
            "displaced": true,
            "propertyInformation": {
                "address": "string",
                "name": "string",
                "department": {
                    "id": 1,
                    "name": "string",
                    "list": "string",
                    "active": true
                },
                "city": {
                    "id": 1,
                    "name": "string",
                    "list": "string",
                    "active": true
                },
                "lane": "string",
                "hectares": 1
            },
            "belongsOrganization": true,
            "organizationBelongingInfo": {
                "type": {
                    "id": 1,
                    "name": "string",
                    "list": "string",
                    "active": true
                },
                "name": "string",
                "nit": "string"
            },
            "paymentInformation": {
                "paymentType": {
                    "id": 1,
                    "name": "string",
                    "list": "string",
                    "active": true
                },
                "bank": {
                    "id": 1,
                    "name": "string",
                    "list": "string",
                    "active": true
                },
                "accountType": {
                    "id": 1,
                    "name": "string",
                    "list": "string",
                    "active": true
                },
                "accountNumber": "string"
            },
            "singleMother": true,
            "termsAcceptance": true,
            "createdBy": null
        }
    """)
       println(data)
    }

})