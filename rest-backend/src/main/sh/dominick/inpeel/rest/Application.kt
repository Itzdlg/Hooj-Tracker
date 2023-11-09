package sh.dominick.inpeel.rest

import com.google.gson.GsonBuilder
import com.google.gson.LongSerializationPolicy
import com.zaxxer.hikari.HikariDataSource
import io.javalin.Javalin
import io.javalin.community.routing.annotations.AnnotatedRoutingPlugin
import io.javalin.json.JsonMapper
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.inpeel.lib.data.oauth2.sql.OAuth2ConnectionsTable
import sh.dominick.inpeel.lib.data.passwords.sql.AccountPasswordsTable
import sh.dominick.inpeel.lib.data.sessions.sql.SessionsTable
import sh.dominick.inpeel.lib.data.sql.AccountsTable
import sh.dominick.inpeel.lib.data.sql.Configuration
import sh.dominick.inpeel.lib.data.sql.ConfigurationTable
import sh.dominick.inpeel.lib.managers.OAuth2ConnectionsManager
import sh.dominick.inpeel.lib.managers.SessionManager
import java.lang.reflect.Type

var gson = GsonBuilder()
    .disableHtmlEscaping()
    .setLongSerializationPolicy(LongSerializationPolicy.STRING)
    .create()
private set

var prettyGson = GsonBuilder().create()
private set

fun main() {
    val dataSource = HikariDataSource().apply {
        maximumPoolSize = Env.DATABASE_POOL_SIZE
        driverClassName = Env.DATABASE_DRIVER
        jdbcUrl = Env.DATABASE_URL
        username = Env.DATABASE_USERNAME
        password = Env.DATABASE_PASSWORD
        isAutoCommit = false
    }

    val database = Database.connect(dataSource, databaseConfig = DatabaseConfig {
        keepLoadedReferencesOutOfTransaction = true
    })

    val app = Javalin.create { config ->
        config.jsonMapper(object : JsonMapper {
            override fun <T : Any> fromJsonString(json: String, targetType: Type): T =
                prettyGson.fromJson(json, targetType)

            override fun toJsonString(obj: Any, type: Type) =
                prettyGson.toJson(obj)
        })

        config.plugins.enableCors { cors ->
            cors.add {
                it.anyHost()
            }
        }

        val routingPlugin = AnnotatedRoutingPlugin()
        val gsonBuilder = gson.newBuilder()

        transaction {
            SchemaUtils.createMissingTablesAndColumns(ConfigurationTable)
            Configuration

            OAuth2ConnectionsManager.load(routingPlugin, gsonBuilder, config)
            SessionManager

            setOf(
                AccountsTable,
                AccountPasswordsTable,
                OAuth2ConnectionsTable,
                SessionsTable,
            ).forEach(SchemaUtils::createMissingTablesAndColumns)
        }

        gson = gsonBuilder.create()
        prettyGson = gsonBuilder
            .setPrettyPrinting()
            .create()

        config.plugins.register(routingPlugin)
    }.start(Env.PORT)
}