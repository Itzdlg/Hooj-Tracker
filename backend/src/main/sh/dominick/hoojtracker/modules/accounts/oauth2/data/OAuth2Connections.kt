package sh.dominick.hoojtracker.modules.accounts.oauth2.data

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SizedIterable
import sh.dominick.hoojtracker.modules.accounts.data.Account
import sh.dominick.hoojtracker.modules.accounts.data.Account.Companion.referrersOn
import sh.dominick.hoojtracker.modules.accounts.data.AccountsTable
import sh.dominick.hoojtracker.modules.accounts.oauth2.OAuth2ConnectionsModule
import sh.dominick.hoojtracker.modules.accounts.oauth2.providers.OAuth2Grant
import sh.dominick.hoojtracker.modules.accounts.oauth2.providers.OAuth2Provider

object OAuth2ConnectionsTable : IntIdTable("oauth2_connections") {
    val account = reference("account", AccountsTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val providerAccountId = text("provider_account_id", eagerLoading = true).nullable().default(null)

    val provider = integer("provider")

    val refreshToken = text("refresh_token", eagerLoading = true)

    init {
        uniqueIndex(account, provider)
    }
}

class OAuth2Connection(id : EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, OAuth2Connection>(OAuth2ConnectionsTable)

    var account by Account referencedOn OAuth2ConnectionsTable.account

    var provider by OAuth2ConnectionsTable.provider.transform(
        toColumn = { it.id },
        toReal = { id -> OAuth2ConnectionsModule.providers.first { it.id == id } }
    )

    var providerAccountId by OAuth2ConnectionsTable.providerAccountId

    var refreshToken by OAuth2ConnectionsTable.refreshToken
}

class AccountConnections(val account: Account, val connections: List<OAuth2Connection>) {
    operator fun get(provider: OAuth2Provider): OAuth2Connection? {
        return connections.firstOrNull { it.provider == provider }
    }
}

private val Account._connections: SizedIterable<OAuth2Connection>? by OAuth2Connection referrersOn OAuth2ConnectionsTable.account
val Account.connections: AccountConnections
    get() = AccountConnections(this, _connections?.toList() ?: emptyList())

val OAuth2Grant.connection
    get() = sh.dominick.hoojtracker.modules.accounts.oauth2.data.OAuth2Connection.find {
        sh.dominick.hoojtracker.modules.accounts.oauth2.data.OAuth2ConnectionsTable.providerAccountId eq providerAccountId
    }.firstOrNull()