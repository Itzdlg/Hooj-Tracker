package sh.dominick.hoojtracker.modules.sessions.data

import com.google.gson.JsonParser
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import sh.dominick.hoojtracker.auth.AuthData
import sh.dominick.hoojtracker.modules.accounts.data.Account
import sh.dominick.hoojtracker.modules.accounts.data.AccountsTable
import sh.dominick.hoojtracker.prettyGson
import sh.dominick.hoojtracker.util.transformInstant
import java.util.*

object SessionsTable : UUIDTable("sessions") {
    val account = reference("account", AccountsTable, onDelete = ReferenceOption.CASCADE)

    val createdAt = long("created_at")
    val expiresAt = long("expires_at")

    val metadata = text("metadata", eagerLoading = true)
}

class Session(id: EntityID<UUID>): UUIDEntity(id), AuthData {
    companion object : EntityClass<UUID, Session>(SessionsTable)

    override var account by Account referencedOn SessionsTable.account

    var createdAt by SessionsTable.createdAt.transformInstant()
    var expiresAt by SessionsTable.expiresAt.transformInstant()

    var metadata by SessionsTable.metadata.transform(
        toReal = { JsonParser.parseString(it).asJsonObject },
        toColumn = { prettyGson.toJson(it) }
    )

    fun <T> metadata(clazz: Class<T>): T {
        return prettyGson.fromJson(metadata, clazz)
    }

    fun dto() = mapOf(
        "id" to id.value,
        "account" to account.dto(),
        "createdAt" to createdAt.toEpochMilli(),
        "expiresAt" to expiresAt.toEpochMilli()
    )
}