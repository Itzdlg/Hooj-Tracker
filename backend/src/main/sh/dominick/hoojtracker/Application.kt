package sh.dominick.hoojtracker

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
import sh.dominick.hoojtracker.data.config.Configuration
import sh.dominick.hoojtracker.data.config.ConfigurationTable
import sh.dominick.hoojtracker.modules.accounts.AccountsModule
import sh.dominick.hoojtracker.modules.sessions.SessionsModule
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
        }

        setOf(
            AccountsModule,
            SessionsModule
        ).forEach { module ->
            module.load(routingPlugin, gsonBuilder, config)
        }

        gson = gsonBuilder.create()
        prettyGson = gsonBuilder
            .setPrettyPrinting()
            .create()

        config.plugins.register(routingPlugin)
    }.start(Env.PORT)
}