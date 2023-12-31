package routes.accounts

import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.mockk.MockKAssertScope
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import sh.dominick.inpeel.lib.mock.TestingDatabase
import org.junit.BeforeClass
import org.junit.Test
import sh.dominick.inpeel.lib.data.oauth2.sql.OAuth2ConnectionsTable
import sh.dominick.inpeel.lib.data.passwords.sql.AccountPasswordsTable
import sh.dominick.inpeel.lib.data.sql.AccountDTO
import sh.dominick.inpeel.lib.data.sql.AccountsTable
import sh.dominick.inpeel.rest.routes.AccountRoutes

class CreateAccountTests {
    companion object {
        private val ctx = mockk<Context>(relaxed = true)

        @BeforeClass
        @JvmStatic
        fun setup() {
            TestingDatabase.connect(
                AccountsTable,
                AccountPasswordsTable,
                OAuth2ConnectionsTable
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
        val request = AccountRoutes.CreateNormalRequest(
            email = newEmail(),
            name = newName(),
            password = "1234567!"
        )

        AccountRoutes.create(ctx, request)

        verify {
            ctx.json(withArg {
                assertAndReadAccount(it)
            })
        }
    }

    @Test
    fun `Create a Normal Account With Uppercase Email Does Lowercase`() {
        val request = AccountRoutes.CreateNormalRequest(
            email = newEmail().uppercase(),
            name = newName(),
            password = "1234567!"
        )

        AccountRoutes.create(ctx, request)

        verify { ctx.json(withArg {
            val dto = assertAndReadAccount(it)

            assertTrue(dto.email == request.email.lowercase())
        }) }
    }

    @Test(expected = ConflictResponse::class)
    fun `Create a Normal Account With Conflicting Email Throws ConflictResponse`() {
        val email = newEmail()

        AccountRoutes.create(
            ctx, AccountRoutes.CreateNormalRequest(
            email = email,
            name = newName(),
            password = "1234567!"
        ))

        verify { ctx.json(withArg {
            assertAndReadAccount(it)
        }) }

        AccountRoutes.create(
            ctx, AccountRoutes.CreateNormalRequest(
            email = email,
            name = newName(),
            password = "1234567!"
        ))
    }
}