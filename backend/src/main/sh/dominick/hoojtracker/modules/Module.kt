package sh.dominick.hoojtracker.modules

import com.google.gson.GsonBuilder
import io.javalin.community.routing.annotations.AnnotatedRoutingPlugin
import io.javalin.config.JavalinConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

open class Module(val id: String) {
    open val routes: Set<Any> = setOf()
    open val tables: Set<Table> = setOf()
    open val gson: GsonBuilder.() -> (Unit) = { }
    open val javalin: JavalinConfig.() -> (Unit) = { }

    open val submodules: Set<Module> = setOf()

    open fun load(routingPlugin: AnnotatedRoutingPlugin, gsonBuilder: GsonBuilder, javalinConfig: JavalinConfig) {
        routes.forEach {
            routingPlugin.registerEndpoints(it)
        }

        transaction {
            tables.forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }

        gsonBuilder.apply(gson)
        javalinConfig.apply(javalin)

        submodules.forEach { _ ->
            load(routingPlugin, gsonBuilder, javalinConfig)
        }
    }
}


