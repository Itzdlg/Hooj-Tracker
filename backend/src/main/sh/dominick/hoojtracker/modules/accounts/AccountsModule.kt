package sh.dominick.hoojtracker.modules.accounts

import com.google.gson.GsonBuilder
import io.javalin.community.routing.annotations.AnnotatedRoutingPlugin
import io.javalin.config.JavalinConfig
import sh.dominick.hoojtracker.auth.Authorization
import sh.dominick.hoojtracker.modules.accounts.data.AccountCredentialsTable
import sh.dominick.hoojtracker.modules.accounts.data.AccountsTable
import sh.dominick.hoojtracker.modules.Module
import sh.dominick.hoojtracker.modules.accounts.routes.AccountsController

object AccountsModule : Module("accounts") {
    override val tables = setOf(
        AccountsTable,
        AccountCredentialsTable
    )

    override val routes = setOf(
        AccountsController
    )

    override fun load(routingPlugin: AnnotatedRoutingPlugin, gsonBuilder: GsonBuilder, javalinConfig: JavalinConfig) {
        super.load(routingPlugin, gsonBuilder, javalinConfig)

        Authorization.registerType("Basic") {
            BasicAuth.match(it)
        }
    }
}