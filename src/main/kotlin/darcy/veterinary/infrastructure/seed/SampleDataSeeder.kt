package darcy.veterinary.infrastructure.seed

import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService

class SampleDataSeeder(
    private val ownerService: OwnerService,
    private val patientService: PatientService
) {
    fun seedIfEmpty() {
        if (ownerService.listOwners().isNotEmpty()) return

        val owner = ownerService.registerOwner(
            fullName = "Maya Hartono",
            phoneNumber = "081234567890",
            email = "maya@example.com"
        )
        patientService.registerPet(
            ownerId = owner.id,
            name = "Darcy",
            species = "Dog",
            breed = "Golden Retriever",
            age = 3
        )
    }
}
