package com.qohat

import io.kotest.core.spec.AfterTest
import io.kotest.core.spec.BeforeTest
import io.kotest.core.spec.DslDrivenSpec
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait

class PostgreSQL: PostgreSQLContainer<PostgreSQL>("postgres:latest") {
    init { // Needed for M1
        waitingFor(Wait.forListeningPort())
    }
}

object PostgresTestContainer {

    const val dbName = "postgres"
    const val username = "postgres"
    const val password = "password"

    val postgres: PostgreSQL = PostgreSQL()
        .withDatabaseName(dbName)
        .withUsername(username)
        .withPassword(password)
        .withExposedPorts(5432)
        .withStartupAttempts(2)
        .waitingFor(Wait.forListeningPort())

    val beforeTest: BeforeTest = {
        postgres.start()
    }

    val afterTest: AfterTest = {
        postgres.stop()
    }
}

fun DslDrivenSpec.runPostgresContainer() {
    beforeTest(PostgresTestContainer.beforeTest)
    afterTest(PostgresTestContainer.afterTest)
}