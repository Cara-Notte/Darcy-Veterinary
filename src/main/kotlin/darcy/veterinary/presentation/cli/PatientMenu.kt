package darcy.veterinary.presentation.cli

import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService

class PatientMenu(
    private val ownerService: OwnerService,
    private val patientService: PatientService,
    private val input: InputReader
) {
    fun show() {
        println("\nPatient Management")
        println("1. Register owner")
        println("2. Register pet")
        println("3. List owners")
        println("4. List pets")
        println("5. Search pets")
        when (input.choice("Choose menu: ", 1..5)) {
            1 -> registerOwner()
            2 -> registerPet()
            3 -> listOwners()
            4 -> listPets()
            5 -> searchPets()
        }
    }

    private fun registerOwner() {
        val owner = ownerService.registerOwner(
            fullName = input.text("Owner full name: "),
            phoneNumber = input.text("Phone number: "),
            email = input.optionalText("Email (optional): ")
        )
        println("Owner registered: ${owner.id} - ${owner.fullName}")
    }

    private fun registerPet() {
        val pet = patientService.registerPet(
            ownerId = input.text("Owner ID: "),
            name = input.text("Pet name: "),
            species = input.text("Species: "),
            breed = input.optionalText("Breed (optional): "),
            age = input.int("Age (optional): ", allowBlank = true)
        )
        println("Pet registered: ${pet.id} - ${pet.name}")
    }

    private fun listOwners() {
        ownerService.listOwners().forEach { owner ->
            println("${owner.id} | ${owner.fullName} | ${owner.phoneNumber} | ${owner.email.orEmpty()}")
        }
    }

    private fun listPets() {
        patientService.listPets().forEach { pet ->
            println("${pet.id} | ${pet.name} | ${pet.species} | Owner: ${pet.ownerId}")
        }
    }

    private fun searchPets() {
        val keyword = input.text("Keyword: ")
        patientService.searchPets(keyword).forEach { pet ->
            println("${pet.id} | ${pet.name} | ${pet.species} | Owner: ${pet.ownerId}")
        }
    }
}
