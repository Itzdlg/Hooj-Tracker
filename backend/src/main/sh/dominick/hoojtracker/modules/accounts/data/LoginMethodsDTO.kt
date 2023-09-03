package sh.dominick.hoojtracker.modules.accounts.data

import sh.dominick.hoojtracker.modules.accounts.oauth2.OAuth2ConnectionsModule
import sh.dominick.hoojtracker.modules.accounts.oauth2.data.connections
import sh.dominick.hoojtracker.modules.accounts.passwords.data.activePassword

class LoginMethodsDTO(
    val password: Boolean,
    val connections: Map<String, Boolean>
) {
    constructor(account: Account) : this(
        password = account.activePassword != null,
        connections = OAuth2ConnectionsModule.providers
            .associateBy { it.apiId }
            .mapValues { account.connections[it.value] != null }
    )
}