package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.PatientService
import darcy.veterinary.domain.model.Pet
import darcy.veterinary.domain.model.PetSex
import java.time.LocalDate
import java.time.format.DateTimeParseException

class PatientFormViewModel(
    private val patientService: PatientService
) {
    var state: PatientFormState = PatientFormState()
        private set

    fun startCreate(ownerId: String? = null) {
        state = PatientFormState(ownerId = ownerId.orEmpty())
    }

    fun load(patientId: String) {
        state = state.copy(isLoading = true, errorMessage = null, successMessage = null)
        state = try {
            val patient = patientService.getPet(patientId)
            PatientFormState(
                mode = PatientFormMode.EDIT,
                patientId = patient.id,
                ownerId = patient.ownerId,
                name = patient.name,
                species = patient.species,
                breed = patient.breed.orEmpty(),
                age = patient.age?.toString().orEmpty(),
                sex = patient.sex,
                dateOfBirth = patient.dateOfBirth?.toString().orEmpty(),
                weightKg = patient.weightKg?.toString().orEmpty(),
                allergies = patient.allergies.joinToString("\n"),
                medicalConditions = patient.medicalConditions.joinToString("\n")
            )
        } catch (error: Exception) {
            state.copy(
                isLoading = false,
                errorMessage = error.message ?: "Patient profile could not be loaded."
            )
        }
    }

    fun updateOwnerId(value: String) {
        state = state.withFieldUpdate(ownerId = value, field = PatientFormField.OWNER_ID)
    }

    fun updateName(value: String) {
        state = state.withFieldUpdate(name = value, field = PatientFormField.NAME)
    }

    fun updateSpecies(value: String) {
        state = state.withFieldUpdate(species = value, field = PatientFormField.SPECIES)
    }

    fun updateBreed(value: String) {
        state = state.copy(breed = value, errorMessage = null, successMessage = null)
    }

    fun updateAge(value: String) {
        state = state.withFieldUpdate(age = value, field = PatientFormField.AGE)
    }

    fun updateSex(value: PetSex?) {
        state = state.copy(sex = value, errorMessage = null, successMessage = null)
    }

    fun updateDateOfBirth(value: String) {
        state = state.withFieldUpdate(dateOfBirth = value, field = PatientFormField.DATE_OF_BIRTH)
    }

    fun updateWeightKg(value: String) {
        state = state.withFieldUpdate(weightKg = value, field = PatientFormField.WEIGHT_KG)
    }

    fun updateAllergies(value: String) {
        state = state.copy(allergies = value, errorMessage = null, successMessage = null)
    }

    fun updateMedicalConditions(value: String) {
        state = state.copy(medicalConditions = value, errorMessage = null, successMessage = null)
    }

    fun save() {
        val parsed = parseAndValidate()
        if (parsed.fieldErrors.isNotEmpty()) {
            state = state.copy(
                fieldErrors = parsed.fieldErrors,
                errorMessage = null,
                successMessage = null,
                savedPatientId = null,
                isSaving = false
            )
            return
        }

        state = state.copy(isSaving = true, errorMessage = null, successMessage = null, savedPatientId = null)

        state = try {
            val patient = when (state.mode) {
                PatientFormMode.CREATE -> patientService.registerPet(
                    ownerId = state.ownerId,
                    name = state.name,
                    species = state.species,
                    breed = state.breed.ifBlank { null },
                    age = parsed.age,
                    sex = state.sex,
                    dateOfBirth = parsed.dateOfBirth,
                    weightKg = parsed.weightKg,
                    allergies = parseList(state.allergies),
                    medicalConditions = parseList(state.medicalConditions)
                )
                PatientFormMode.EDIT -> patientService.updatePet(
                    id = state.patientId ?: error("Patient ID is required when editing a patient."),
                    name = state.name,
                    species = state.species,
                    breed = state.breed.ifBlank { null },
                    age = parsed.age,
                    sex = state.sex,
                    dateOfBirth = parsed.dateOfBirth,
                    weightKg = parsed.weightKg,
                    allergies = parseList(state.allergies),
                    medicalConditions = parseList(state.medicalConditions)
                )
            }
            state.afterSaved(patient)
        } catch (error: Exception) {
            state.copy(
                isSaving = false,
                errorMessage = error.message ?: "Patient profile could not be saved.",
                successMessage = null,
                savedPatientId = null
            )
        }
    }

    private fun parseAndValidate(): ParsedPatientForm {
        val errors = linkedMapOf<PatientFormField, String>()

        if (state.ownerId.isBlank()) {
            errors[PatientFormField.OWNER_ID] = "Owner is required."
        }
        if (state.name.isBlank()) {
            errors[PatientFormField.NAME] = "Patient name is required."
        }
        if (state.species.isBlank()) {
            errors[PatientFormField.SPECIES] = "Species is required."
        }

        val parsedAge = state.age.toOptionalInt(
            field = PatientFormField.AGE,
            errors = errors,
            invalidMessage = "Age must be a whole number.",
            invalidRangeMessage = "Age cannot be negative."
        )
        val parsedWeight = state.weightKg.toOptionalDouble(
            field = PatientFormField.WEIGHT_KG,
            errors = errors,
            invalidMessage = "Weight must be a number.",
            invalidRangeMessage = "Weight must be greater than zero."
        )
        val parsedDate = state.dateOfBirth.toOptionalDate(errors)

        return ParsedPatientForm(
            fieldErrors = errors,
            age = parsedAge,
            weightKg = parsedWeight,
            dateOfBirth = parsedDate
        )
    }

    private fun String.toOptionalInt(
        field: PatientFormField,
        errors: MutableMap<PatientFormField, String>,
        invalidMessage: String,
        invalidRangeMessage: String
    ): Int? {
        val trimmed = trim()
        if (trimmed.isEmpty()) return null
        val value = trimmed.toIntOrNull()
        if (value == null) {
            errors[field] = invalidMessage
            return null
        }
        if (value < 0) {
            errors[field] = invalidRangeMessage
            return null
        }
        return value
    }

    private fun String.toOptionalDouble(
        field: PatientFormField,
        errors: MutableMap<PatientFormField, String>,
        invalidMessage: String,
        invalidRangeMessage: String
    ): Double? {
        val trimmed = trim()
        if (trimmed.isEmpty()) return null
        val value = trimmed.toDoubleOrNull()
        if (value == null) {
            errors[field] = invalidMessage
            return null
        }
        if (value <= 0.0) {
            errors[field] = invalidRangeMessage
            return null
        }
        return value
    }

    private fun String.toOptionalDate(errors: MutableMap<PatientFormField, String>): LocalDate? {
        val trimmed = trim()
        if (trimmed.isEmpty()) return null
        return try {
            LocalDate.parse(trimmed)
        } catch (_: DateTimeParseException) {
            errors[PatientFormField.DATE_OF_BIRTH] = "Date of birth must use YYYY-MM-DD."
            null
        }
    }

    private fun parseList(value: String): List<String> =
        value.split(',', ';', '\n')
            .map(String::trim)
            .filter(String::isNotBlank)

    private fun PatientFormState.withFieldUpdate(
        ownerId: String = this.ownerId,
        name: String = this.name,
        species: String = this.species,
        age: String = this.age,
        dateOfBirth: String = this.dateOfBirth,
        weightKg: String = this.weightKg,
        field: PatientFormField
    ): PatientFormState = copy(
        ownerId = ownerId,
        name = name,
        species = species,
        age = age,
        dateOfBirth = dateOfBirth,
        weightKg = weightKg,
        fieldErrors = fieldErrors - field,
        errorMessage = null,
        successMessage = null
    )

    private fun PatientFormState.afterSaved(patient: Pet): PatientFormState = copy(
        mode = PatientFormMode.EDIT,
        patientId = patient.id,
        ownerId = patient.ownerId,
        name = patient.name,
        species = patient.species,
        breed = patient.breed.orEmpty(),
        age = patient.age?.toString().orEmpty(),
        sex = patient.sex,
        dateOfBirth = patient.dateOfBirth?.toString().orEmpty(),
        weightKg = patient.weightKg?.toString().orEmpty(),
        allergies = patient.allergies.joinToString("\n"),
        medicalConditions = patient.medicalConditions.joinToString("\n"),
        fieldErrors = emptyMap(),
        isSaving = false,
        errorMessage = null,
        successMessage = when (mode) {
            PatientFormMode.CREATE -> "Patient profile created."
            PatientFormMode.EDIT -> "Patient profile updated."
        },
        savedPatientId = patient.id
    )
}

enum class PatientFormMode {
    CREATE,
    EDIT
}

enum class PatientFormField {
    OWNER_ID,
    NAME,
    SPECIES,
    AGE,
    DATE_OF_BIRTH,
    WEIGHT_KG
}

data class PatientFormState(
    val mode: PatientFormMode = PatientFormMode.CREATE,
    val patientId: String? = null,
    val ownerId: String = "",
    val name: String = "",
    val species: String = "",
    val breed: String = "",
    val age: String = "",
    val sex: PetSex? = null,
    val dateOfBirth: String = "",
    val weightKg: String = "",
    val allergies: String = "",
    val medicalConditions: String = "",
    val fieldErrors: Map<PatientFormField, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val savedPatientId: String? = null
) {
    val canAttemptSave: Boolean = !isLoading && !isSaving
}

private data class ParsedPatientForm(
    val fieldErrors: Map<PatientFormField, String>,
    val age: Int?,
    val weightKg: Double?,
    val dateOfBirth: LocalDate?
)
