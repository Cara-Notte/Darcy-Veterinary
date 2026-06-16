package darcy.veterinary.repository

import darcy.veterinary.domain.model.Pet

interface PetRepository {
    fun save(pet: Pet): Pet
    fun findById(id: String): Pet?
    fun findAll(): List<Pet>
    fun findByOwnerId(ownerId: String): List<Pet>
    fun search(keyword: String): List<Pet>
}
