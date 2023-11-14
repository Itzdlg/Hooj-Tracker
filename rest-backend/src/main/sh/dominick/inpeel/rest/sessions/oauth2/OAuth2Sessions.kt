package sh.dominick.inpeel.rest.sessions.oauth2

import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.gson.JsonObject
import io.javalin.http.Context
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.inpeel.lib.data.oauth2.sql.connection
import sh.dominick.inpeel.lib.data.sessions.sql.Session
import sh.dominick.inpeel.lib.sessions.OAuth2SessionMetadata
import sh.dominick.inpeel.rest.sessions.RestSessionManager
import sh.dominick.inpeel.rest.sessions.SessionTransformer
import java.time.Instant
import java.time.temporal.ChronoUnit

object OAuth2SessionTransformer : SessionTransformer {
    override fun invoke(body: JsonObject, ctx: Context, rememberMe: Boolean): Session {
        val provider = body["provider"]?.asString?.let {  provider ->
            OAuth2ConnectionsManager.providers.first { it.apiId.equals(provider, ignoreCase = true) }
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
                expiresAt =
                    if (rememberMe)
                        Instant.now().plus(30, ChronoUnit.DAYS)
                    else
                        Instant.now().plus(6, ChronoUnit.HOURS)
                metadata = RestSessionManager.gson.toJsonTree(OAuth2SessionMetadata(grant)).asJsonObject
            }
        }
    }
}