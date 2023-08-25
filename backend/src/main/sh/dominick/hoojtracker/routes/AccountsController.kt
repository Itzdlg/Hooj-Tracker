package sh.dominick.hoojtracker.routes

import io.javalin.community.routing.annotations.*
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.hoojtracker.data.accounts.Account
import sh.dominick.hoojtracker.data.accounts.AccountCredentials
import sh.dominick.hoojtracker.data.accounts.AccountsTable
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
}