package sh.dominick.hoojtracker.modules.oauth2.data

import com.google.api.client.auth.oauth2.StoredCredential
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SizedIterable
import sh.dominick.hoojtracker.modules.accounts.data.Account
import sh.dominick.hoojtracker.modules.accounts.data.Account.Companion.referrersOn
import sh.dominick.hoojtracker.modules.accounts.data.AccountsTable
import sh.dominick.hoojtracker.modules.oauth2.OAuth2ConnectionsModule
import sh.dominick.hoojtracker.modules.oauth2.providers.OAuth2Provider
import sh.dominick.hoojtracker.util.transformInstant
import java.time.Instant

object OAuth2ConnectionsTable : IntIdTable("oauth2_connections") {
    val account = reference("account", AccountsTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val providerAccountId = text("provider_account_id", eagerLoading = true).nullable().default(null)

    val provider = integer("provider")

    val accessToken = text("access_token", eagerLoading = true)
    val refreshToken = text("refresh_token", eagerLoading = true)
    val expiration = long("expiration")

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

    var accessToken by OAuth2ConnectionsTable.accessToken
    var refreshToken by OAuth2ConnectionsTable.refreshToken
    var expiration by OAuth2ConnectionsTable.expiration.transformInstant()

    var storedCredential: StoredCredential
        get() {
            val obj = StoredCredential()
            obj.setAccessToken(this.accessToken)
            obj.setRefreshToken(this.refreshToken)
            obj.setExpirationTimeMilliseconds(this.expiration.toEpochMilli())

            return obj
        }
        set(value) {
            this.accessToken = value.accessToken
            this.refreshToken = value.refreshToken
            this.expiration = Instant.ofEpochMilli(value.expirationTimeMilliseconds)
        }
}

class AccountConnections(val account: Account, val connections: List<OAuth2Connection>) {
    operator fun get(provider: OAuth2Provider): OAuth2Connection? {
        return connections.firstOrNull { it.provider == provider }
    }
}

private val Account._connections: SizedIterable<OAuth2Connection>? by OAuth2Connection referrersOn OAuth2ConnectionsTable.account
val Account.connections: AccountConnections
    get() = AccountConnections(this, _connections?.toList() ?: emptyList())