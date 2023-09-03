package sh.dominick.hoojtracker.modules.accounts.oauth2.routes

import com.google.api.client.auth.oauth2.TokenResponseException
import io.javalin.community.routing.annotations.Body
import io.javalin.community.routing.annotations.Post
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.ServiceUnavailableResponse
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.hoojtracker.modules.accounts.data.Account
import sh.dominick.hoojtracker.modules.accounts.passwords.data.AccountPassword
import sh.dominick.hoojtracker.modules.accounts.oauth2.OAuth2ConnectionsModule
import sh.dominick.hoojtracker.modules.accounts.oauth2.data.OAuth2Connection
import sh.dominick.hoojtracker.modules.accounts.oauth2.data.OAuth2ConnectionsTable

object OAuth2ConnectionsController {
    data class CreateOAuth2Request(
        val provider: String,
        val code: String,

        val email: String?,
        val name: String?,
        val password: String?
    )

    @Post("/accounts/signup/oauth2")
    fun createOAuth2(ctx: Context, @Body request: CreateOAuth2Request) {
        if (OAuth2ConnectionsModule.DISABLED)
            throw ServiceUnavailableResponse()

        val providers = OAuth2ConnectionsModule.providers

        transaction {
            val provider = providers.firstOrNull { it.apiId.equals(request.provider, ignoreCase = true) } ?:
                throw BadRequestResponse("OAuth2 provider '${request.provider}' was not found.")

            if (!provider.acceptingRegistrations)
                throw ServiceUnavailableResponse()

            val grant = try {
                provider.requestToken(request.code)
            } catch (ex: TokenResponseException) {
                throw ConflictResponse("The provided code was not valid.")
            }

            val exists = OAuth2ConnectionsTable.select {
                OAuth2ConnectionsTable.providerAccountId eq grant.providerAccountId
            }.count() > 0

            if (exists)
                throw ConflictResponse("An account already exists with this connection.")

            val account = Account.new {
                this.name = request.name ?: grant.providerAccountName
                this.email = request.email
            }

            val connection = OAuth2Connection.new {
                this.account = account

                this.provider = provider
                this.providerAccountId = grant.providerAccountId

                grant.withProperties {
                    this.refreshToken = it.refreshToken
                }
            }

            if (request.password != null) {
                if (request.email == null)
                    throw ConflictResponse("You must provide an email to set a password.")

                AccountPassword.new(account, request.password)
            }

            ctx.json(mapOf(
                "account" to account.dto()
            ))
        }
    }
}