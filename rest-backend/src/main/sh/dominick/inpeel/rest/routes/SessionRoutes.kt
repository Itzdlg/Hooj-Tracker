package sh.dominick.inpeel.rest.routes

import com.google.gson.JsonParser
import io.javalin.community.routing.annotations.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.inpeel.rest.sessions.RestSessionManager
import java.lang.IllegalArgumentException

@Endpoints("/sessions")
object SessionRoutes {
    @Post
    fun create(ctx: Context) {
        val body = JsonParser.parseString(ctx.body()).asJsonObject
            ?: throw BadRequestResponse("Missing JSON body.")

        val rememberMe = body["rememberMe"]?.asBoolean ?: false

        val type = body["type"].asString
            ?: throw BadRequestResponse("Missing `type` property.")

        val handler = RestSessionManager.getType(type)
            ?: throw BadRequestResponse("That is not a valid session type.")

        val session = try {
            handler(body, ctx, rememberMe)
        } catch (ex: IllegalArgumentException) {
            throw BadRequestResponse(ex.message ?: "Invalid JSON provided for the specified type.")
        }

        ctx.cookie(RestSessionManager.cookieKey, session.id.value.toString())

        ctx.json(mapOf(
            "session" to session.dto()
        ))
    }

    @Delete("/@")
    fun logout(ctx: Context) {
        val session = RestSessionManager[ctx]
            ?: throw ForbiddenResponse()

        transaction {
            session.delete()
            ctx.removeCookie(RestSessionManager.cookieKey)
        }
    }

    @Get("/@")
    fun info(ctx: Context) {
        val session = RestSessionManager[ctx]
            ?: throw ForbiddenResponse()

        ctx.json(mapOf(
            "session" to session.dto()
        ))
    }
}