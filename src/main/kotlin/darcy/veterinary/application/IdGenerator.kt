package darcy.veterinary.application

import java.util.UUID

interface IdGenerator {
    fun nextId(prefix: String): String
}

object UuidIdGenerator : IdGenerator {
    override fun nextId(prefix: String): String = "$prefix-${UUID.randomUUID().toString().take(8).uppercase()}"
}

class SequenceIdGenerator : IdGenerator {
    private val counters = mutableMapOf<String, Int>()

    override fun nextId(prefix: String): String {
        val next = (counters[prefix] ?: 0) + 1
        counters[prefix] = next
        return "$prefix-${next.toString().padStart(4, '0')}"
    }
}
