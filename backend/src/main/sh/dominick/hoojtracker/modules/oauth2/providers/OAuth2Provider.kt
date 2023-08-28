package sh.dominick.hoojtracker.modules.oauth2.providers

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow
import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.util.store.DataStore
import sh.dominick.hoojtracker.Env
import sh.dominick.hoojtracker.modules.accounts.data.Account
import sh.dominick.hoojtracker.modules.oauth2.data.OAuth2Connection
import java.lang.RuntimeException

abstract class OAuth2Provider(val id: Int, val apiId: String) {
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