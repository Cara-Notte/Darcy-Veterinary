package darcy.veterinary.presentation.cli

import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class InputReader {
    fun text(prompt: String): String {
        while (true) {
            print(prompt)
            val value = readln().trim()
            if (value.isNotBlank()) return value
            println("Input cannot be empty.")
        }
    }

    fun optionalText(prompt: String): String? {
        print(prompt)
        return readln().trim().ifBlank { null }
    }

    fun int(prompt: String, allowBlank: Boolean = false): Int? {
        while (true) {
            print(prompt)
            val value = readln().trim()
            if (allowBlank && value.isBlank()) return null
            val number = value.toIntOrNull()
            if (number != null) return number
            println("Enter a valid number.")
        }
    }

    fun choice(prompt: String, range: IntRange): Int {
        while (true) {
            val value = int(prompt) ?: continue
            if (value in range) return value
            println("Enter a number from ${range.first} to ${range.last}.")
        }
    }

    fun dateTime(prompt: String): LocalDateTime {
        while (true) {
            val value = text(prompt)
            try {
                return LocalDateTime.parse(value)
            } catch (_: DateTimeParseException) {
                println("Use ISO date-time format, for example 2026-06-16T14:30.")
            }
        }
    }
}
