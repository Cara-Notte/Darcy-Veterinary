package darcy.veterinary.repository

import darcy.veterinary.domain.ClinicService
import darcy.veterinary.domain.Pet

interface Repository<T> {
    fun add(item: T): Boolean
    fun getAll(): List<T>
    fun findById(id: String): T?
}

class PetRepository : Repository<Pet> {
    private val pets = mutableListOf<Pet>()

    override fun add(item: Pet): Boolean {
        if (findById(item.id) != null) return false
        return pets.add(item)
    }

    override fun getAll(): List<Pet> = pets.toList()

    override fun findById(id: String): Pet? = pets.firstOrNull {
        it.id.equals(id.trim(), ignoreCase = true)
    }

    fun addService(petId: String, service: ClinicService): Boolean {
        return findById(petId)?.services?.add(service) ?: false
    }

    fun searchByNameOrSpecies(keyword: String): List<Pet> {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) return emptyList()

        return pets.filter { pet ->
            pet.name.contains(normalizedKeyword, ignoreCase = true) ||
                pet.species.contains(normalizedKeyword, ignoreCase = true)
        }
    }
}
