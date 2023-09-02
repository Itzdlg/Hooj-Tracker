package sh.dominick.hoojtracker.modules.oauth2

import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.javalin.community.routing.annotations.AnnotatedRoutingPlugin
import io.javalin.config.JavalinConfig
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.hoojtracker.data.config.Configuration
import sh.dominick.hoojtracker.modules.oauth2.data.OAuth2ConnectionsTable
import sh.dominick.hoojtracker.modules.oauth2.providers.DiscordOAuth2Provider
import sh.dominick.hoojtracker.modules.oauth2.providers.OAuth2Provider
import sh.dominick.hoojtracker.modules.Module
import sh.dominick.hoojtracker.modules.oauth2.data.connection
import sh.dominick.hoojtracker.modules.oauth2.providers.ProviderGrant
import sh.dominick.hoojtracker.modules.oauth2.routes.OAuth2ConnectionsController
import sh.dominick.hoojtracker.modules.sessions.SessionsModule
import sh.dominick.hoojtracker.modules.sessions.data.Session
import java.lang.IllegalArgumentException
import java.time.Instant
import java.time.temporal.ChronoUnit

object OAuth2ConnectionsModule : Module("accounts.oauth2_connections") {
    val DISABLED by Configuration.BooleanEntry("modules.${id}.disabled", false)

    override val tables = setOf(
        OAuth2ConnectionsTable
    )

    override val routes = setOf(
        OAuth2ConnectionsController
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

        providers.map { it.apiId }.forEach {
            Configuration.define(
                "modules.${id}.providers.${it}.client_id" to "",
                "modules.${id}.providers.${it}.client_secret" to "",
                "modules.${id}.providers.${it}.logins" to "false",
                "modules.${id}.providers.${it}.registrations" to "false"
            )
        }

        SessionsModule.registerType("oauth2-connection") {
            val provider = it["provider"]?.asString?.let {  provider ->
                providers.first { it.apiId.equals(provider, ignoreCase = true) }
            } ?: throw IllegalArgumentException("No provider was found with that name.")

            val code = it["code"]?.asString
                ?: throw IllegalArgumentException("Missing `code` property.")

            val grant = try {
                provider.requestToken(code)
            } catch (ex: TokenResponseException) {
                throw IllegalArgumentException("Invalid code provided.")
            }

            transaction {
                grant.connection?.refreshToken = grant.withProperties { props ->
                    props.refreshToken
                }
            }

            val rememberMe = it["rememberMe"]?.asBoolean ?: false

            transaction {
                Session.new {
                    account = grant.connection?.account ?: throw IllegalArgumentException("No account was connected with that grant.")
                    createdAt = Instant.now()
                    expiresAt =
                        if (rememberMe)
                            Instant.now().plus(30, ChronoUnit.DAYS)
                        else
                            Instant.now().plus(6, ChronoUnit.HOURS)
                    metadata = JsonObject().apply {
                        grant.withProperties { props ->
                            addProperty("accessToken", props.accessToken)
                            addProperty("refreshToken", props.refreshToken)
                            addProperty("expiration", props.expiration.toEpochMilli())
                            if (grant is ProviderGrant)
                                addProperty("provider", grant.provider.id)
                        }
                    }
                }
            }
        }
    }
}