package com.qohat.features

import com.qohat.domain.PermissionCode
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.application.ApplicationCallPipeline.ApplicationPhase.Plugins
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

typealias Permission = PermissionCode

data class AuthUser(
    val id: UUID,
    val email: String,
    val fullName: String,
    val scopes: String,
    val role: String,
    val ccf: Int,
    val expiresAt: Date?
): Principal

class AuthorizedRouteSelector(private val description: String) : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int) = RouteSelectorEvaluation.Constant

    override fun toString(): String = "(authorize ${description})"
}

fun Route.withPermission(permission: Permission, build: Route.() -> Unit): Route {
    val authorizedRoute = createChild(AuthorizedRouteSelector("PermissionPlugin"))
    authorizedRoute.install(PermissionPlugin) { permissions = permission }
    authorizedRoute.build()
    return authorizedRoute
}

private val logger: Logger = LoggerFactory.getLogger("PermissionPlugin")

val PermissionPlugin = createRouteScopedPlugin(
    name = "PermissionPlugin",
    createConfiguration = ::PluginConfiguration
) {
    var permission = pluginConfig.permissions
    pluginConfig.apply {
        on(AuthenticationChecked) { call ->
            call.principal<AuthUser>()?.let {
                if(!it.scopes.contains(permission.name)) {
                    val message = "You don't have enough permissions for executing this action"
                    logger.warn("Authorization failed for ${call.request.path()}. Permission required: $permission - permissionChain: ${it.scopes}")
                    call.respond(HttpStatusCode.Unauthorized, mapOf("message" to message))
                }
            } ?: call.respond(HttpStatusCode.Forbidden, mapOf("message" to "Missing principal"))
        }
    }
}

class PluginConfiguration {
    var permissions: Permission = PermissionCode.Empty
}