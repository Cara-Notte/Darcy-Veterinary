package darcy.veterinary.application

import darcy.veterinary.domain.exception.EntityNotFoundException
import darcy.veterinary.domain.model.Pet
import darcy.veterinary.domain.model.PetSex
import darcy.veterinary.repository.OwnerRepository
import darcy.veterinary.repository.PetRepository
import java.time.LocalDate

class PatientService(
    private val petRepository: PetRepository,
    private val ownerRepository: OwnerRepository,
    private val idGenerator: IdGenerator = UuidIdGenerator
) {
    fun registerPet(
        ownerId: String,
        name: String,
        species: String,
        breed: String? = null,
        age: Int? = null,
        sex: PetSex? = null,
        dateOfBirth: LocalDate? = null,
        weightKg: Double? = null,
        allergies: List<String> = emptyList(),
        medicalConditions: List<String> = emptyList()
    ): Pet {
        validatePetProfile(name, species, age, weightKg)
        ownerRepository.findById(ownerId)
            ?: throw EntityNotFoundException("Owner with ID $ownerId was not found.")

        return petRepository.save(
            Pet(
                id = idGenerator.nextId("PET"),
                ownerId = ownerId,
                name = name.trim(),
                species = species.trim(),
                breed = breed?.trim()?.ifBlank { null },
                age = age,
                sex = sex,
                dateOfBirth = dateOfBirth,
                weightKg = weightKg,
                allergies = cleanList(allergies),
                medicalConditions = cleanList(medicalConditions)
            )
        )
    }

    fun updatePet(
        id: String,
        name: String,
        species: String,
        breed: String? = null,
        age: Int? = null,
        sex: PetSex? = null,
        dateOfBirth: LocalDate? = null,
        weightKg: Double? = null,
        allergies: List<String> = emptyList(),
        medicalConditions: List<String> = emptyList()
    ): Pet {
        validatePetProfile(name, species, age, weightKg)
        val existing = getPet(id)

        return petRepository.save(
            existing.copy(
                name = name.trim(),
                species = species.trim(),
                breed = breed?.trim()?.ifBlank { null },
                age = age,
                sex = sex,
                dateOfBirth = dateOfBirth,
                weightKg = weightKg,
                allergies = cleanList(allergies),
                medicalConditions = cleanList(medicalConditions)
            )
        )
    }

    fun getPet(id: String): Pet = petRepository.findById(id)
        ?: throw EntityNotFoundException("Pet with ID $id was not found.")

    fun listPets(): List<Pet> = petRepository.findAll()

    fun listPetsByOwner(ownerId: String): List<Pet> = petRepository.findByOwnerId(ownerId)

    fun searchPets(keyword: String): List<Pet> = petRepository.search(keyword)

    private fun validatePetProfile(name: String, species: String, age: Int?, weightKg: Double?) {
        require(name.isNotBlank()) { "Pet name cannot be blank." }
        require(species.isNotBlank()) { "Species cannot be blank." }
        require(age == null || age >= 0) { "Age cannot be negative." }
        require(weightKg == null || weightKg > 0.0) { "Weight must be greater than zero." }
    }

    private fun cleanList(values: List<String>): List<String> = values.map(String::trim).filter(String::isNotBlank)
}
