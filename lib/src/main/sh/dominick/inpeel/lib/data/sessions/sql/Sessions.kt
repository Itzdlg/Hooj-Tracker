package sh.dominick.inpeel.lib.data.sessions.sql

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.ReferenceOption
import sh.dominick.inpeel.lib.managers.AuthData
import sh.dominick.inpeel.lib.data.sql.Account
import sh.dominick.inpeel.lib.data.sql.AccountsTable
import sh.dominick.inpeel.lib.data.sql.dto
import sh.dominick.inpeel.lib.util.transformInstant
import java.util.*

object SessionsTable : UUIDTable("sessions") {
    val account = reference("account", AccountsTable, onDelete = ReferenceOption.CASCADE)

    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
    val expiresAt = long("expires_at")

    val metadata = text("metadata", eagerLoading = true).clientDefault { "{}" }
}

class Session(id: EntityID<UUID>): UUIDEntity(id), AuthData {
    companion object : EntityClass<UUID, Session>(SessionsTable) {
        val gson: Gson = GsonBuilder()
            .create()

        override fun findById(id: EntityID<UUID>): Session?
            = super.findById(id)?.load(Session::account)
    }

    override var account by Account referencedOn SessionsTable.account

    var createdAt by SessionsTable.createdAt.transformInstant()
    var expiresAt by SessionsTable.expiresAt.transformInstant()

    var metadata by SessionsTable.metadata.transform(
        toReal = { JsonParser.parseString(it).asJsonObject },
        toColumn = { gson.toJson(it) }
    )

    fun <T> metadata(clazz: Class<T>): T {
        return gson.fromJson(metadata, clazz)
    }

    fun dto() = mapOf(
        "id" to id.value.toString(),
        "account" to account.dto(),
        "createdAt" to createdAt.toEpochMilli(),
        "expiresAt" to expiresAt.toEpochMilli()
    )
}