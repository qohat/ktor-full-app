package com.qohat.postgres

import com.qohat.PostgresTestContainer
import com.qohat.config.DBConfig
import com.qohat.runPostgresContainer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan

class Migration: FunSpec({
    runPostgresContainer()
    val postgres = PostgresTestContainer.postgres
    test("Should run more that 0 migrations") {
        val dbConfig = DBConfig(postgres.jdbcUrl, postgres.username, postgres.password, "fiduciaria")
        val migrations = migrate(dbConfig)
        migrations.use {
            it shouldBeGreaterThan 0
        }
    }
})