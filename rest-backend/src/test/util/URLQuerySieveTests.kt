package util

import io.javalin.http.Context
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import sh.dominick.inpeel.lib.mock.TestingDatabase
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
import sh.dominick.inpeel.rest.util.Filter
import java.util.concurrent.ThreadLocalRandom

class URLQuerySieveTests {
    companion object {
        // This class needs to connect to a database
        // for Exposed to check #equals on queries.

        private val filters = setOf(
            Filter.Parameter(TestTable.name),
            Filter.Parameter(TestTable.age)
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
            TestingDatabase.connect(TestTable)
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
    fun `Unspecified Filter Returns EQ Query`() {
        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("f:age") } returns "$testAge"

        val filter = Filter.of(ctx, filters, 25)

        assertQueryEquals(filter.query, TestTable.age eq testAge)
    }

    @Test
    fun `Like Filter Returns LIKE Query`() {
        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("f:name") } returns "like:%${testName}%"

        val filter = Filter.of(ctx, filters, 25)

        assertQueryEquals(filter.query, TestTable.name like "%${testName}%")
    }

    @Test
    fun `LTE Filter Returns LTE Query`() {
        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("f:age") } returns "lte:${testAge}"

        val filter = Filter.of(ctx, filters, 25)

        assertQueryEquals(filter.query, TestTable.age lessEq testAge)
    }

    @Test
    fun `GTE Filter Returns GTE Query`() {
        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("f:age") } returns "gte:${testAge}"

        val filter = Filter.of(ctx, filters, 25)

        assertQueryEquals(filter.query, TestTable.age greaterEq testAge)
    }

    private fun complexQueryTest(ctx: Context): Filter {
        every { ctx.queryParam("f:name") } returns "like:%${testName}%"
        every { ctx.queryParam("f:age") } returns "gte:${testAge}"

        val filter = Filter.of(ctx, filters, 25)

        return filter
    }

    @Test
    fun `No Match Specified Returns AND Queries`() {
        val ctx = mockk<Context>(relaxed = true)
        val filter = complexQueryTest(ctx)

        assertQueryEquals(filter.query, (TestTable.name like "%${testName}%") and (TestTable.age greaterEq testAge))
    }

    @Test
    fun `Match All Filter Returns AND Queries`() {
        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("match") } returns "all"
        val filter = complexQueryTest(ctx)

        assertQueryEquals(filter.query, (TestTable.name like "%${testName}%") and (TestTable.age greaterEq testAge))
    }

    @Test
    fun `Match Any Filter Returns OR Queries`() {
        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("match") } returns "any"
        val filter = complexQueryTest(ctx)

        assertQueryEquals(filter.query, (TestTable.name like "%${testName}%") or (TestTable.age greaterEq testAge))
    }
}