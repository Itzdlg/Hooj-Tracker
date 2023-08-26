package sh.dominick.hoojtracker.data.accounts

import de.mkammerer.argon2.Argon2Factory
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.insert
import sh.dominick.hoojtracker.data.accounts.Account.Companion.referrersOn
import sh.dominick.hoojtracker.data.config.Configuration
import sh.dominick.hoojtracker.util.transformInstant
import java.time.Instant

object AccountCredentialsTable : IntIdTable("account_credentials") {
    val account = reference("account", AccountsTable)

    val createdAt = long("created_at")

    val argon2Output = text("argon2_output", eagerLoading = true)
}

class AccountCredentials(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, AccountCredentials>(AccountCredentialsTable) {
        fun new(account: Account, password: String) {
            val argon2 = Argon2Factory.create(
                Configuration.PASSWORD_SALT_LENGTH,
                1024
            )

            val passwordArray = password.toCharArray()
            val hashedPassword = try {
                argon2.hash(
                    Configuration.PASSWORD_HASH_ITERATIONS,
                    Configuration.ARGON2_MEMORY,
                    Configuration.ARGON2_PARALLELISM,
                    passwordArray
                )
            } finally {
                argon2.wipeArray(passwordArray)
            }

            AccountCredentialsTable.insert {
                it[AccountCredentialsTable.account] = account.id
                it[AccountCredentialsTable.createdAt] = Instant.now().toEpochMilli()

                it[AccountCredentialsTable.argon2Output] = hashedPassword
            }
        }
    }

    val account by Account referencedOn AccountCredentialsTable.account

    val createdAt by AccountCredentialsTable.createdAt.transformInstant()

    val argon2Output by AccountCredentialsTable.argon2Output

    fun isPassword(password: String): Boolean {
        val argon2 = Argon2Factory.create()
        val passwordArray = password.toCharArray()

        return try {
            argon2.verify(this.argon2Output, passwordArray)
        } finally {
            argon2.wipeArray(passwordArray)
        }
    }
}

val Account.credentials: SizedIterable<AccountCredentials>? by AccountCredentials referrersOn AccountCredentialsTable.account
val Account.activeCredentials: AccountCredentials?
    get() = credentials?.maxByOrNull { it.createdAt }

fun Account.isPassword(password: String): Boolean
    = this.activeCredentials?.isPassword(password) ?: false