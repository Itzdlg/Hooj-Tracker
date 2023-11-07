package data.config

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import mock.TestingDatabase
import org.junit.BeforeClass
import org.junit.Test
import sh.dominick.inpeel.data.config.Configuration
import sh.dominick.inpeel.data.config.ConfigurationTable

class ConfigurationTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            TestingDatabase.connect(
                ConfigurationTable
            )
        }
    }

    @Test
    fun `Defined Configuration Does Default`() {
        Configuration.define("test" to "Hello")

        assertTrue(Configuration["test"] == "Hello")
    }

    @Test
    fun `Undefined Configuration Returns Null`() {
        assertTrue(Configuration["undefined"] == null)
    }

    @Test
    fun `Undefined Configuration Returns False For exists()`() {
        assertFalse(Configuration.exists("undefined"))
    }

    @Test
    fun `Set Configuration Does Set`() {
        Configuration["test"] = "5"
        assertTrue(Configuration["test"] == "5")
    }

    @Test
    fun `Deleted Configuration Does Delete`() {
        Configuration["test"] = "Hello"
        Configuration.delete("test")

        assertTrue(Configuration["test"] == null)
    }
}