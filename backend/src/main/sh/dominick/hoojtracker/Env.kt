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

    val ARGON2_MEMORY_ALLOCATION = dotenv["ARGON2_MEMORY_ALLOCATION"]?.toIntOrNull() ?: 65535
    val ARGON2_PARALLELISM = dotenv["ARGON2_PARALLELISM"]?.toIntOrNull() ?: 1

    val PASSWORD_SALT_CHARSET = dotenv["PASSWORD_SALT_CHARSET"]
        ?: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"

    val PASSWORD_HASH_ITERATIONS = dotenv["PASSWORD_HASH_ITERATIONS"]?.toIntOrNull()
        ?: 6 /* For Argon2 this is enough */
}