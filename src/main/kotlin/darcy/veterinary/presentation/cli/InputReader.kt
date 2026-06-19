package darcy.veterinary.presentation.cli

import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class EndOfInputException : RuntimeException("No console input is available.")

class InputReader {
    fun text(prompt: String): String {
        while (true) {
            print(prompt)
            val value = readInputLine().trim()
            if (value.isNotBlank()) return value
            println("Input cannot be empty.")
        }
    }

    fun optionalText(prompt: String): String? {
        print(prompt)
        return readInputLine().trim().ifBlank { null }
    }

    fun int(prompt: String, allowBlank: Boolean = false): Int? {
        while (true) {
            print(prompt)
            val value = readInputLine().trim()
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

    fun confirm(prompt: String): Boolean {
        while (true) {
            print("$prompt (y/n): ")
            val parsed = CliConfirmation.parse(readInputLine())
            if (parsed != null) return parsed
            println("Enter y or n.")
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

    private fun readInputLine(): String = readLine() ?: throw EndOfInputException()
}
