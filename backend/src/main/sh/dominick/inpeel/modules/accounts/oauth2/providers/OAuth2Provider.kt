package sh.dominick.inpeel.modules.accounts.oauth2.providers

import com.google.api.client.auth.oauth2.*
import com.google.api.client.http.BasicAuthentication
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.gson.JsonObject
import sh.dominick.inpeel.Env
import java.io.IOException
import java.time.Instant

abstract class OAuth2Provider(val id: Int, val apiId: String) {
    open val flow: AuthorizationCodeFlow by lazy {
        AuthorizationCodeFlow.Builder(
            BearerToken.authorizationHeaderAccessMethod(),
            NetHttpTransport(),
            GsonFactory(),
            GenericUrl(tokenUri),
            BasicAuthentication(clientId, clientSecret),
            clientId,
            authorizationUri
        ).build()
    }

    abstract val clientId: String
    abstract val clientSecret: String

    abstract val acceptingRegistrations: Boolean
    abstract val acceptingLogins: Boolean

    abstract val tokenUri: String
    abstract val authorizationUri: String

    open val redirectUri: String
        = Env.OAUTH2_REDIRECT_URI.replace("{provider}", apiId)

    abstract fun requestProviderAccount(accessToken: String): JsonObject

    abstract fun requestProviderAccountId(accessToken: String): String
    abstract fun requestProviderAccountName(accessToken: String): String

    open fun requestToken(code: String): OAuth2Grant {
        val response = flow
            .newTokenRequest(code)
            .setRedirectUri(redirectUri)
            .execute()

        return ProviderGrant(OAuth2Grant.Properties(response), this)
    }

    open fun refreshToken(refreshToken: String): OAuth2Grant.Properties {
        val refreshResponse = RefreshTokenRequest(
            flow.transport,
            flow.jsonFactory,
            GenericUrl(flow.tokenServerEncodedUrl),
            refreshToken
        )
            .setClientAuthentication(flow.clientAuthentication)
            .setRequestInitializer(flow.requestInitializer)
            .execute()

        return OAuth2Grant.Properties(refreshResponse)
    }
}

abstract class OAuth2Grant(
    protected var properties: Properties
) {
    class Properties(
        val accessToken: String,
        val refreshToken: String,
        val expiration: Instant
    ) {
        constructor(tokenResponse: TokenResponse) : this(
            tokenResponse.accessToken,
            tokenResponse.refreshToken,
            Instant.now().plusSeconds(tokenResponse.expiresInSeconds)
        )
    }

    abstract val providerAccount: JsonObject
    abstract val providerAccountId: String
    abstract val providerAccountName: String
    abstract fun refresh()

    open fun <T> withProperties(grant: (Properties) -> (T)): T {
        return try {
            grant(this.properties)
        } catch (ex: IOException) {
            this.refresh()
            grant(this.properties)
        }
    }
}

class ProviderGrant(properties: Properties, val provider: OAuth2Provider) : OAuth2Grant(properties) {
    override val providerAccount: JsonObject by lazy {
        withProperties {
            provider.requestProviderAccount(it.accessToken)
        }
    }

    override val providerAccountId: String by lazy {
        withProperties {
            provider.requestProviderAccountId(it.accessToken)
        }
    }

    override val providerAccountName: String by lazy {
        withProperties {
            provider.requestProviderAccountName(it.accessToken)
        }
    }

    override fun refresh() {
        properties = provider.refreshToken(properties.refreshToken)
    }
}

open class ProviderException(msg: String) : RuntimeException(msg)
class InvalidProviderResponseException : ProviderException("The OAuth2 provider returned an invalid response.")
class ExistingConnectionException : ProviderException("The OAuth2 provider returned an account that has already been connected.")