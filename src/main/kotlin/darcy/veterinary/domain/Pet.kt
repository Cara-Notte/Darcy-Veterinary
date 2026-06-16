package darcy.veterinary.domain

data class Pet(
    val id: String,
    val name: String,
    val species: String,
    val services: MutableList<ClinicService> = mutableListOf()
) {
    fun totalCost(): Double = services.sumOf { it.cost }

    fun serviceSummary(): String = if (services.isEmpty()) {
        "No services recorded"
    } else {
        services.joinToString(", ") { it.displayName }
    }

    fun displayInfo(): String {
        val serviceCount = services.size
        val totalCost = totalCost().toInt()
        return "$id - $name ($species) | Services: $serviceCount | Total: Rp $totalCost"
    }
}
