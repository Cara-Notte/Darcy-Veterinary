package darcy.veterinary.domain.model

import java.time.LocalDateTime

data class MedicalRecordRevision(
    val id: String,
    val recordId: String,
    val diagnosis: String,
    val treatment: String,
    val notes: String,
    val changedAt: LocalDateTime
)
