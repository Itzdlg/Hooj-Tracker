﻿package sh.dominick.inpeel.rest.routes

import io.javalin.community.routing.annotations.Body
import io.javalin.community.routing.annotations.Endpoints
import io.javalin.community.routing.annotations.Post
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.inpeel.lib.data.passwords.sql.AccountPassword
import sh.dominick.inpeel.lib.data.sql.Account
import sh.dominick.inpeel.lib.data.sql.AccountsTable
import sh.dominick.inpeel.lib.data.sql.dto

@Endpoints("/accounts")
object AccountRoutes {
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
            }

            AccountPassword.new(account, request.password)

            ctx.json(
                mapOf("account" to account.dto())
            )
        }
    }
}