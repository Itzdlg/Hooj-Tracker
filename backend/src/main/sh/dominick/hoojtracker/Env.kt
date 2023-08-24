package sh.dominick.hoojtracker

import io.github.cdimascio.dotenv.dotenv

object Env {
    private val dotenv = dotenv()

    val PORT = dotenv["PORT"]?.toIntOrNull() ?: 8080

    val DATABASE_DRIVER = dotenv["DATABASE_DRIVER"] ?: "com.mysql.cj.jdbc.Driver"
    val DATABASE_URL = dotenv["DATABASE_URL"] ?: "jdbc:mysql://localhost:3306/preferred_private_care"
    val DATABASE_USERNAME = dotenv["DATABASE_USERNAME"] ?: "root"
    val DATABASE_PASSWORD = dotenv["DATABASE_PASSWORD"] ?: ""
    val DATABASE_POOL_SIZE = dotenv["DATABASE_POOL_SIZE"]?.toIntOrNull() ?: 10

    val ARGON2_MEMORY_ALLOCATION = dotenv["ARGON2_MEMORY_ALLOCATION"]?.toIntOrNull() ?: 65535
    val ARGON2_PARALLELISM = dotenv["ARGON2_PARALLELISM"]?.toIntOrNull() ?: 1
}