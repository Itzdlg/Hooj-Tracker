package sh.dominick.hoojtracker.data.oauth2.providers

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow
import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.http.BasicAuthentication
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.DataStore
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import sh.dominick.hoojtracker.data.accounts.Account
import sh.dominick.hoojtracker.data.config.Configuration
import sh.dominick.hoojtracker.data.oauth2.ExposedCredentialDataStore
import sh.dominick.hoojtracker.data.oauth2.OAuth2Connection
import sh.dominick.hoojtracker.data.oauth2.OAuth2ConnectionsTable
import sh.dominick.hoojtracker.data.oauth2.connections
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

object DiscordOAuth2Provider : OAuth2Provider(0, "discord") {
    override val clientId
        get() = Configuration.OAUTH2_DISCORD_CLIENT_ID

    override val clientSecret
        get() = Configuration.OAUTH2_DISCORD_CLIENT_SECRET

    override val acceptingRegistrations
        get() = Configuration.OAUTH2_DISCORD_REGISTRATIONS && clientSecret.isNotBlank()

    override val acceptingLogins: Boolean
        get() = Configuration.OAUTH2_DISCORD_LOGIN

    override val flow: AuthorizationCodeFlow by lazy {
        AuthorizationCodeFlow.Builder(
            BearerToken.authorizationHeaderAccessMethod(),
            NetHttpTransport(),
            GsonFactory(),
            GenericUrl("https://discord.com/api/oauth2/token"),
            BasicAuthentication(clientId, clientSecret),
            clientId,
            "https://discord.com/api/oauth2/authorize"
        ).setCredentialDataStore(
            dataStore
        )
            .build()
    }

    override val dataStore: DataStore<StoredCredential> by lazy {
        ExposedCredentialDataStore(this)
    }

    override fun connect(code: String, applyName: Account.(String) -> (Unit)): Pair<Account, OAuth2Connection> {
        return transaction {
            val response = flow
                .newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute()

            val account = Account.new {
                this.name = "OAuth2 Flow $code"

                this.createdAt = Instant.now()
                this.updated()
            }

            val accountId = account.id.value
            val credential = flow.createAndStoreCredential(response, accountId.toString())

            val connection = account.connections[this@DiscordOAuth2Provider]
                ?: throw IllegalStateException()

            val client = HttpClient.newHttpClient()

            val meRequest = HttpRequest.newBuilder(URI.create("https://discord.com/api/oauth2/@me"))
                .header("Authorization", "Bearer " + credential.accessToken)
                .GET()
                .build()

            val meResponse = client.send(meRequest, HttpResponse.BodyHandlers.ofString())

            val json = JsonParser.parseString(meResponse.body()).asJsonObject
            val userObj = json["user"]?.asJsonObject
                ?: throw IllegalStateException()

            val name = userObj.get("global_name")?.asString
                ?: throw IllegalStateException()

            val id = userObj.get("id")?.asString
                ?: throw IllegalStateException()

            val exists = OAuth2ConnectionsTable.select {
                OAuth2ConnectionsTable.providerAccountId eq id
            }.count() > 0

            if (exists)
                throw ExistingConnectionException()

            applyName(account, name)
            connection.providerAccountId = id

            return@transaction account to connection
        }
    }
}