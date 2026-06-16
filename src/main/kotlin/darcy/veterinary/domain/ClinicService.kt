package darcy.veterinary.domain

sealed class ClinicService(
    val code: String,
    val displayName: String,
    val cost: Double
) {
    object Grooming : ClinicService("GROOMING", "Grooming", 150_000.0)
    object Vaccination : ClinicService("VACCINATION", "Vaccination", 250_000.0)

    companion object {
        val availableServices: List<ClinicService> = listOf(Grooming, Vaccination)

        fun fromCode(code: String): ClinicService? = availableServices.firstOrNull {
            it.code.equals(code.trim(), ignoreCase = true)
        }
    }
}
