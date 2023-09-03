package sh.dominick.hoojtracker.modules.accounts.oauth2

import com.google.gson.GsonBuilder
import io.javalin.community.routing.annotations.AnnotatedRoutingPlugin
import io.javalin.config.JavalinConfig
import sh.dominick.hoojtracker.data.config.Configuration
import sh.dominick.hoojtracker.modules.Module
import sh.dominick.hoojtracker.modules.accounts.oauth2.data.OAuth2ConnectionsTable
import sh.dominick.hoojtracker.modules.accounts.oauth2.providers.DiscordOAuth2Provider
import sh.dominick.hoojtracker.modules.accounts.oauth2.providers.OAuth2Provider
import sh.dominick.hoojtracker.modules.accounts.oauth2.routes.OAuth2ConnectionRoutes
import sh.dominick.hoojtracker.modules.sessions.SessionsModule

object OAuth2ConnectionsModule : Module("accounts.oauth2_connections") {
    val DISABLED by Configuration.BooleanEntry("modules.${id}.disabled", false)

    override val tables = setOf(
        OAuth2ConnectionsTable
    )

    override val routes = setOf(
        OAuth2ConnectionRoutes
    )

    private val clientId = { provider: OAuth2Provider ->
        Configuration["modules.${id}.providers.${provider.apiId}.client_id"] ?: ""
    }

    private val clientSecret = { provider: OAuth2Provider ->
        Configuration["modules.${id}.providers.${provider.apiId}.client_secret"] ?: ""
    }

    private val acceptingLogins = { provider: OAuth2Provider ->
        Configuration["modules.${id}.providers.${provider.apiId}.logins"]?.toBooleanStrictOrNull() ?: false
    }

    private val acceptingRegistrations = { provider: OAuth2Provider ->
        Configuration["modules.${id}.providers.${provider.apiId}.registrations"]?.toBooleanStrictOrNull() ?: false
    }

    val providers: Set<OAuth2Provider> = setOf(
        object : DiscordOAuth2Provider() {
            override val clientId: String
                get() = clientId(this)

            override val clientSecret: String
                get() = clientSecret(this)

            override val acceptingLogins: Boolean
                get() = acceptingLogins(this)

            override val acceptingRegistrations: Boolean
                get() = !DISABLED && acceptingRegistrations(this)
        }
    )

    override fun load(routingPlugin: AnnotatedRoutingPlugin, gsonBuilder: GsonBuilder, javalinConfig: JavalinConfig) {
        super.load(routingPlugin, gsonBuilder, javalinConfig)

        gsonBuilder.registerTypeAdapter(OAuth2SessionMetadata::class.java, OAuth2SessionMetadata.GsonSerializer)

        providers.map { it.apiId }.forEach {
            Configuration.define(
                "modules.${id}.providers.${it}.client_id" to "",
                "modules.${id}.providers.${it}.client_secret" to "",
                "modules.${id}.providers.${it}.logins" to "false",
                "modules.${id}.providers.${it}.registrations" to "false"
            )
        }

        SessionsModule.registerType("oauth2-connection", OAuth2SessionTransformer)
    }
}