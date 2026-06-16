package darcy.veterinary.domain.model

data class Pet(
    val id: String,
    val ownerId: String,
    val name: String,
    val species: String,
    val breed: String? = null,
    val age: Int? = null
)
