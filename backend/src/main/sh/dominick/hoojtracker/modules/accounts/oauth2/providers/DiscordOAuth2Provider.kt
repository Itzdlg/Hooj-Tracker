package sh.dominick.hoojtracker.modules.accounts.oauth2.providers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

abstract class DiscordOAuth2Provider : OAuth2Provider(0, "discord") {
    override val tokenUri: String
        get() = "https://discord.com/api/oauth2/token"

    override val authorizationUri: String
        get() = "https://discord.com/api/oauth2/authorize"

    override fun requestProviderAccount(accessToken: String): JsonObject {
        val client = HttpClient.newHttpClient()

        val meRequest = HttpRequest.newBuilder(URI.create("https://discord.com/api/oauth2/@me"))
            .header("Authorization", "Bearer $accessToken")
            .GET()
            .build()

        val meResponse = client.send(meRequest, HttpResponse.BodyHandlers.ofString())

        val json = JsonParser.parseString(meResponse.body()).asJsonObject

        return json["user"]?.asJsonObject
            ?: throw IllegalStateException()
    }

    override fun requestProviderAccountId(accessToken: String): String {
        val userObj = requestProviderAccount(accessToken)

        return userObj.get("id")?.asString
            ?: throw IllegalStateException()
    }

    override fun requestProviderAccountName(accessToken: String): String {
        val userObj = requestProviderAccount(accessToken)

        return userObj.get("global_name")?.asString
            ?: throw IllegalStateException()
    }
}