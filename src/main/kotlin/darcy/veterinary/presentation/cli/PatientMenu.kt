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
        val owner = selector.choose(
            title = "Available owners",
            items = ownerService.listOwners(),
            emptyMessage = "No owners registered yet. Register an owner before registering a pet.",
            prompt = "Select owner: ",
            formatter = Owner::summary
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

    private fun listOwners() {
        selector.show(
            title = "Owners",
            items = ownerService.listOwners(),
            emptyMessage = "No owners registered yet.",
            formatter = Owner::summary
        )
    }

    private fun listPets() {
        selector.show(
            title = "Pets",
            items = patientService.listPets(),
            emptyMessage = "No pets registered yet.",
            formatter = Pet::summary
        )
    }

    private fun searchPets() {
        val keyword = input.text("Keyword: ")
        selector.show(
            title = "Search results",
            items = patientService.searchPets(keyword),
            emptyMessage = "No pets matched '$keyword'.",
            formatter = Pet::summary
        )
    }

    private fun Owner.summary(): String = "$id | $fullName | $phoneNumber | ${email.orEmpty()}"

    private fun Pet.summary(): String = "$id | $name | $species | Owner: $ownerId"
}
