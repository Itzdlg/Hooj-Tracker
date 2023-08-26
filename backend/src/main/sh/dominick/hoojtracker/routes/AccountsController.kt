package sh.dominick.hoojtracker.routes

import io.javalin.community.routing.annotations.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.hoojtracker.data.accounts.Account
import sh.dominick.hoojtracker.data.accounts.AccountCredentials
import sh.dominick.hoojtracker.data.accounts.AccountsTable
import sh.dominick.hoojtracker.data.oauth2.providers.OAuth2Provider
import java.time.Instant

@Endpoints("/accounts")
object AccountsController {
    data class CreateNormalRequest(
        val email: String,
        val name: String,
        val password: String
    )

    @Post("/signup")
    fun create(ctx: Context, @Body request: CreateNormalRequest) {
        transaction {
            if (Account.find { AccountsTable.email.lowerCase() eq request.email.lowercase() }.count() > 0)
                throw ConflictResponse("An account with that email already exists.")

            val account = Account.new {
                this.email = request.email.lowercase()
                this.name = request.name

                this.createdAt = Instant.now()
                this.updated()
            }

            AccountCredentials.new(account, request.password)

            ctx.json(
                mapOf("account" to account.dto())
            )
        }
    }

    data class CreateOAuth2Request(
        val provider: String,
        val code: String,

        val email: String?,
        val name: String?,
        val password: String?
    )

    @Post("/signup/oauth2")
    fun createOAuth2(ctx: Context, @Body request: CreateOAuth2Request) {
        val provider = OAuth2Provider.fromApiId(request.provider) ?:
            throw BadRequestResponse("OAuth2 provider '${request.provider}' was not found.")

        val result = provider.connect(request.code) {
            if (request.name == null)
                this.name = it
            else this.name = request.name
        }

        transaction {
            result.first.email = request.email

            if (request.password != null)
                AccountCredentials.new(result.first, request.password)

            ctx.json(mapOf(
                "account" to result.first.dto()
            ))
        }
    }
}