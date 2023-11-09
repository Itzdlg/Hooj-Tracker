package sh.dominick.inpeel.lib.managers

import com.google.gson.GsonBuilder
import io.javalin.community.routing.annotations.AnnotatedRoutingPlugin
import io.javalin.config.JavalinConfig
import sh.dominick.inpeel.lib.ApplicationInitialized
import sh.dominick.inpeel.lib.data.oauth2.OAuth2SessionMetadata
import sh.dominick.inpeel.lib.data.oauth2.OAuth2SessionTransformer
import sh.dominick.inpeel.lib.data.oauth2.providers.DiscordOAuth2Provider
import sh.dominick.inpeel.lib.data.oauth2.providers.OAuth2Provider
import sh.dominick.inpeel.lib.data.sql.Configuration

object OAuth2ConnectionsManager : ApplicationInitialized {
    init {
        SessionManager.registerType("oauth2-connection", OAuth2SessionTransformer)

        SessionManager.gson
            .newBuilder()
            .registerTypeAdapter(OAuth2SessionMetadata::class.java, OAuth2SessionMetadata.GsonSerializer)
            .create()
    }

    private val clientId = { provider: OAuth2Provider ->
        Configuration["oauth2.providers.${provider.apiId}.client_id"] ?: ""
    }

    private val clientSecret = { provider: OAuth2Provider ->
        Configuration["oauth2.providers.${provider.apiId}.client_secret"] ?: ""
    }

    private val acceptingLogins = { provider: OAuth2Provider ->
        Configuration["oauth2.providers.${provider.apiId}.logins"]?.toBooleanStrictOrNull() ?: false
    }

    private val acceptingRegistrations = { provider: OAuth2Provider ->
        Configuration["oauth2.providers.${provider.apiId}.registrations"]?.toBooleanStrictOrNull() ?: false
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
                get() = acceptingRegistrations(this)
        }
    )

    override fun load(routingPlugin: AnnotatedRoutingPlugin, gsonBuilder: GsonBuilder, javalinConfig: JavalinConfig) {
        providers.map { it.apiId }.forEach {
            Configuration.define(
                "oauth2.providers.${it}.client_id" to "",
                "oauth2.providers.${it}.client_secret" to "",
                "oauth2.providers.${it}.logins" to "false",
                "oauth2.providers.${it}.registrations" to "false"
            )
        }
    }
}