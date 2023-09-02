package sh.dominick.hoojtracker.modules.oauth2

import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.gson.*
import io.javalin.http.Context
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.hoojtracker.modules.oauth2.data.connection
import sh.dominick.hoojtracker.modules.oauth2.providers.OAuth2Grant
import sh.dominick.hoojtracker.modules.oauth2.providers.ProviderGrant
import sh.dominick.hoojtracker.modules.sessions.SessionTransformer
import sh.dominick.hoojtracker.modules.sessions.data.Session
import sh.dominick.hoojtracker.util.toJsonObject
import java.lang.reflect.Type
import java.time.Instant
import java.time.temporal.ChronoUnit

object OAuth2SessionTransformer : SessionTransformer {
    override fun invoke(body: JsonObject, ctx: Context, rememberMe: Boolean): Session {
        val provider = body["provider"]?.asString?.let {  provider ->
            OAuth2ConnectionsModule.providers.first { it.apiId.equals(provider, ignoreCase = true) }
        } ?: throw IllegalArgumentException("No provider was found with that name.")

        val code = body["code"]?.asString
            ?: throw IllegalArgumentException("Missing `code` property.")

        val grant = try {
            provider.requestToken(code)
        } catch (ex: TokenResponseException) {
            throw IllegalArgumentException("Invalid `code` provided.")
        }

        return transaction {
            grant.connection?.refreshToken = grant.withProperties { props ->
                props.refreshToken
            }

            Session.new {
                account =
                    grant.connection?.account ?: throw IllegalArgumentException("No account was connected with that grant.")
                createdAt = Instant.now()
                expiresAt =
                    if (rememberMe)
                        Instant.now().plus(30, ChronoUnit.DAYS)
                    else
                        Instant.now().plus(6, ChronoUnit.HOURS)
                metadata = OAuth2SessionMetadata(grant).toJsonObject()
            }
        }
    }
}

class OAuth2SessionMetadata(val grant: OAuth2Grant) {
    object GsonSerializer : JsonSerializer<OAuth2SessionMetadata>, JsonDeserializer<OAuth2SessionMetadata> {
        override fun serialize(md: OAuth2SessionMetadata, p1: Type, p2: JsonSerializationContext): JsonElement {
            val grant = md.grant

            return JsonObject().apply {
                grant.withProperties { props ->
                    addProperty("accessToken", props.accessToken)
                    addProperty("refreshToken", props.refreshToken)
                    addProperty("expiration", props.expiration.toEpochMilli())
                    if (grant is ProviderGrant)
                        addProperty("provider", grant.provider.id)
                }
            }
        }

        override fun deserialize(el: JsonElement, p1: Type, p2: JsonDeserializationContext): OAuth2SessionMetadata {
            val obj = el.asJsonObject

            val accessToken = obj["accessToken"]?.asString
                ?: throw JsonParseException("Missing `accessToken` property")

            val refreshToken = obj["refreshToken"]?.asString
                ?: throw JsonParseException("Missing `refreshToken` property")

            val expiration = obj["expiration"]?.asLong?.let {
                Instant.ofEpochMilli(it)
            } ?: throw JsonParseException("Missing `expiration` property")

            val props = OAuth2Grant.Properties(accessToken, refreshToken, expiration)

            val providerId = obj["provider"]?.asInt
                ?: throw JsonParseException("Missing `provider` property")

            val provider = OAuth2ConnectionsModule.providers.firstOrNull {
                it.id == providerId
            } ?: throw JsonParseException("Invalid `provider` property")

            val grant = ProviderGrant(props, provider)

            return OAuth2SessionMetadata(grant)
        }
    }
}