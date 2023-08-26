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
import sh.dominick.hoojtracker.data.accounts.AccountCredentialsTable
import sh.dominick.hoojtracker.data.accounts.AccountsTable
import sh.dominick.hoojtracker.data.oauth2.OAuth2ConnectionsTable
import sh.dominick.hoojtracker.data.oauth2.providers.DiscordOAuth2Provider
import sh.dominick.hoojtracker.data.oauth2.providers.OAuth2Provider
import sh.dominick.hoojtracker.routes.AccountsController
import java.lang.reflect.Type

var prettyGson = GsonBuilder()
    .setPrettyPrinting()
    .disableHtmlEscaping()
    .setLongSerializationPolicy(LongSerializationPolicy.STRING)
    .create()

fun main() {
    val dataSource = HikariDataSource().apply {
        maximumPoolSize = Env.DATABASE_POOL_SIZE
        driverClassName = Env.DATABASE_DRIVER
        jdbcUrl = Env.DATABASE_URL
        username = Env.DATABASE_USERNAME
        password = Env.DATABASE_PASSWORD
        isAutoCommit = true
    }

    val database = Database.connect(dataSource, databaseConfig = DatabaseConfig {
        keepLoadedReferencesOutOfTransaction = true
    })

    transaction {
        SchemaUtils.createMissingTablesAndColumns(
            AccountsTable,
            AccountCredentialsTable,
            OAuth2ConnectionsTable
        )
    }

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
        routingPlugin.registerEndpoints(
            AccountsController
        )

        config.plugins.register(routingPlugin)
    }.start(Env.PORT)

    setOf(DiscordOAuth2Provider).forEach {
        OAuth2Provider.register(it)
    }
}