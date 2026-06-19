package darcy.veterinary.domain.model

import java.time.LocalDateTime

data class MedicalRecord(
    val id: String,
    val petId: String,
    val appointmentId: String? = null,
    val diagnosis: String,
    val treatment: String,
    val notes: String,
    val recordedAt: LocalDateTime,
    val veterinarianName: String? = null
)
