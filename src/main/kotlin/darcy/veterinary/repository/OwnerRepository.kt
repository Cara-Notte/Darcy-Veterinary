package darcy.veterinary.repository

import darcy.veterinary.domain.model.Owner

interface OwnerRepository {
    fun save(owner: Owner): Owner
    fun findById(id: String): Owner?
    fun findAll(): List<Owner>
    fun search(keyword: String): List<Owner>
}
