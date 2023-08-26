package sh.dominick.hoojtracker.data.oauth2

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.util.store.DataStore
import com.google.api.client.util.store.DataStoreFactory
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.hoojtracker.data.accounts.Account
import sh.dominick.hoojtracker.data.oauth2.providers.OAuth2Provider
import java.io.Serializable
import java.util.*

class ExposedCredentialDataStore(val provider: OAuth2Provider) : DataStore<StoredCredential> {
    companion object {
        fun wrapId(userId: String): UUID
            = UUID.fromString(userId)

        fun unwrapId(userId: UUID): String
            = userId.toString()
    }

    override fun getDataStoreFactory(): DataStoreFactory {
        return object : DataStoreFactory {
            override fun <V : Serializable?> getDataStore(p0: String?): DataStore<V> {
                return ExposedCredentialDataStore(provider) as DataStore<V>
            }

        }
    }

    override fun getId(): String {
        return "exposed-credential-store"
    }

    override fun size(): Int {
        return transaction {
            OAuth2ConnectionsTable.selectAll().count().toInt()
        }
    }

    override fun isEmpty(): Boolean {
        return size() == 0
    }

    override fun containsKey(userId: String): Boolean {
        return transaction {
            OAuth2ConnectionsTable.select {
                OAuth2ConnectionsTable.account eq wrapId(userId)
            }.count() > 0
        }
    }

    override fun keySet(): MutableSet<String> {
        return transaction {
            OAuth2Connection.all().map { unwrapId(it.account.id.value) }.toMutableSet()
        }
    }

    override fun values(): MutableCollection<StoredCredential> {
        return transaction {
            OAuth2Connection.all().map { it.storedCredential }.toMutableSet()
        }
    }

    override fun get(userId: String): StoredCredential {
        return transaction {
            OAuth2Connection.find { OAuth2ConnectionsTable.account eq wrapId(userId) }.first().storedCredential
        }
    }

    override fun clear(): DataStore<StoredCredential> {
        transaction {
            OAuth2ConnectionsTable.deleteAll()
        }

        return this
    }

    override fun delete(userId: String): DataStore<StoredCredential> {
        transaction {
            OAuth2Connection.find { OAuth2ConnectionsTable.account eq wrapId(userId) }.firstOrNull()?.delete()
        }

        return this
    }

    override fun set(userId: String, storedCredential: StoredCredential?): DataStore<StoredCredential> {
        if (storedCredential == null) {
            this.delete(userId)
            return this
        }

        val accountId = wrapId(userId)

        transaction {
            val entry = OAuth2Connection.find { OAuth2ConnectionsTable.account eq accountId }.firstOrNull()

            if (entry != null) {
                entry.storedCredential = storedCredential
                return@transaction
            }

            val account = Account.findById(accountId)
                ?: throw IllegalArgumentException("The provided userId does not match an existing account.")

            OAuth2Connection.new {
                this.account = account
                this.storedCredential = storedCredential
                this.provider = this@ExposedCredentialDataStore.provider
            }
        }

        return this
    }

    override fun containsValue(storedCredential: StoredCredential?): Boolean {
        if (storedCredential == null)
            return false

        return transaction {
            OAuth2Connection.find { OAuth2ConnectionsTable.accessToken eq storedCredential.accessToken }.count() > 0
        }
    }

}