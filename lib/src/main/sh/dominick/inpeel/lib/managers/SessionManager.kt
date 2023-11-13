package sh.dominick.inpeel.lib.managers

import com.google.gson.GsonBuilder
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.inpeel.lib.data.oauth2.providers.ProviderException
import sh.dominick.inpeel.lib.data.sessions.sql.Session
import java.time.Instant
import java.util.*

open class SessionManager<T>(val cookieKey: String) {
    init {
        AuthorizationManager.registerType("Bearer") {
            val sessionId = try {
                UUID.fromString(it)
            } catch (ex: IllegalArgumentException) {
                throw BadRequestResponse("The provided Bearer token is not a UUID.")
            }

            this[sessionId] ?: throw ProviderException("That session is not valid.")
        }
    }

    var gson = GsonBuilder().create()

    protected val types = mutableMapOf<String, T>()

    fun registerType(key: String, handler: T) {
        if (types.containsKey(key.lowercase()))
            throw IllegalStateException()

        types[key.lowercase()] = handler
    }

    fun getType(key: String): T? = types[key.lowercase()]

    operator fun get(ctx: Context): Session? {
        val cookie = ctx.cookie(cookieKey)

        if (cookie.isNullOrBlank()) {
            val auth = AuthorizationManager[ctx]
            if (auth !is Session)
                throw ForbiddenResponse()

            return this[auth.id.value]
        }

        return try {
            this[UUID.fromString(cookie)]
        } catch (ex: IllegalArgumentException) {
            throw BadRequestResponse("The provided Bearer token is not a UUID.")
        }
    }

    operator fun get(sessionId: UUID): Session? {
        return transaction {
            val session = Session.findById(sessionId)
                ?: return@transaction null

            if (session.expiresAt.isBefore(Instant.now())) {
                session.delete()
                return@transaction null
            }

            return@transaction session
        }
    }
}