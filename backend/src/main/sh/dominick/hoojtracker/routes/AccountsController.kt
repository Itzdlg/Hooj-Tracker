package sh.dominick.hoojtracker.routes

import io.javalin.community.routing.annotations.Body
import io.javalin.community.routing.annotations.Endpoints
import io.javalin.community.routing.annotations.Post
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.hoojtracker.Env
import sh.dominick.hoojtracker.data.Account
import sh.dominick.hoojtracker.data.AccountsTable
import sh.dominick.hoojtracker.util.argon2
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

@Endpoints("/accounts")
object AccountsController {
    data class CreateNormalRequest(
        val email: String,
        val name: String,
        val password: String
    )

    @Post("/signup")
    fun create(ctx: Context, @Body request: CreateNormalRequest) {
        var salt = ""
        for (i in 1..AccountsTable.SALT_LENGTH) {
            val random = ThreadLocalRandom.current().nextInt(Env.PASSWORD_SALT_CHARSET.length)
            val char = Env.PASSWORD_SALT_CHARSET[random]
            salt += char
        }

        val saltedPassword = request.password + salt
        val hashedPassword = argon2(saltedPassword, Env.PASSWORD_HASH_ITERATIONS)

        transaction {
            if (Account.find { AccountsTable.email.lowerCase() eq request.email.lowercase() }.count() > 0)
                throw ConflictResponse("An account with that email already exists.")

            val account = Account.new {
                this.email = request.email.lowercase()
                this.name = request.name

                this.password = hashedPassword
                this.salt = salt

                this.createdAt = Instant.now()
                this.updated()
            }

            ctx.json(
                mapOf("account" to account.dto())
            )
        }
    }
}