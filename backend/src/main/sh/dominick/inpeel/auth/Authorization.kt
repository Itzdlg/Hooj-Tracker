package sh.dominick.inpeel.auth

import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse
import io.javalin.http.UnauthorizedResponse
import sh.dominick.inpeel.modules.accounts.data.Account

object Authorization {
    private val handlers = mutableMapOf<String, (String) -> AuthData>()
    fun registerType(type: String, handler: (String) -> AuthData) {
        handlers[type.lowercase()] = handler
    }

    operator fun get(ctx: Context): AuthData {
        val header = ctx.header("Authorization")
            ?: throw UnauthorizedResponse("Missing Authorization Header")

        if (!header.contains(" "))
            throw UnauthorizedResponse("Missing Valid Authorization Header")

        val type = header.substringBefore(" ").lowercase()
        val data = header.substringAfter(" ")

        try {
            return handlers[type.lowercase()]?.invoke(data)
                ?: throw UnauthorizedResponse("Missing valid authorization header")
        } catch (ex: ParseException) {
            throw ForbiddenResponse(ex.message ?: "Incorrect credentials.")
        }
    }
}

open class ParseException(msg: String) : Exception(msg)

interface AuthData {
    val account: Account
}