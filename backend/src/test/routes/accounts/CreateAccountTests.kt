package routes.accounts

import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.mockk.MockKAssertScope
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import mock.TestingDatabase
import org.junit.BeforeClass
import org.junit.Test
import sh.dominick.hoojtracker.data.accounts.AccountCredentialsTable
import sh.dominick.hoojtracker.data.accounts.AccountDTO
import sh.dominick.hoojtracker.data.accounts.AccountsTable
import sh.dominick.hoojtracker.routes.AccountsController

class CreateAccountTests {
    companion object {
        private val ctx = mockk<Context>(relaxed = true)

        @BeforeClass
        @JvmStatic
        fun setup() {
            TestingDatabase.connect(
                AccountsTable,
                AccountCredentialsTable
            )
        }

        private var index: Int = 0

        private fun newName(): String
            = "Test Smith " + (++index)

        private fun newEmail(): String
            = "test.smith" + (++index) + "@gmail.com"

        private fun MockKAssertScope.assertAndReadAccount(it: Any): AccountDTO {
            assertTrue(it is Map<*, *>)
            val uncheckedMap = it as Map<*, *>

            assertTrue(uncheckedMap.keys.all { it is String })

            val map = it as Map<String, *>
            assertTrue(map.containsKey("account"))

            val value = map["account"]!!
            assertTrue(value is AccountDTO)

            return value as AccountDTO
        }
    }

    @Test
    fun `Create a Normal Account Succeeds`() {
        val request = AccountsController.CreateNormalRequest(
            email = newEmail(),
            name = newName(),
            password = "1234567!"
        )

        AccountsController.create(ctx, request)

        verify {
            ctx.json(withArg {
                assertAndReadAccount(it)
            })
        }
    }

    @Test
    fun `Create a Normal Account With Uppercase Email Does Lowercase`() {
        val request = AccountsController.CreateNormalRequest(
            email = newEmail().uppercase(),
            name = newName(),
            password = "1234567!"
        )

        AccountsController.create(ctx, request)

        verify { ctx.json(withArg {
            val dto = assertAndReadAccount(it)

            assertTrue(dto.email == request.email.lowercase())
        }) }
    }

    @Test
    fun `Create a Normal Account With Password Includes Password Login Method`() {
        val request = AccountsController.CreateNormalRequest(
            email = newEmail(),
            name = newName(),
            password = "1234567!"
        )

        AccountsController.create(ctx, request)

        verify { ctx.json(withArg {
            val dto = assertAndReadAccount(it)
            assertTrue(dto.loginMethods.password)
        }) }
    }

    @Test(expected = ConflictResponse::class)
    fun `Create a Normal Account With Conflicting Email Throws ConflictResponse`() {
        val email = newEmail()

        AccountsController.create(
            ctx, AccountsController.CreateNormalRequest(
            email = email,
            name = newName(),
            password = "1234567!"
        ))

        verify { ctx.json(withArg {
            assertAndReadAccount(it)
        }) }

        AccountsController.create(
            ctx, AccountsController.CreateNormalRequest(
            email = email,
            name = newName(),
            password = "1234567!"
        ))
    }
}