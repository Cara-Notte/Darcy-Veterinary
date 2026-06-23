package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.domain.model.MedicalRecord
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class MedicalRecordFormViewModel(
    private val medicalRecordService: MedicalRecordService
) {
    var state: MedicalRecordFormState = MedicalRecordFormState()
        private set

    fun startCreate(patientId: String? = null, appointmentId: String? = null) {
        state = MedicalRecordFormState(
            patientId = patientId.orEmpty(),
            appointmentId = appointmentId.orEmpty()
        )
    }

    fun load(recordId: String) {
        state = state.copy(isLoading = true, errorMessage = null, successMessage = null)
        state = try {
            val record = medicalRecordService.getRecord(recordId)
            MedicalRecordFormState(
                mode = MedicalRecordFormMode.EDIT,
                recordId = record.id,
                patientId = record.petId,
                appointmentId = record.appointmentId.orEmpty(),
                diagnosis = record.diagnosis,
                treatment = record.treatment,
                notes = record.notes,
                recordedAt = record.recordedAt.toString(),
                veterinarianName = record.veterinarianName.orEmpty()
            )
        } catch (error: Exception) {
            state.copy(
                isLoading = false,
                errorMessage = error.message ?: "Medical record could not be loaded."
            )
        }
    }

    fun updatePatientId(value: String) {
        state = state.withFieldUpdate(patientId = value, field = MedicalRecordFormField.PATIENT_ID)
    }

    fun updateAppointmentId(value: String) {
        state = state.copy(appointmentId = value, errorMessage = null, successMessage = null)
    }

    fun updateDiagnosis(value: String) {
        state = state.withFieldUpdate(diagnosis = value, field = MedicalRecordFormField.DIAGNOSIS)
    }

    fun updateTreatment(value: String) {
        state = state.withFieldUpdate(treatment = value, field = MedicalRecordFormField.TREATMENT)
    }

    fun updateNotes(value: String) {
        state = state.copy(notes = value, errorMessage = null, successMessage = null)
    }

    fun updateRecordedAt(value: String) {
        state = state.withFieldUpdate(recordedAt = value, field = MedicalRecordFormField.RECORDED_AT)
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
                savedRecordId = null,
                isSaving = false
            )
            return
        }

        state = state.copy(isSaving = true, errorMessage = null, successMessage = null, savedRecordId = null)

        state = try {
            val record = when (state.mode) {
                MedicalRecordFormMode.CREATE -> medicalRecordService.createRecord(
                    petId = state.patientId,
                    diagnosis = state.diagnosis,
                    treatment = state.treatment,
                    notes = state.notes,
                    appointmentId = state.appointmentId.ifBlank { null },
                    recordedAt = parsed.recordedAt ?: LocalDateTime.now(),
                    veterinarianName = state.veterinarianName.ifBlank { null }
                )
                MedicalRecordFormMode.EDIT -> medicalRecordService.updateRecord(
                    id = state.recordId ?: error("Medical record ID is required when editing a record."),
                    diagnosis = state.diagnosis,
                    treatment = state.treatment,
                    notes = state.notes,
                    veterinarianName = state.veterinarianName.ifBlank { null }
                )
            }
            state.afterSaved(record)
        } catch (error: Exception) {
            state.copy(
                isSaving = false,
                errorMessage = error.message ?: "Medical record could not be saved.",
                successMessage = null,
                savedRecordId = null
            )
        }
    }

    private fun parseAndValidate(): ParsedMedicalRecordForm {
        val errors = linkedMapOf<MedicalRecordFormField, String>()

        if (state.patientId.isBlank()) {
            errors[MedicalRecordFormField.PATIENT_ID] = "Patient is required."
        }
        if (state.diagnosis.isBlank()) {
            errors[MedicalRecordFormField.DIAGNOSIS] = "Diagnosis is required."
        }
        if (state.treatment.isBlank()) {
            errors[MedicalRecordFormField.TREATMENT] = "Treatment is required."
        }

        val parsedRecordedAt = state.recordedAt.toOptionalDateTime(errors)

        return ParsedMedicalRecordForm(
            fieldErrors = errors,
            recordedAt = parsedRecordedAt
        )
    }

    private fun String.toOptionalDateTime(errors: MutableMap<MedicalRecordFormField, String>): LocalDateTime? {
        val trimmed = trim()
        if (trimmed.isEmpty()) return null

        return try {
            LocalDateTime.parse(trimmed)
        } catch (_: DateTimeParseException) {
            errors[MedicalRecordFormField.RECORDED_AT] = "Recorded date and time must use YYYY-MM-DDTHH:MM."
            null
        }
    }

    private fun MedicalRecordFormState.withFieldUpdate(
        patientId: String = this.patientId,
        diagnosis: String = this.diagnosis,
        treatment: String = this.treatment,
        recordedAt: String = this.recordedAt,
        field: MedicalRecordFormField
    ): MedicalRecordFormState = copy(
        patientId = patientId,
        diagnosis = diagnosis,
        treatment = treatment,
        recordedAt = recordedAt,
        fieldErrors = fieldErrors - field,
        errorMessage = null,
        successMessage = null
    )

    private fun MedicalRecordFormState.afterSaved(record: MedicalRecord): MedicalRecordFormState = copy(
        mode = MedicalRecordFormMode.EDIT,
        recordId = record.id,
        patientId = record.petId,
        appointmentId = record.appointmentId.orEmpty(),
        diagnosis = record.diagnosis,
        treatment = record.treatment,
        notes = record.notes,
        recordedAt = record.recordedAt.toString(),
        veterinarianName = record.veterinarianName.orEmpty(),
        fieldErrors = emptyMap(),
        isSaving = false,
        errorMessage = null,
        successMessage = when (mode) {
            MedicalRecordFormMode.CREATE -> "Medical record created."
            MedicalRecordFormMode.EDIT -> "Medical record updated."
        },
        savedRecordId = record.id
    )
}

enum class MedicalRecordFormMode {
    CREATE,
    EDIT
}

enum class MedicalRecordFormField {
    PATIENT_ID,
    DIAGNOSIS,
    TREATMENT,
    RECORDED_AT
}

data class MedicalRecordFormState(
    val mode: MedicalRecordFormMode = MedicalRecordFormMode.CREATE,
    val recordId: String? = null,
    val patientId: String = "",
    val appointmentId: String = "",
    val diagnosis: String = "",
    val treatment: String = "",
    val notes: String = "",
    val recordedAt: String = "",
    val veterinarianName: String = "",
    val fieldErrors: Map<MedicalRecordFormField, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val savedRecordId: String? = null
) {
    val canAttemptSave: Boolean = !isLoading && !isSaving
}

private data class ParsedMedicalRecordForm(
    val fieldErrors: Map<MedicalRecordFormField, String>,
    val recordedAt: LocalDateTime?
)
