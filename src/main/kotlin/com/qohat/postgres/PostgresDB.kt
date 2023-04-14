package com.qohat.postgres

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.continuations.resource
import com.qohat.config.DBConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

fun postgres(dbConfig: DBConfig): Resource<Database> = resource {
    val dataSource = hikari(dbConfig).bind()
    migrate(dbConfig).bind()
    Database.connect(dataSource)
}

fun migrate(dbConfig: DBConfig): Resource<Int> = resource {
    val flyway = Flyway.configure()
        .dataSource(dbConfig.jdbcUrl, dbConfig.user, dbConfig.password)
        .schemas(dbConfig.schema)
        .load()
    val result = flyway.migrate()
    result.migrationsExecuted
}

fun hikari(dbConfig: DBConfig): Resource<HikariDataSource> = resource {
    HikariDataSource(
        HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = dbConfig.jdbcUrl.replace("?", "?currentSchema=${dbConfig.schema}&")
            username = dbConfig.user
            password = dbConfig.password
            maximumPoolSize = 90
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }
    )
}