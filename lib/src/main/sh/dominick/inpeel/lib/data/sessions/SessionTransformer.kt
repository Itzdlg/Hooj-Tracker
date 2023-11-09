package sh.dominick.inpeel.lib.data.sessions

import com.google.gson.JsonObject
import io.javalin.http.Context
import sh.dominick.inpeel.lib.data.sessions.sql.Session

fun interface SessionTransformer {
    operator fun invoke(body: JsonObject, ctx: Context, rememberMe: Boolean): Session
}