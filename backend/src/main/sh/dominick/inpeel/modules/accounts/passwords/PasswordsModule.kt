package sh.dominick.inpeel.modules.accounts.passwords

import com.google.gson.GsonBuilder
import io.javalin.community.routing.annotations.AnnotatedRoutingPlugin
import io.javalin.config.JavalinConfig
import sh.dominick.inpeel.auth.Authorization
import sh.dominick.inpeel.modules.Module
import sh.dominick.inpeel.modules.accounts.passwords.data.AccountPasswordsTable
import sh.dominick.inpeel.modules.sessions.SessionsModule

object PasswordsModule : Module("account.passwords") {
    override val tables = setOf(
        AccountPasswordsTable
    )

    override fun load(routingPlugin: AnnotatedRoutingPlugin, gsonBuilder: GsonBuilder, javalinConfig: JavalinConfig) {
        super.load(routingPlugin, gsonBuilder, javalinConfig)

        Authorization.registerType("Basic") {
            BasicAuth.match(it)
        }

        SessionsModule.registerType("password", PasswordSessionTransformer)
    }
}