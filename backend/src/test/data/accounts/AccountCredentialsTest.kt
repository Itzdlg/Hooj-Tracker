package data.accounts

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import mock.TestingDatabase
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.BeforeClass
import org.junit.Test
import sh.dominick.hoojtracker.data.accounts.*
import java.time.Instant

class AccountCredentialsTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            TestingDatabase.connect(
                AccountsTable,
                AccountCredentialsTable
            )
        }
    }

    private fun createAccount(password: String?): Account {
        val account = Account.new {
            this.email = "test"
            this.name = "test"

            this.createdAt = Instant.now()
            this.updated()
        }

        if (password != null)
            AccountCredentials.new(account, password)

        return account
    }

    @Test
    fun `Account With Password Returns An Active Credential`() {
        transaction {
            val account = createAccount("1")

            val credentials = account.activeCredentials
            assertTrue(credentials != null)
        }
    }

    @Test
    fun `Account With Password Returns Correct isPassword`() {
        transaction {
            val password = "password"

            val account = createAccount(password)

            assertTrue(
                "The correct password was not accepted.",
                account.isPassword(password)
            )

            assertFalse(
                "The incorrect password was accepted.",
                account.isPassword(password.substring(1))
            )
        }
    }

    @Test
    fun `Account Without Password Returns Null Active Credential`() {
        transaction {
            val account = createAccount(null)

            val credentials = account.activeCredentials
            assertTrue(credentials == null)
        }
    }

    @Test
    fun `Several Passwords Returns Correct Credentials`() {
        transaction {
            val account = createAccount(null)

            AccountCredentials.new(account, "1")
            AccountCredentials.new(account, "2")
            AccountCredentials.new(account, "3")

            assertTrue(
                "An incorrect number of credentials were saved",
                account.credentials?.count() == 3L
            )

            val credentials = account.activeCredentials
            assertTrue(
                "No active credentials were returned",
                credentials != null
            )

            assertTrue(
                "The wrong active credentials were returned",
                credentials!!.isPassword("3")
            )
        }
    }
}