package sh.dominick.inpeel.lib.mock

import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.inpeel.lib.data.sql.ConfigurationTable

object TestingDatabase {
    init {
        val dataSource = HikariDataSource().apply {
            maximumPoolSize = 1
            jdbcUrl = "jdbc:h2:mem:test"
            username = "root"
            password = ""
            isAutoCommit = true
        }

        Database.connect(dataSource, databaseConfig = DatabaseConfig {
            keepLoadedReferencesOutOfTransaction = true
        })

        transaction {  }
    }

    fun connect(vararg tables: Table) {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                ConfigurationTable,
                *tables
            )
        }
    }
}