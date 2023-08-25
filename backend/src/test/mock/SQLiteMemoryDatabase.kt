package mock

import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object SQLiteMemoryDatabase {
    fun connect(vararg tables: Table) {
        val dataSource = HikariDataSource().apply {
            maximumPoolSize = 1
            jdbcUrl = "jdbc:sqlite::memory:"
            username = "root"
            password = ""
            isAutoCommit = true
        }

        val database = Database.connect(dataSource, databaseConfig = DatabaseConfig {
            keepLoadedReferencesOutOfTransaction = true
        })

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                *tables
            )
        }
    }
}