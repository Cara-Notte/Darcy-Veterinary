package darcy.veterinary.ui

import darcy.veterinary.domain.ClinicService
import darcy.veterinary.domain.Pet
import darcy.veterinary.repository.PetRepository
import darcy.veterinary.storage.FileStorage
import java.util.InputMismatchException

object ConsoleUI {
    private val repository = PetRepository()
    private var running = true

    init {
        FileStorage.loadData(repository)
            .onSuccess { loadedCount ->
                if (loadedCount > 0) println("[INFO] Loaded $loadedCount patient record(s).")
            }
            .onFailure { println("[ERROR] Failed to load existing data: ${it.message}") }
    }

    fun start() {
        printHeader()

        while (running) {
            showMainMenu()
        }
    }

    private fun printHeader() {
        println("=".repeat(50))
        println("DARCY VETERINARY PATIENT MANAGEMENT")
        println("=".repeat(50))
    }

    private fun showMainMenu() {
        println("\nMain Menu:")
        println("1. Register new patient")
        println("2. View all patients")
        println("3. Search patient and add service")
        println("4. Search patients by name or species")
        println("5. Save data and exit")

        when (readInt("Choose menu (1-5): ", 1..5)) {
            1 -> registerPatient()
            2 -> viewAllPatients()
            3 -> applyService()
            4 -> searchPatients()
            5 -> exit()
        }
    }

    private fun registerPatient() {
        println("\n-- Patient Registration --")
        val id = readRequiredString("Patient ID: ")

        val pet = Pet(
            id = id,
            name = readRequiredString("Pet name: "),
            species = readRequiredString("Species: ")
        )

        if (repository.add(pet)) {
            println("[INFO] Patient ${pet.name} ($id) has been registered.")
        } else {
            println("[ERROR] Patient ID already exists.")
        }
    }

    private fun viewAllPatients() {
        println("\n-- All Patients --")
        val patients = repository.getAll()

        if (patients.isEmpty()) {
            println("[INFO] No patients registered yet.")
            return
        }

        patients.forEachIndexed { index, pet ->
            println("${index + 1}. ${pet.displayInfo()}")
            println("   Service history: ${pet.serviceSummary()}")
        }
        println("[INFO] Total registered patients: ${patients.size}")
    }

    private fun applyService() {
        println("\n-- Clinic Service --")
        val id = readRequiredString("Patient ID: ")
        val pet = repository.findById(id)

        if (pet == null) {
            println("[INFO] Patient with ID $id was not found.")
            return
        }

        println("[INFO] Patient found: ${pet.displayInfo()}")
        val service = chooseService()

        repository.addService(pet.id, service)
        println("[INFO] ${service.displayName} has been added for ${pet.name}.")
        println("Service cost: Rp ${service.cost.toInt()}")
        println("Updated total cost: Rp ${pet.totalCost().toInt()}")
    }

    private fun searchPatients() {
        println("\n-- Patient Search --")
        val keyword = readRequiredString("Search keyword: ")
        val results = repository.searchByNameOrSpecies(keyword)

        if (results.isEmpty()) {
            println("[INFO] No patients matched '$keyword'.")
            return
        }

        results.forEachIndexed { index, pet ->
            println("${index + 1}. ${pet.displayInfo()}")
        }
        println("[INFO] Found ${results.size} matching patient(s).")
    }

    private fun chooseService(): ClinicService {
        println("Choose service:")
        ClinicService.availableServices.forEachIndexed { index, service ->
            println("${index + 1}. ${service.displayName} (Rp ${service.cost.toInt()})")
        }

        val selectedIndex = readInt("Your choice: ", 1..ClinicService.availableServices.size) - 1
        return ClinicService.availableServices[selectedIndex]
    }

    private fun exit() {
        running = false
        FileStorage.saveData(repository.getAll())
            .onSuccess { println("[INFO] Data saved. Thank you for using Darcy Veterinary.") }
            .onFailure { println("[ERROR] Failed to save data: ${it.message}") }
    }

    private fun readRequiredString(prompt: String): String {
        while (true) {
            print(prompt)
            val value = readln().trim()
            if (value.isNotBlank()) return value
            println("[ERROR] Input cannot be empty.")
        }
    }

    private fun readInt(prompt: String, range: IntRange): Int {
        while (true) {
            try {
                return readRequiredString(prompt).toInt().takeIf { it in range }
                    ?: throw InputMismatchException()
            } catch (_: Exception) {
                println("[ERROR] Invalid input. Enter a number from ${range.first} to ${range.last}.")
            }
        }
    }
}
