package sh.dominick.hoojtracker.util

import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import sh.dominick.hoojtracker.Env

fun argon2(
    text: String,
    iterations: Int,

    memory: Int = Env.ARGON2_MEMORY_ALLOCATION,
    parallelism: Int = Env.ARGON2_PARALLELISM
): String {
    val chars = text.toCharArray()
    val argon2: Argon2 = Argon2Factory.create()

    try {
        return argon2.hash(iterations, memory, parallelism, chars)
    } finally {
        argon2.wipeArray(chars)
    }
}