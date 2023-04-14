val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val arrow_version: String by project
val exposed_version: String by project
val hikari_version: String by project
val postgres_version: String by project
val bcrypt_version: String by project
val flyway_version: String by project
val aws_sdk_s3_version: String by project
val aws_sdk_ses_version: String by project
val aws_sdk_auth_version: String by project
val csv_read_write: String by project
val kotest_version: String by project
val test_container_postgres: String by project
val kotest_test_container_version: String by project
val suspendapp: String by project
val swagger_codegen_version: String by project

plugins {
    application
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "com.qohat"
application {
    mainClass.set("com.qohat.ApplicationKt")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
}

dependencies {
    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    //http
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-openapi:$ktor_version")
    implementation("io.ktor:ktor-server-swagger:$ktor_version")
    implementation("io.swagger.codegen.v3:swagger-codegen-generators:$swagger_codegen_version")


    //Auth
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")

    //Password
    implementation("at.favre.lib:bcrypt:$bcrypt_version")

    implementation("ch.qos.logback:logback-classic:$logback_version")

    //Arrow
    implementation("io.arrow-kt:arrow-core:$arrow_version")
    implementation("io.arrow-kt:arrow-fx-coroutines:$arrow_version")
    implementation("io.arrow-kt:arrow-fx-stm:$arrow_version")

    //DB Exposed
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposed_version")
    implementation( "com.zaxxer:HikariCP:$hikari_version")
    implementation("org.postgresql:postgresql:$postgres_version")

    //S3

    implementation("software.amazon.awssdk:s3:$aws_sdk_s3_version")
    implementation("software.amazon.awssdk:ses:$aws_sdk_ses_version")
    implementation("software.amazon.awssdk:auth:$aws_sdk_auth_version")

    //DB Migration
    implementation("org.flywaydb:flyway-core:$flyway_version")

    //CSV
    implementation("org.apache.commons:commons-csv:$csv_read_write") //for JVM platform

    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.kotest:kotest-runner-junit5-jvm:4.6.0")

    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0")
    testImplementation("io.kotest:kotest-runner-junit5:$kotest_version")
    testImplementation("io.kotest:kotest-assertions-core:$kotest_version")
    testImplementation("io.kotest.extensions:kotest-assertions-ktor:1.0.3")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    //testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:$kotest_test_container_version")
    testImplementation("org.testcontainers:postgresql:$test_container_postgres")

}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks{
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "com.qohat.ApplicationKt"))
        }
    }
}