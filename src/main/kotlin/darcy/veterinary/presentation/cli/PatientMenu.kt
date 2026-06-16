package darcy.veterinary.presentation.cli

import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.domain.model.Owner
import darcy.veterinary.domain.model.Pet

class PatientMenu(
    private val ownerService: OwnerService,
    private val patientService: PatientService,
    private val input: InputReader
) {
    private val selector = CliListSelector(input)

    fun show() {
        println("\nPatient Management")
        println("1. Register owner")
        println("2. Register pet")
        println("3. Edit owner")
        println("4. Edit pet")
        println("5. List owners")
        println("6. List pets")
        println("7. Search pets")
        when (input.choice("Choose menu: ", 1..7)) {
            1 -> registerOwner()
            2 -> registerPet()
            3 -> editOwner()
            4 -> editPet()
            5 -> listOwners()
            6 -> listPets()
            7 -> searchPets()
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
        val owner = selector.choose(
            title = "Available owners",
            items = ownerService.listOwners(),
            emptyMessage = "No owners registered yet. Register an owner before registering a pet.",
            prompt = "Select owner: ",
            formatter = { it.summary() }
        ) ?: return

        val pet = patientService.registerPet(
            ownerId = owner.id,
            name = input.text("Pet name: "),
            species = input.text("Species: "),
            breed = input.optionalText("Breed (optional): "),
            age = input.int("Age (optional): ", allowBlank = true)
        )
        println("Pet registered: ${pet.id} - ${pet.name}")
    }

    private fun editOwner() {
        val owner = selector.choose(
            title = "Owners",
            items = ownerService.listOwners(),
            emptyMessage = "No owners registered yet.",
            prompt = "Select owner to edit: ",
            formatter = { it.summary() }
        ) ?: return

        val updated = ownerService.updateOwner(
            id = owner.id,
            fullName = input.optionalText("Owner full name [${owner.fullName}]: ") ?: owner.fullName,
            phoneNumber = input.optionalText("Phone number [${owner.phoneNumber}]: ") ?: owner.phoneNumber,
            email = input.optionalText("Email [${owner.email.orEmpty()}]: ") ?: owner.email
        )
        println("Owner updated: ${updated.id} - ${updated.fullName}")
    }

    private fun editPet() {
        val pet = selector.choose(
            title = "Pets",
            items = patientService.listPets(),
            emptyMessage = "No pets registered yet.",
            prompt = "Select pet to edit: ",
            formatter = { it.summary() }
        ) ?: return

        val updated = patientService.updatePet(
            id = pet.id,
            name = input.optionalText("Pet name [${pet.name}]: ") ?: pet.name,
            species = input.optionalText("Species [${pet.species}]: ") ?: pet.species,
            breed = input.optionalText("Breed [${pet.breed.orEmpty()}]: ") ?: pet.breed,
            age = input.int("Age [${pet.age ?: "blank"}]: ", allowBlank = true) ?: pet.age
        )
        println("Pet updated: ${updated.id} - ${updated.name}")
    }

    private fun listOwners() {
        selector.show(
            title = "Owners",
            items = ownerService.listOwners(),
            emptyMessage = "No owners registered yet.",
            formatter = { it.summary() }
        )
    }

    private fun listPets() {
        selector.show(
            title = "Pets",
            items = patientService.listPets(),
            emptyMessage = "No pets registered yet.",
            formatter = { it.summary() }
        )
    }

    private fun searchPets() {
        val keyword = input.text("Keyword: ")
        selector.show(
            title = "Search results",
            items = patientService.searchPets(keyword),
            emptyMessage = "No pets matched '$keyword'.",
            formatter = { it.summary() }
        )
    }

    private fun Owner.summary(): String = "$id | $fullName | $phoneNumber | ${email.orEmpty()}"

    private fun Pet.summary(): String = "$id | $name | $species | Owner: $ownerId"
}
