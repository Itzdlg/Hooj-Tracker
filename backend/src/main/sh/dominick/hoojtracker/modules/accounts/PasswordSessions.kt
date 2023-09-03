package sh.dominick.hoojtracker.modules.accounts

import com.google.gson.JsonObject
import io.javalin.http.Context
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.hoojtracker.modules.accounts.data.Account
import sh.dominick.hoojtracker.modules.accounts.data.AccountsTable
import sh.dominick.hoojtracker.modules.accounts.data.isPassword
import sh.dominick.hoojtracker.modules.sessions.SessionTransformer
import sh.dominick.hoojtracker.modules.sessions.data.Session
import sh.dominick.hoojtracker.util.toJsonObject
import java.time.Instant
import java.time.temporal.ChronoUnit

object PasswordSessionTransformer : SessionTransformer {
    override fun invoke(body: JsonObject, ctx: Context, rememberMe: Boolean): Session {
        val email = body.get("email")?.asString
            ?: throw IllegalArgumentException("The provided body is missing an email.")

        val password = body.get("password")?.asString
            ?: throw IllegalArgumentException("The provided body is missing a password.")

        return transaction {
            val account = Account.find {
                AccountsTable.email.lowerCase() eq email.lowercase()
            }.firstOrNull() ?: throw IllegalArgumentException("No account with that email was found.")

            if (!account.isPassword(password))
                throw IllegalArgumentException("The provided password is not correct.")

            Session.new {
                this.account = account
                this.expiresAt =
                    if (rememberMe)
                        Instant.now().plus(30, ChronoUnit.DAYS)
                    else Instant.now().plus(6, ChronoUnit.HOURS)
                this.metadata = PasswordSessionMetadata(email).toJsonObject()
            }
        }
    }
}

class PasswordSessionMetadata(val email: String)