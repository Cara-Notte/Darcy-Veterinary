package darcy.veterinary.domain.model

import java.time.LocalDate

data class Pet(
    val id: String,
    val ownerId: String,
    val name: String,
    val species: String,
    val breed: String? = null,
    val age: Int? = null,
    val sex: PetSex? = null,
    val dateOfBirth: LocalDate? = null,
    val weightKg: Double? = null,
    val allergies: List<String> = emptyList(),
    val medicalConditions: List<String> = emptyList()
)
