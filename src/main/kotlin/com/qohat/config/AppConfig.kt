package com.qohat.config

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig

data class AppConfig private constructor(val dbConfig: DBConfig,
                                         val filesConfig: FilesConfig,
                                         val jwtConfig: JWTConfig,
                                         val s3Config: S3Config,
                                         val smtpConfig: SmtpConfig,
                                         val businessParams: BusinessParams) {
    companion object {
        @JvmStatic fun create(): AppConfig {
            val appConfig = HoconApplicationConfig(ConfigFactory.load())
            val dbConfig =
                DBConfig(
                    jdbcUrl = appConfig.property("app.db.jdbc_url").getString(),
                    user = appConfig.property("app.db.user").getString(),
                    password = appConfig.property("app.db.password").getString(),
                    schema = appConfig.property("app.db.schema").getString()
                )
            val filesConfig = FilesConfig(
                companies = appConfig.property("app.files.path.companies").getString(),
                people = appConfig.property("app.files.path.people").getString(),
                imported = appConfig.property("app.files.path.imported").getString()
            )

            val jwtConfig = JWTConfig(
                secret = appConfig.property("app.jwt.secret").getString(),
                issuer = appConfig.property("app.jwt.issuer").getString(),
                audience = appConfig.property("app.jwt.audience").getString(),
                realm = appConfig.property("app.jwt.realm").getString(),
            )

            val s3Config = S3Config(
                bucketName = appConfig.property("app.s3.bucket_name").getString(),
                region = appConfig.property("app.s3.region").getString()
            )

            val businessParams = BusinessParams(
                maxAllowedPayments = appConfig.property("app.business_params.max_allowed_payment").getString().toInt()
            )

            val smtpConfig = SmtpConfig(
                host = appConfig.property("app.email.base_host").getString(),
                from = appConfig.property("app.email.from_default").getString()
            )

            return AppConfig(
                dbConfig = dbConfig,
                filesConfig = filesConfig,
                jwtConfig = jwtConfig,
                s3Config = s3Config,
                businessParams = businessParams,
                smtpConfig = smtpConfig
            )
        }
    }
}

data class DBConfig(val jdbcUrl: String, val user: String, val password: String, val schema: String)
data class FilesConfig(val companies: String, val people: String, val imported: String)
data class JWTConfig(val secret: String, val issuer: String, val audience: String, val realm: String)
data class S3Config(val bucketName: String, val region: String)
data class BusinessParams(val maxAllowedPayments: Int)
data class SmtpConfig(val host: String, val from: String)