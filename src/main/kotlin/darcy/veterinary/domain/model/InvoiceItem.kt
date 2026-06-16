package darcy.veterinary.domain.model

data class InvoiceItem(
    val service: ClinicService,
    val description: String = service.displayName,
    val cost: Double = service.defaultCost
)
