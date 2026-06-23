package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.domain.model.Appointment
import darcy.veterinary.domain.model.VisitType
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class AppointmentFormViewModel(
    private val appointmentService: AppointmentService
) {
    var state: AppointmentFormState = AppointmentFormState()
        private set

    fun startCreate(patientId: String? = null) {
        state = AppointmentFormState(patientId = patientId.orEmpty())
    }

    fun load(appointmentId: String) {
        state = state.copy(isLoading = true, errorMessage = null, successMessage = null)
        state = try {
            val appointment = appointmentService.getAppointment(appointmentId)
            AppointmentFormState(
                mode = AppointmentFormMode.EDIT,
                appointmentId = appointment.id,
                patientId = appointment.petId,
                scheduledAt = appointment.scheduledAt.toString(),
                reason = appointment.reason,
                visitType = appointment.visitType,
                veterinarianName = appointment.veterinarianName.orEmpty()
            )
        } catch (error: Exception) {
            state.copy(
                isLoading = false,
                errorMessage = error.message ?: "Appointment could not be loaded."
            )
        }
    }

    fun updatePatientId(value: String) {
        state = state.withFieldUpdate(patientId = value, field = AppointmentFormField.PATIENT_ID)
    }

    fun updateScheduledAt(value: String) {
        state = state.withFieldUpdate(scheduledAt = value, field = AppointmentFormField.SCHEDULED_AT)
    }

    fun updateReason(value: String) {
        state = state.withFieldUpdate(reason = value, field = AppointmentFormField.REASON)
    }

    fun updateVisitType(value: VisitType) {
        state = state.copy(visitType = value, errorMessage = null, successMessage = null)
    }

    fun updateVeterinarianName(value: String) {
        state = state.copy(veterinarianName = value, errorMessage = null, successMessage = null)
    }

    fun save() {
        val parsed = parseAndValidate()
        if (parsed.fieldErrors.isNotEmpty()) {
            state = state.copy(
                fieldErrors = parsed.fieldErrors,
                errorMessage = null,
                successMessage = null,
                savedAppointmentId = null,
                isSaving = false
            )
            return
        }

        state = state.copy(isSaving = true, errorMessage = null, successMessage = null, savedAppointmentId = null)

        state = try {
            val appointment = when (state.mode) {
                AppointmentFormMode.CREATE -> appointmentService.scheduleAppointment(
                    petId = state.patientId,
                    scheduledAt = parsed.scheduledAt ?: error("Scheduled date and time are required."),
                    reason = state.reason,
                    visitType = state.visitType,
                    veterinarianName = state.veterinarianName.ifBlank { null }
                )
                AppointmentFormMode.EDIT -> appointmentService.rescheduleAppointment(
                    id = state.appointmentId ?: error("Appointment ID is required when editing an appointment."),
                    scheduledAt = parsed.scheduledAt ?: error("Scheduled date and time are required."),
                    reason = state.reason,
                    visitType = state.visitType,
                    veterinarianName = state.veterinarianName.ifBlank { null }
                )
            }
            state.afterSaved(appointment)
        } catch (error: Exception) {
            state.copy(
                isSaving = false,
                errorMessage = error.message ?: "Appointment could not be saved.",
                successMessage = null,
                savedAppointmentId = null
            )
        }
    }

    private fun parseAndValidate(): ParsedAppointmentForm {
        val errors = linkedMapOf<AppointmentFormField, String>()

        if (state.patientId.isBlank()) {
            errors[AppointmentFormField.PATIENT_ID] = "Patient is required."
        }
        if (state.reason.isBlank()) {
            errors[AppointmentFormField.REASON] = "Appointment reason is required."
        }

        val parsedScheduledAt = state.scheduledAt.toOptionalDateTime(errors)

        return ParsedAppointmentForm(
            fieldErrors = errors,
            scheduledAt = parsedScheduledAt
        )
    }

    private fun String.toOptionalDateTime(errors: MutableMap<AppointmentFormField, String>): LocalDateTime? {
        val trimmed = trim()
        if (trimmed.isEmpty()) {
            errors[AppointmentFormField.SCHEDULED_AT] = "Scheduled date and time are required."
            return null
        }

        return try {
            LocalDateTime.parse(trimmed)
        } catch (_: DateTimeParseException) {
            errors[AppointmentFormField.SCHEDULED_AT] = "Scheduled date and time must use YYYY-MM-DDTHH:MM."
            null
        }
    }

    private fun AppointmentFormState.withFieldUpdate(
        patientId: String = this.patientId,
        scheduledAt: String = this.scheduledAt,
        reason: String = this.reason,
        field: AppointmentFormField
    ): AppointmentFormState = copy(
        patientId = patientId,
        scheduledAt = scheduledAt,
        reason = reason,
        fieldErrors = fieldErrors - field,
        errorMessage = null,
        successMessage = null
    )

    private fun AppointmentFormState.afterSaved(appointment: Appointment): AppointmentFormState = copy(
        mode = AppointmentFormMode.EDIT,
        appointmentId = appointment.id,
        patientId = appointment.petId,
        scheduledAt = appointment.scheduledAt.toString(),
        reason = appointment.reason,
        visitType = appointment.visitType,
        veterinarianName = appointment.veterinarianName.orEmpty(),
        fieldErrors = emptyMap(),
        isSaving = false,
        errorMessage = null,
        successMessage = when (mode) {
            AppointmentFormMode.CREATE -> "Appointment scheduled."
            AppointmentFormMode.EDIT -> "Appointment updated."
        },
        savedAppointmentId = appointment.id
    )
}

enum class AppointmentFormMode {
    CREATE,
    EDIT
}

enum class AppointmentFormField {
    PATIENT_ID,
    SCHEDULED_AT,
    REASON
}

data class AppointmentFormState(
    val mode: AppointmentFormMode = AppointmentFormMode.CREATE,
    val appointmentId: String? = null,
    val patientId: String = "",
    val scheduledAt: String = "",
    val reason: String = "",
    val visitType: VisitType = VisitType.GENERAL,
    val veterinarianName: String = "",
    val fieldErrors: Map<AppointmentFormField, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val savedAppointmentId: String? = null
) {
    val canAttemptSave: Boolean = !isLoading && !isSaving
}

private data class ParsedAppointmentForm(
    val fieldErrors: Map<AppointmentFormField, String>,
    val scheduledAt: LocalDateTime?
)
