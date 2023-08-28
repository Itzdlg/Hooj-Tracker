package sh.dominick.hoojtracker.modules.accounts

import sh.dominick.hoojtracker.modules.accounts.data.AccountCredentialsTable
import sh.dominick.hoojtracker.modules.accounts.data.AccountsTable
import sh.dominick.hoojtracker.modules.Module
import sh.dominick.hoojtracker.modules.accounts.routes.AccountsController

object AccountsModule : Module("accounts") {
    override val tables = setOf(
        AccountsTable,
        AccountCredentialsTable
    )

    override val routes = setOf(
        AccountsController
    )
}