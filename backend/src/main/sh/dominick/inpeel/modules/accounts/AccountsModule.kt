package sh.dominick.inpeel.modules.accounts

import sh.dominick.inpeel.modules.Module
import sh.dominick.inpeel.modules.accounts.data.AccountsTable
import sh.dominick.inpeel.modules.accounts.oauth2.OAuth2ConnectionsModule
import sh.dominick.inpeel.modules.accounts.passwords.PasswordsModule
import sh.dominick.inpeel.modules.accounts.passwords.data.AccountPasswordsTable
import sh.dominick.inpeel.modules.accounts.routes.AccountRoutes

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