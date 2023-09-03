package sh.dominick.hoojtracker.modules.accounts

import sh.dominick.hoojtracker.modules.Module
import sh.dominick.hoojtracker.modules.accounts.data.AccountsTable
import sh.dominick.hoojtracker.modules.accounts.oauth2.OAuth2ConnectionsModule
import sh.dominick.hoojtracker.modules.accounts.passwords.PasswordsModule
import sh.dominick.hoojtracker.modules.accounts.passwords.data.AccountPasswordsTable
import sh.dominick.hoojtracker.modules.accounts.routes.AccountRoutes

object AccountsModule : Module("accounts") {
    override val tables = setOf(
        AccountsTable,
        AccountPasswordsTable
    )

    override val routes = setOf(
        AccountRoutes
    )

    override val submodules = setOf(
        PasswordsModule,
        OAuth2ConnectionsModule
    )
}