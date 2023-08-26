package sh.dominick.hoojtracker.data.accounts

import sh.dominick.hoojtracker.data.oauth2.connections
import sh.dominick.hoojtracker.data.oauth2.providers.OAuth2Provider

class LoginMethodsDTO(
    val password: Boolean,
    val connections: Map<String, Boolean>
) {
    constructor(account: Account) : this(
        password = account.activeCredentials != null,
        connections = OAuth2Provider.values()
            .associateBy { it.apiId }
            .mapValues { account.connections[it.value] != null }
    )
}