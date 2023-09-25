package sh.dominick.hoojtracker.modules.accounts.data

import io.javalin.validation.JavalinValidation
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.hoojtracker.util.transformInstant
import java.util.*

object AccountsTable : UUIDTable("accounts") {
    val email = varchar("email", 320).nullable()
    val name = varchar("name", 48)

    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
    val updatedAt = long("updated_at").clientDefault { System.currentTimeMillis() }
}

class Account(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Account>(AccountsTable) {
        init {
            JavalinValidation.register(Account::class.java) {
                val uuid = UUID.fromString(it)

                return@register transaction {
                    Account.findById(uuid)
                }
            }
        }
    }

    var email by AccountsTable.email
    var name by AccountsTable.name

    var createdAt by AccountsTable.createdAt.transformInstant()
    var updatedAt by AccountsTable.updatedAt.transformInstant()
}

open class AccountDTO(
    val id: UUID,
    val email: String?,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)

fun Account.dto() = AccountDTO(
    id = id.value,
    email = email,
    name = name,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli()
)