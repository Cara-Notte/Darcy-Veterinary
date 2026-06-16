package darcy.veterinary.domain.model

enum class ClinicService(
    val displayName: String,
    val defaultCost: Double
) {
    CONSULTATION("Consultation", 100_000.0),
    GROOMING("Grooming", 150_000.0),
    VACCINATION("Vaccination", 250_000.0),
    BASIC_TREATMENT("Basic Treatment", 75_000.0),
    EXTENDED_CARE("Extended Care", 500_000.0);

    companion object {
        fun fromCode(code: String): ClinicService? = values().firstOrNull {
            it.name.equals(code.trim(), ignoreCase = true)
        }
    }
}
