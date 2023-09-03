package sh.dominick.hoojtracker.modules.accounts.passwords.data

import de.mkammerer.argon2.Argon2Factory
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.insert
import sh.dominick.hoojtracker.modules.accounts.data.Account.Companion.referrersOn
import sh.dominick.hoojtracker.data.config.Configuration
import sh.dominick.hoojtracker.modules.accounts.data.Account
import sh.dominick.hoojtracker.modules.accounts.data.AccountsTable
import sh.dominick.hoojtracker.util.transformInstant

object AccountPasswordsTable : IntIdTable("account_passwords") {
    val account = reference("account", AccountsTable, onDelete = ReferenceOption.CASCADE)
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }

    val argon2Output = text("argon2_output", eagerLoading = true)
}

class AccountPassword(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, AccountPassword>(AccountPasswordsTable) {
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

            AccountPasswordsTable.insert {
                it[AccountPasswordsTable.account] = account.id
                it[argon2Output] = hashedPassword
            }
        }
    }

    val account by Account referencedOn AccountPasswordsTable.account
    val createdAt by AccountPasswordsTable.createdAt.transformInstant()

    val argon2Output by AccountPasswordsTable.argon2Output

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

val Account.passwords: SizedIterable<AccountPassword>? by AccountPassword referrersOn AccountPasswordsTable.account
val Account.activePassword: AccountPassword?
    get() = passwords?.maxByOrNull { it.createdAt }

fun Account.isPassword(password: String): Boolean
    = this.activePassword?.isPassword(password) ?: false