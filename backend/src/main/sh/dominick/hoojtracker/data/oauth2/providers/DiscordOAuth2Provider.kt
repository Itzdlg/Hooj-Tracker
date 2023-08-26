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
import sh.dominick.hoojtracker.Env
import sh.dominick.hoojtracker.data.accounts.Account
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
    override val flow: AuthorizationCodeFlow by lazy {
        AuthorizationCodeFlow.Builder(
            BearerToken.authorizationHeaderAccessMethod(),
            NetHttpTransport(),
            GsonFactory(),
            GenericUrl("https://discord.com/api/oauth2/token"),
            BasicAuthentication(Env.OAUTH2_DISCORD_CLIENT_ID, Env.OAUTH2_DISCORD_CLIENT_SECRET),
            Env.OAUTH2_DISCORD_CLIENT_ID,
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
        val response = flow
            .newTokenRequest(code)
            .setRedirectUri("http://localhost:5173/signup/discord")
            .execute()

        val account = transaction {
            Account.new {
                this.name = "OAuth2 Flow $code"

                this.createdAt = Instant.now()
                this.updated()
            }
        }

        val accountId = account.id.value

        val credential = flow.createAndStoreCredential(response, accountId.toString())

        val connection = transaction {
            val it = account.connections[this@DiscordOAuth2Provider]
            if (it == null) {
                account.delete()
                throw IllegalStateException()
            }

            return@transaction it
        }

        val client = HttpClient.newHttpClient()

        val meRequest = HttpRequest.newBuilder(URI.create("https://discord.com/api/oauth2/@me"))
            .header("Authorization", "Bearer " + credential.accessToken)
            .GET()
            .build()

        val meResponse = client.send(meRequest, HttpResponse.BodyHandlers.ofString())

        val json = JsonParser.parseString(meResponse.body()).asJsonObject

        if (!json.has("user")) transaction {
            account.delete()
            throw InvalidProviderResponseException()
        }

        val userObj: JsonObject? = json["user"]?.asJsonObject

        val name = userObj?.get("global_name")?.asString
        val id = userObj?.get("id")?.asString

        if (name == null || id == null) transaction {
            connection.delete()
            account.delete()

            throw InvalidProviderResponseException()
        }

        val idExists = transaction {
            OAuth2ConnectionsTable.select {
                OAuth2ConnectionsTable.providerAccountId eq id
            }.count() > 0
        }

        if (idExists) transaction {
            connection.delete()
            account.delete()

            throw ExistingConnectionException()
        }

        return transaction {
            applyName(account, name)

            connection.providerAccountId = id
            account to connection
        }
    }
}