package sh.dominick.inpeel.lib.data.sql

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.reflect.KProperty

object ConfigurationTable : IntIdTable("configuration") {
    val key = text("key", eagerLoading = true)
    val value = text("value", eagerLoading = true)

    fun query(key: String)
        = ConfigurationTable.key.lowerCase() eq key.lowercase()
}

object Configuration {
    // Needs to be specified in here to ensure defaults are set at load.

    val ARGON2_MEMORY by IntEntry("argon2.memory", 65535)
    val ARGON2_PARALLELISM by IntEntry("argon2.parallelism", 1)

    val PASSWORD_SALT_LENGTH by IntEntry("passwords.salt_length", 32)
    val PASSWORD_HASH_ITERATIONS by IntEntry("passwords.hash_iterations", 6)

    operator fun get(key: String): String? {
        return transaction {
            ConfigurationTable.select {
                ConfigurationTable.key.lowerCase() eq key.lowercase()
            }.firstOrNull()?.get(ConfigurationTable.value)
        }
    }

    fun get(key: String, default: String): String
        = get(key) ?: default

    operator fun set(key: String, value: String?) {
        if (value == null)
            return delete(key)

        if (get(key) != null)
            delete(key)

        transaction {
            ConfigurationTable.insert {
                it[ConfigurationTable.key] = key.lowercase()
                it[ConfigurationTable.value] = value
            }
        }
    }

    fun delete(key: String) {
        transaction {
            ConfigurationTable.deleteWhere {
                query(key)
            }
        }
    }

    fun exists(key: String)
        = get(key) != null

    fun define(vararg pairs: Pair<String, String>, overwrite: Boolean = false) {
        for (pair in pairs) {
            if (!exists(pair.first) || overwrite)
                set(pair.first, pair.second)
        }
    }

    fun entries(): Map<String, String> {
        return transaction {
            ConfigurationTable.selectAll()
                .associateBy { it[ConfigurationTable.key] }
                .mapValues { it.value[ConfigurationTable.value] }
        }
    }

    open class Entry<T>(
        private val key: String? = null,
        private val default: T? = null,
        private val toValue: (T) -> String = { it.toString() },
        private val toReal: (String?) -> T,
    ) {
        init {
            if (key != null && default != null) {
                define(key to toValue(default))
            }
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return toReal(Configuration[key ?: property.name])
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            Configuration[key ?: property.name] = toValue(value)
        }
    }

    class StringEntry(
        key: String,
        default: String
    ) : Entry<String>(key, default, toReal = { it ?: default })

    class IntEntry(
        key: String,
        default: Int
    ) : Entry<Int>(key, default, toReal = { it?.toIntOrNull() ?: default })

    class LongEntry(
        key: String,
        default: Long
    ) : Entry<Long>(key, default, toReal = { it?.toLongOrNull() ?: default })

    class BooleanEntry(
        key: String,
        default: Boolean
    ) : Entry<Boolean>(key, default, toReal = { it?.toBooleanStrictOrNull() ?: default })
}