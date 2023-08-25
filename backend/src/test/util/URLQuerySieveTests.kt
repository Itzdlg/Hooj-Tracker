package util

import io.javalin.http.Context
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import mock.SQLiteMemoryDatabase
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import sh.dominick.hoojtracker.util.Filter
import java.util.concurrent.ThreadLocalRandom

class URLQuerySieveTests {
    companion object {
        // This class needs to connect to a database
        // for Exposed to check #equals on queries.

        private val filters = setOf(
            Filter.Parameter("name", TestTable.name, String::class.java),
            Filter.Parameter("age", TestTable.age, Int::class.java)
        )

        private var testName = "John"
        private var testAge = 18

        @Before
        fun randomizeAge() {
            testAge = ThreadLocalRandom.current().nextInt(65)
        }

        @BeforeClass
        @JvmStatic
        fun setup() {
            SQLiteMemoryDatabase.connect(TestTable)
        }

        private fun assertQueryEquals(a: Op<Boolean>, b: Op<Boolean>) {
            transaction {
                assertTrue(a == b)
            }
        }
    }

    object TestTable : Table("test") {
        val name = text("name")
        val age = integer("age")
    }

    @Test
    fun `No Filter Returns Empty Query`() {
        val ctx = mockk<Context>(relaxed = true)

        val filter = Filter.of(ctx, filters, 25)

        assertQueryEquals(filter.query, Op.TRUE)
    }

    @Test
    fun `Unspecified Method String Filter Returns LIKE Query`() {
        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("f:name") } returns testName

        val filter = Filter.of(ctx, filters, 25)

        assertQueryEquals(filter.query, Op.TRUE and (TestTable.name like "%${testName}%"))
    }

    @Test
    fun `Unspecified Method Integer Filter Returns EQ Query`() {
        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("f:age") } returns "$testAge"

        val filter = Filter.of(ctx, filters, 25)

        assertQueryEquals(filter.query, Op.TRUE and (TestTable.age eq testAge))
    }

    @Test
    fun `LT Filter Returns LTE Query`() {
        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("f:age") } returns "lt:${testAge}"

        val filter = Filter.of(ctx, filters, 25)

        assertQueryEquals(filter.query, Op.TRUE and (TestTable.age lessEq testAge))
    }

    @Test
    fun `GT Filter Returns GTE Query`() {
        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("f:age") } returns "gt:${testAge}"

        val filter = Filter.of(ctx, filters, 25)

        assertQueryEquals(filter.query, Op.TRUE and (TestTable.age greaterEq testAge))
    }

    private fun complexQueryTest(ctx: Context): Filter {
        every { ctx.queryParam("f:name") } returns testName
        every { ctx.queryParam("f:age") } returns "gt:${testAge}"

        val filter = Filter.of(ctx, filters, 25)

        return filter
    }

    @Test
    fun `No Match Specified Returns AND Queries`() {
        val ctx = mockk<Context>(relaxed = true)
        val filter = complexQueryTest(ctx)

        assertQueryEquals(filter.query, Op.TRUE and (TestTable.name like "%${testName}%") and (TestTable.age greaterEq testAge))
    }

    @Test
    fun `Match All Filter Returns AND Queries`() {
        val ctx = mockk<Context>(relaxed = true)
        val filter = complexQueryTest(ctx)

        assertQueryEquals(filter.query, Op.TRUE and (TestTable.name like "%${testName}%") and (TestTable.age greaterEq testAge))
    }

    @Test
    fun `Match Any Filter Returns OR Queries`() {
        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("match") } returns "any"
        val filter = complexQueryTest(ctx)

        assertQueryEquals(filter.query, Op.FALSE or (TestTable.name like "%${testName}%") or (TestTable.age greaterEq testAge))
    }
}