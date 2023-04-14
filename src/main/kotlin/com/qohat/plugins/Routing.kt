package com.qohat.plugins

import com.qohat.config.Dependencies
import com.qohat.http.authRouting
import com.qohat.http.docsRouting
import com.qohat.http.eventRouting
import com.qohat.http.healthRouting
import com.qohat.http.listRouting
import com.qohat.http.peopleRequestRouting
import com.qohat.http.peopleRouting
import com.qohat.http.productRouter
import com.qohat.http.reportsRouting
import com.qohat.http.roleRouter
import com.qohat.http.storagesRouter
import com.qohat.http.suppliesRouter
import com.qohat.http.userRouting
import com.qohat.http.valuesRouting
import io.ktor.server.application.Application
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import io.swagger.codegen.v3.generators.html.StaticHtmlCodegen

fun Application.configureRouting(dependencies: Dependencies) {
    routing {
        authRouting(dependencies.authService)
        userRouting(dependencies.userRepo, dependencies.sesClient, dependencies.roleRepo)
        peopleRouting(dependencies.peopleRepo)
        listRouting(dependencies.listService)
        docsRouting()
        valuesRouting(dependencies.configRepo)
        reportsRouting(dependencies.viewsRepo)
        roleRouter(dependencies.roleRepo)
        peopleRequestRouting(dependencies.peopleRequestRepo, dependencies.productRepo, dependencies.configRepo, dependencies.s3Client, dependencies.assignmentService)
        productRouter(dependencies.productRepo)
        suppliesRouter(dependencies.suppliesRepo)
        storagesRouter(dependencies.storagesRepo)
        eventRouting(dependencies.eventRepoV2)
        healthRouting()
        openAPI(path="openapi", swaggerFile = "openapi/documentation.yaml") {
            codegen = StaticHtmlCodegen()
        }
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml") {
            version = "4.15.5"
        }
    }
}


