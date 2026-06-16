package darcy.veterinary.domain.model

data class Owner(
    val id: String,
    val fullName: String,
    val phoneNumber: String,
    val email: String? = null
)
