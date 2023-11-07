package sh.dominick.inpeel.modules.accounts.passwords

import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.inpeel.auth.AuthData
import sh.dominick.inpeel.auth.ParseException
import sh.dominick.inpeel.modules.accounts.data.Account
import sh.dominick.inpeel.modules.accounts.data.AccountsTable
import sh.dominick.inpeel.modules.accounts.passwords.data.isPassword

class BasicAuth(val email: String, val hashedPassword: String, override val account: Account): AuthData {
    class ImproperlyFormattedException : ParseException("The provided authorization has not been formatted correctly.")
    class NoAccountFoundException : ParseException("The provided authorization has not matched any accounts.")
    class IncorrectPasswordException : ParseException("The provided authorization does not match the account's password.")

    companion object {
        fun match(data: String): BasicAuth {
            if (!data.contains(":"))
                throw ImproperlyFormattedException()

            val dataEmail = data.substringBefore(":")
            val dataPassword = data.substringAfter(":")

            return transaction {
                val account = Account.find {
                    AccountsTable.email.lowerCase() eq dataEmail.lowercase()
                }.firstOrNull() ?: throw NoAccountFoundException()

                if (!account.isPassword(dataPassword))
                    throw IncorrectPasswordException()

                return@transaction BasicAuth(dataEmail, dataPassword, account)
            }
        }
    }
}