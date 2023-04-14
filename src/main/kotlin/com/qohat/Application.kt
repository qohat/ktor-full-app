package com.qohat

import com.qohat.config.AppConfig
import com.qohat.config.Dependencies
import com.qohat.config.dependencies
import com.qohat.plugins.configureAuthentication
import com.qohat.plugins.configureHttp
import com.qohat.plugins.configureRouting
import com.qohat.plugins.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking(Dispatchers.Default) {
        val appConfig = AppConfig.create()
        dependencies(appConfig).use {
            embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
                app(it)
            }.start(wait = true)
        }
    }
fun Application.app(dependencies: Dependencies) {
    configure(dependencies)
}

fun Application.configure(dependencies: Dependencies) {
    configureSerialization()
    configureHttp()
    configureAuthentication(dependencies.appConfig)
    configureRouting(dependencies)
}