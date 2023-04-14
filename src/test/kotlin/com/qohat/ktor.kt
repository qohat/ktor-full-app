package com.qohat

import com.qohat.codec.Codecs
import com.qohat.config.Dependencies
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.TestApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.math.BigDecimal

/** Small DSL that exposes a setup [HttpClient] */
interface ServiceTest {
    val client: HttpClient
}

/** DSL to test MainKt server with setup [HttpClient] through [ServiceTest] */
suspend fun withService(
    dependencies: Dependencies,
    test: suspend ServiceTest.() -> Unit
): Unit = testApplication {
    application {
        app(dependencies)
    }
    createClient {
        expectSuccess = false
        install(ContentNegotiation) { json(
            Json {
                isLenient = true
                ignoreUnknownKeys = true
            }
        ) }
    }.use { client ->
        test(
            object : ServiceTest {
                override val client: HttpClient = client
            }
        )
    }
}

// Small optimisation to avoid runBlocking from Ktor impl
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
private suspend fun testApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
    val builder = ApplicationTestBuilder().apply { block() }
    val testApplication = TestApplication(builder)
    testApplication.engine.start()
    testApplication.stop()
}