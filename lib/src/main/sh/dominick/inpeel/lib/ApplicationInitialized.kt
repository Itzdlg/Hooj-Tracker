package sh.dominick.inpeel.lib

import com.google.gson.GsonBuilder
import io.javalin.community.routing.annotations.AnnotatedRoutingPlugin
import io.javalin.config.JavalinConfig

interface ApplicationInitialized {
    fun load(routingPlugin: AnnotatedRoutingPlugin, gsonBuilder: GsonBuilder, javalinConfig: JavalinConfig)
}