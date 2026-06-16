package darcy.veterinary.infrastructure.memory

import darcy.veterinary.domain.model.Owner
import darcy.veterinary.repository.OwnerRepository

class InMemoryOwnerRepository : OwnerRepository {
    private val owners = linkedMapOf<String, Owner>()

    override fun save(owner: Owner): Owner {
        owners[owner.id] = owner
        return owner
    }

    override fun findById(id: String): Owner? = owners[id]

    override fun findAll(): List<Owner> = owners.values.toList()

    override fun search(keyword: String): List<Owner> {
        val query = keyword.trim()
        if (query.isBlank()) return emptyList()

        return owners.values.filter { owner ->
            owner.fullName.contains(query, ignoreCase = true) ||
                owner.phoneNumber.contains(query) ||
                owner.email?.contains(query, ignoreCase = true) == true
        }
    }
}
