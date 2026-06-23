package darcy.veterinary.application

import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.VisitType
import java.time.LocalDate
import java.time.LocalDateTime

class AppointmentBoardFacade(
    private val ownerService: OwnerService,
    private val patientService: PatientService,
    private val appointmentService: AppointmentService
) {
    fun dayBoard(
        date: LocalDate,
        statusFilter: AppointmentStatus? = null
    ): AppointmentBoardViewData {
        val appointmentsForDate = appointmentService.listAppointments()
            .filter { it.scheduledAt.toLocalDate() == date }
            .sortedBy { it.scheduledAt }

        val rows = appointmentsForDate
            .filter { statusFilter == null || it.status == statusFilter }
            .map { appointment ->
                val patient = patientService.getPet(appointment.petId)
                val owner = ownerService.getOwner(patient.ownerId)

                AppointmentBoardRow(
                    id = appointment.id,
                    scheduledAt = appointment.scheduledAt,
                    status = appointment.status,
                    reason = appointment.reason,
                    visitType = appointment.visitType,
                    veterinarianName = appointment.veterinarianName,
                    patientId = patient.id,
                    patientName = patient.name,
                    species = patient.species,
                    breed = patient.breed,
                    ownerId = owner.id,
                    ownerName = owner.fullName,
                    ownerPhoneNumber = owner.phoneNumber,
                    hasPatientAlerts = patient.allergies.isNotEmpty() || patient.medicalConditions.isNotEmpty()
                )
            }

        return AppointmentBoardViewData(
            date = date,
            statusFilter = statusFilter,
            rows = rows,
            summary = AppointmentBoardSummary(
                totalCount = appointmentsForDate.size,
                scheduledCount = appointmentsForDate.count { it.status == AppointmentStatus.SCHEDULED },
                completedCount = appointmentsForDate.count { it.status == AppointmentStatus.COMPLETED },
                cancelledCount = appointmentsForDate.count { it.status == AppointmentStatus.CANCELLED }
            )
        )
    }
}

data class AppointmentBoardViewData(
    val date: LocalDate,
    val statusFilter: AppointmentStatus?,
    val rows: List<AppointmentBoardRow>,
    val summary: AppointmentBoardSummary
) {
    val hasAppointments: Boolean = rows.isNotEmpty()
}

data class AppointmentBoardRow(
    val id: String,
    val scheduledAt: LocalDateTime,
    val status: AppointmentStatus,
    val reason: String,
    val visitType: VisitType,
    val veterinarianName: String?,
    val patientId: String,
    val patientName: String,
    val species: String,
    val breed: String?,
    val ownerId: String,
    val ownerName: String,
    val ownerPhoneNumber: String,
    val hasPatientAlerts: Boolean
)

data class AppointmentBoardSummary(
    val totalCount: Int,
    val scheduledCount: Int,
    val completedCount: Int,
    val cancelledCount: Int
)
