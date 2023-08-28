package sh.dominick.hoojtracker.modules.oauth2.routes

import io.javalin.community.routing.annotations.Body
import io.javalin.community.routing.annotations.Post
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.ServiceUnavailableResponse
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.hoojtracker.modules.accounts.data.AccountCredentials
import sh.dominick.hoojtracker.modules.oauth2.OAuth2ConnectionsModule

object OAuth2ConnectionsController {
    data class CreateOAuth2Request(
        val provider: String,
        val code: String,

        val email: String?,
        val name: String?,
        val password: String?
    )

    @Post("/signup/oauth2")
    fun createOAuth2(ctx: Context, @Body request: CreateOAuth2Request) {
        if (OAuth2ConnectionsModule.DISABLED)
            throw ServiceUnavailableResponse()

        val providers = OAuth2ConnectionsModule.providers

        transaction {
            val provider = providers.firstOrNull { it.apiId.equals(request.provider, ignoreCase = true) } ?:
                throw BadRequestResponse("OAuth2 provider '${request.provider}' was not found.")

            if (!provider.acceptingRegistrations)
                throw ServiceUnavailableResponse()

            val result = provider.connect(request.code) {
                if (request.name == null)
                    this.name = it
                else this.name = request.name
            }

            result.first.email = request.email

            if (request.password != null)
                AccountCredentials.new(result.first, request.password)

            ctx.json(mapOf(
                "account" to result.first.dto()
            ))
        }
    }
}