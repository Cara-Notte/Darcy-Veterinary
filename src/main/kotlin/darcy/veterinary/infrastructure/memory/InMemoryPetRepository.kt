package darcy.veterinary.infrastructure.memory

import darcy.veterinary.domain.model.Pet
import darcy.veterinary.repository.PetRepository

class InMemoryPetRepository : PetRepository {
    private val pets = linkedMapOf<String, Pet>()

    override fun save(pet: Pet): Pet {
        pets[pet.id] = pet
        return pet
    }

    override fun findById(id: String): Pet? = pets[id]

    override fun findAll(): List<Pet> = pets.values.toList()

    override fun findByOwnerId(ownerId: String): List<Pet> = pets.values.filter { it.ownerId == ownerId }

    override fun search(keyword: String): List<Pet> {
        val query = keyword.trim()
        if (query.isBlank()) return emptyList()

        return pets.values.filter { pet ->
            pet.name.contains(query, ignoreCase = true) ||
                pet.species.contains(query, ignoreCase = true) ||
                pet.breed?.contains(query, ignoreCase = true) == true
        }
    }
}
