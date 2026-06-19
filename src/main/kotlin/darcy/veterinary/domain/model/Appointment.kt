package darcy.veterinary.domain.model

import java.time.LocalDateTime

data class Appointment(
    val id: String,
    val petId: String,
    val scheduledAt: LocalDateTime,
    val reason: String,
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val visitType: VisitType = VisitType.GENERAL,
    val veterinarianName: String? = null
)
