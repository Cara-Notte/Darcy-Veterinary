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

        ensurePhoneNumberIsUnique(phoneNumber.trim())

        return ownerRepository.save(
            Owner(
                id = idGenerator.nextId("OWN"),
                fullName = fullName.trim(),
                phoneNumber = phoneNumber.trim(),
                email = email?.trim()?.ifBlank { null }
            )
        )
    }

    fun updateOwner(id: String, fullName: String, phoneNumber: String, email: String? = null): Owner {
        require(fullName.isNotBlank()) { "Owner name cannot be blank." }
        require(phoneNumber.isNotBlank()) { "Phone number cannot be blank." }
        getOwner(id)
        ensurePhoneNumberIsUnique(phoneNumber.trim(), ignoredOwnerId = id)

        return ownerRepository.save(
            Owner(
                id = id,
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

    private fun ensurePhoneNumberIsUnique(phoneNumber: String, ignoredOwnerId: String? = null) {
        val duplicate = ownerRepository.findAll().any { owner ->
            owner.phoneNumber == phoneNumber && owner.id != ignoredOwnerId
        }
        if (duplicate) throw DuplicateEntityException("Owner phone number already exists.")
    }
}
