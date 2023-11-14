package sh.dominick.inpeel.lib.sessions

import com.google.gson.*
import sh.dominick.inpeel.lib.data.oauth2.providers.OAuth2Grant
import sh.dominick.inpeel.lib.data.oauth2.providers.ProviderGrant
import sh.dominick.inpeel.lib.data.oauth2.sql.OAuth2Connection
import java.lang.reflect.Type
import java.time.Instant

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

            val provider = OAuth2Connection.providers.firstOrNull {
                it.id == providerId
            } ?: throw JsonParseException("Invalid `provider` property")

            val grant = ProviderGrant(props, provider)

            return OAuth2SessionMetadata(grant)
        }
    }
}