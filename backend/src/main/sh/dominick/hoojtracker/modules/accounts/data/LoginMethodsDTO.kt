package sh.dominick.hoojtracker.modules.accounts.data

import sh.dominick.hoojtracker.modules.oauth2.OAuth2ConnectionsModule
import sh.dominick.hoojtracker.modules.oauth2.data.connections

class LoginMethodsDTO(
    val password: Boolean,
    val connections: Map<String, Boolean>
) {
    constructor(account: Account) : this(
        password = account.activeCredentials != null,
        connections = OAuth2ConnectionsModule.providers
            .associateBy { it.apiId }
            .mapValues { account.connections[it.value] != null }
    )
}