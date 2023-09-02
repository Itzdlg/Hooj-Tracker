package sh.dominick.hoojtracker.modules.accounts

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.javalin.community.routing.annotations.AnnotatedRoutingPlugin
import io.javalin.config.JavalinConfig
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.hoojtracker.auth.Authorization
import sh.dominick.hoojtracker.modules.Module
import sh.dominick.hoojtracker.modules.accounts.data.*
import sh.dominick.hoojtracker.modules.accounts.routes.AccountsController
import sh.dominick.hoojtracker.modules.sessions.SessionsModule
import sh.dominick.hoojtracker.modules.sessions.data.Session
import java.time.Instant
import java.time.temporal.ChronoUnit

object AccountsModule : Module("accounts") {
    override val tables = setOf(
        AccountsTable,
        AccountCredentialsTable
    )

    override val routes = setOf(
        AccountsController
    )

    override fun load(routingPlugin: AnnotatedRoutingPlugin, gsonBuilder: GsonBuilder, javalinConfig: JavalinConfig) {
        super.load(routingPlugin, gsonBuilder, javalinConfig)

        Authorization.registerType("Basic") {
            BasicAuth.match(it)
        }

        SessionsModule.registerType("password") {
            val email = it.get("email")?.asString
                ?: throw IllegalArgumentException("The provided body is missing an email.")

            val password = it.get("password")?.asString
                ?: throw IllegalArgumentException("The provided body is missing a password.")

            val rememberMe = it.get("rememberMe")?.asBoolean ?: false

            transaction {
                val account = Account.find {
                    AccountsTable.email.lowerCase() eq email.lowercase()
                }.firstOrNull() ?: throw IllegalArgumentException("No account with that email was found.")

                if (!account.isPassword(password))
                    throw IllegalArgumentException("The provided password is not correct.")

                val session = Session.new {
                    this.account = account
                    this.createdAt = Instant.now()
                    this.expiresAt =
                        if (rememberMe)
                            Instant.now().plus(30, ChronoUnit.DAYS)
                        else Instant.now().plus(6, ChronoUnit.HOURS)
                    this.metadata = JsonObject()
                }

                return@transaction session
            }
        }
    }
}