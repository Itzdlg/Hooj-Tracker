package sh.dominick.hoojtracker

import io.github.cdimascio.dotenv.dotenv

object Env {
    private val dotenv = dotenv()

    val PORT = dotenv["PORT"]?.toIntOrNull() ?: 8080

    val DATABASE_DRIVER = dotenv["DATABASE_DRIVER"] ?: "com.mysql.cj.jdbc.Driver"
    val DATABASE_URL = dotenv["DATABASE_URL"] ?: "jdbc:mysql://localhost:3306/hooj_tracker"
    val DATABASE_USERNAME = dotenv["DATABASE_USERNAME"] ?: "root"
    val DATABASE_PASSWORD = dotenv["DATABASE_PASSWORD"] ?: "passwd"
    val DATABASE_POOL_SIZE = dotenv["DATABASE_POOL_SIZE"]?.toIntOrNull() ?: 10

    val OAUTH2_REDIRECT_URI = dotenv["OAUTH2_REDIRECT_URI"] ?: "http://localhost:5173/signup/{provider}"
}