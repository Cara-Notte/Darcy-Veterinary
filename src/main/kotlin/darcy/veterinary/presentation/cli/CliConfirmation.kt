package darcy.veterinary.presentation.cli

object CliConfirmation {
    fun parse(value: String): Boolean? = when (value.trim().lowercase()) {
        "y", "yes" -> true
        "n", "no" -> false
        else -> null
    }
}
