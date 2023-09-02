package sh.dominick.hoojtracker.modules.sessions

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.javalin.community.routing.annotations.AnnotatedRoutingPlugin
import io.javalin.config.JavalinConfig
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.hoojtracker.auth.Authorization
import sh.dominick.hoojtracker.modules.Module
import sh.dominick.hoojtracker.modules.oauth2.providers.ProviderException
import sh.dominick.hoojtracker.modules.sessions.data.Session
import sh.dominick.hoojtracker.modules.sessions.data.SessionsTable
import sh.dominick.hoojtracker.modules.sessions.routes.SessionsController
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.UUID

object SessionsModule : Module("sessions") {
    const val COOKIE_KEY = "session"

    override val routes = setOf(
        SessionsController
    )

    override val tables = setOf(
        SessionsTable
    )

    private val types = mutableMapOf<String, SessionTransformer>()

    fun registerType(key: String, handler: SessionTransformer) {
        if (types.containsKey(key.lowercase()))
            throw IllegalStateException()

        types[key.lowercase()] = handler
    }

    fun getType(key: String): SessionTransformer? = types[key.lowercase()]

    operator fun get(ctx: Context): Session? {
        val cookie = ctx.cookie(COOKIE_KEY)

        if (cookie.isNullOrBlank()) {
            val auth = Authorization[ctx]
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

    override fun load(routingPlugin: AnnotatedRoutingPlugin, gsonBuilder: GsonBuilder, javalinConfig: JavalinConfig) {
        super.load(routingPlugin, gsonBuilder, javalinConfig)

        Authorization.registerType("Bearer") {
            val sessionId = try {
                UUID.fromString(it)
            } catch (ex: IllegalArgumentException) {
                throw BadRequestResponse("The provided Bearer token is not a UUID.")
            }

            this[sessionId] ?: throw ProviderException("That session is not valid.")
        }
    }
}

fun interface SessionTransformer {
    fun invoke(body: JsonObject, ctx: Context, rememberMe: Boolean): Session
}