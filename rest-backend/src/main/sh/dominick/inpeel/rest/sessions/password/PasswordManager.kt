package sh.dominick.inpeel.rest.sessions.password

import com.google.gson.GsonBuilder
import io.javalin.community.routing.annotations.AnnotatedRoutingPlugin
import io.javalin.config.JavalinConfig
import sh.dominick.inpeel.lib.ApplicationInitialized
import sh.dominick.inpeel.lib.data.passwords.BasicAuth
import sh.dominick.inpeel.lib.managers.AuthorizationManager
import sh.dominick.inpeel.rest.sessions.RestSessionManager

object PasswordManager : ApplicationInitialized {
    override fun load(routingPlugin: AnnotatedRoutingPlugin, gsonBuilder: GsonBuilder, javalinConfig: JavalinConfig) {
        AuthorizationManager.registerType("Basic") {
            BasicAuth.match(it)
        }

        RestSessionManager.registerType("password", PasswordSessionTransformer)
    }
}