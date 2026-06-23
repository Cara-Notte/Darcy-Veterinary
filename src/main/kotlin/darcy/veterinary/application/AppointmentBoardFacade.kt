package darcy.veterinary.application

import darcy.veterinary.domain.model.Appointment
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
            .map { it.toBoardRow() }

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

    fun completeAppointment(id: String): AppointmentBoardRow =
        appointmentService.completeAppointment(id).toBoardRow()

    fun cancelAppointment(id: String): AppointmentBoardRow =
        appointmentService.cancelAppointment(id).toBoardRow()

    private fun Appointment.toBoardRow(): AppointmentBoardRow {
        val patient = patientService.getPet(petId)
        val owner = ownerService.getOwner(patient.ownerId)

        return AppointmentBoardRow(
            id = id,
            scheduledAt = scheduledAt,
            status = status,
            reason = reason,
            visitType = visitType,
            veterinarianName = veterinarianName,
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
