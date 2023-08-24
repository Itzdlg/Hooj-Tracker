package sh.dominick.hoojtracker.data

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import sh.dominick.hoojtracker.Env
import sh.dominick.hoojtracker.util.argon2
import sh.dominick.hoojtracker.util.transformInstant
import java.time.Instant
import java.util.*

object AccountsTable : UUIDTable("accounts") {
    const val SALT_LENGTH = 32

    val email = varchar("email", 320).nullable()
    val name = varchar("name", 48)

    val password = text("password", eagerLoading = true).nullable()
    val salt = varchar("password_salt", SALT_LENGTH).nullable()

    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
}

class Account(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Account>(AccountsTable)

    var email by AccountsTable.email
    var name by AccountsTable.name

    var password by AccountsTable.password
    var salt by AccountsTable.salt

    var createdAt by AccountsTable.createdAt.transformInstant()
    var updatedAt by AccountsTable.updatedAt.transformInstant()

    fun isPassword(password: String): Boolean {
        if (this.password == null || this.salt == null)
            return false

        val salted = password + this.salt
        val hashed = argon2(salted, Env.PASSWORD_HASH_ITERATIONS)

        return this.password == hashed
    }

    fun updated() {
        updatedAt = Instant.now()
    }

    fun dto() = AccountDTO(
        id = id.value,
        email = email,
        name = name,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
        loginMethods = AccountLoginMethodsDTO(
            password = password != null && salt != null
        )
    )
}

open class AccountDTO(
    val id: UUID,
    val email: String?,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val loginMethods: AccountLoginMethodsDTO
)

open class AccountLoginMethodsDTO(
    val password: Boolean
)