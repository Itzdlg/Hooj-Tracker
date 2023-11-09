package sh.dominick.inpeel.lib.data.passwords

import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.inpeel.lib.managers.AuthData
import sh.dominick.inpeel.lib.managers.ParseException
import sh.dominick.inpeel.lib.data.passwords.sql.isPassword
import sh.dominick.inpeel.lib.data.sql.Account
import sh.dominick.inpeel.lib.data.sql.AccountsTable

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