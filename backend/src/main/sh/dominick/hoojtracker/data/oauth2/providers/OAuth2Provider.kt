package sh.dominick.hoojtracker.data.oauth2.providers

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow
import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.util.store.DataStore
import sh.dominick.hoojtracker.Env
import sh.dominick.hoojtracker.data.accounts.Account
import sh.dominick.hoojtracker.data.oauth2.OAuth2Connection
import java.lang.RuntimeException

abstract class OAuth2Provider(val id: Int, val apiId: String) {
    companion object {
        private val providers = mutableMapOf<Int, OAuth2Provider>()

        fun register(provider: OAuth2Provider) {
            if (providers.containsKey(provider.id))
                throw IllegalArgumentException("A provider has already been registered with that ID")

            providers[provider.id] = provider
        }

        fun values(): Set<OAuth2Provider> {
            return providers.values.toSet()
        }

        fun fromId(id: Int): OAuth2Provider? {
            return providers[id]
        }

        fun fromApiId(id: String): OAuth2Provider? {
            return providers.values.toSet().firstOrNull { it.apiId.equals(id, ignoreCase = true) }
        }
    }

    abstract val flow: AuthorizationCodeFlow
    abstract val dataStore: DataStore<StoredCredential>

    abstract val clientId: String
    abstract val clientSecret: String

    abstract val acceptingRegistrations: Boolean
    abstract val acceptingLogins: Boolean

    open val redirectUri: String
        = Env.OAUTH2_REDIRECT_URI.replace("{provider}", apiId)

    abstract fun connect(code: String, applyName: Account.(String) -> (Unit)): Pair<Account, OAuth2Connection>
}

open class ProviderException(msg: String) : RuntimeException(msg)
class InvalidProviderResponseException : ProviderException("The OAuth2 provider returned an invalid response.")
class ExistingConnectionException : ProviderException("The OAuth2 provider returned an account that has already been connected.")