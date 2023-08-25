package sh.dominick.hoojtracker.data.accounts

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import sh.dominick.hoojtracker.util.transformInstant
import java.time.Instant
import java.util.*

object AccountsTable : UUIDTable("accounts") {
    val email = varchar("email", 320).nullable()
    val name = varchar("name", 48)

    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
}

class Account(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Account>(AccountsTable)

    var email by AccountsTable.email
    var name by AccountsTable.name

    var createdAt by AccountsTable.createdAt.transformInstant()
    var updatedAt by AccountsTable.updatedAt.transformInstant()

    fun updated() {
        updatedAt = Instant.now()
    }

    fun dto() = AccountDTO(
        id = id.value,
        email = email,
        name = name,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
        loginMethods = LoginMethodsDTO(this)
    )
}

open class AccountDTO(
    val id: UUID,
    val email: String?,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val loginMethods: LoginMethodsDTO
)