package darcy.veterinary.application

import darcy.veterinary.domain.exception.DuplicateEntityException
import darcy.veterinary.domain.exception.EntityNotFoundException
import darcy.veterinary.domain.model.Owner
import darcy.veterinary.repository.OwnerRepository

class OwnerService(
    private val ownerRepository: OwnerRepository,
    private val idGenerator: IdGenerator = UuidIdGenerator
) {
    fun registerOwner(fullName: String, phoneNumber: String, email: String? = null): Owner {
        require(fullName.isNotBlank()) { "Owner name cannot be blank." }
        require(phoneNumber.isNotBlank()) { "Phone number cannot be blank." }

        val duplicate = ownerRepository.findAll().any { it.phoneNumber == phoneNumber.trim() }
        if (duplicate) throw DuplicateEntityException("Owner phone number already exists.")

        return ownerRepository.save(
            Owner(
                id = idGenerator.nextId("OWN"),
                fullName = fullName.trim(),
                phoneNumber = phoneNumber.trim(),
                email = email?.trim()?.ifBlank { null }
            )
        )
    }

    fun getOwner(id: String): Owner = ownerRepository.findById(id)
        ?: throw EntityNotFoundException("Owner with ID $id was not found.")

    fun listOwners(): List<Owner> = ownerRepository.findAll()

    fun searchOwners(keyword: String): List<Owner> = ownerRepository.search(keyword)
}
