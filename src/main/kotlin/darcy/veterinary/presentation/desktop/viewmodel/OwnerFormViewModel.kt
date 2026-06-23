package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.OwnerService
import darcy.veterinary.domain.model.Owner

class OwnerFormViewModel(
    private val ownerService: OwnerService
) {
    var state: OwnerFormState = OwnerFormState()
        private set

    fun startCreate() {
        state = OwnerFormState(mode = OwnerFormMode.CREATE)
    }

    fun load(ownerId: String) {
        state = state.copy(isLoading = true, errorMessage = null, successMessage = null)
        state = try {
            val owner = ownerService.getOwner(ownerId)
            OwnerFormState(
                mode = OwnerFormMode.EDIT,
                ownerId = owner.id,
                fullName = owner.fullName,
                phoneNumber = owner.phoneNumber,
                email = owner.email.orEmpty()
            )
        } catch (error: Exception) {
            state.copy(
                isLoading = false,
                errorMessage = error.message ?: "Owner profile could not be loaded."
            )
        }
    }

    fun updateFullName(value: String) {
        state = state.copy(
            fullName = value,
            fieldErrors = state.fieldErrors - OwnerFormField.FULL_NAME,
            errorMessage = null,
            successMessage = null
        )
    }

    fun updatePhoneNumber(value: String) {
        state = state.copy(
            phoneNumber = value,
            fieldErrors = state.fieldErrors - OwnerFormField.PHONE_NUMBER,
            errorMessage = null,
            successMessage = null
        )
    }

    fun updateEmail(value: String) {
        state = state.copy(
            email = value,
            errorMessage = null,
            successMessage = null
        )
    }

    fun save() {
        val fieldErrors = validate()
        if (fieldErrors.isNotEmpty()) {
            state = state.copy(
                fieldErrors = fieldErrors,
                errorMessage = null,
                successMessage = null,
                savedOwnerId = null,
                isSaving = false
            )
            return
        }

        state = state.copy(isSaving = true, errorMessage = null, successMessage = null, savedOwnerId = null)

        state = try {
            val owner = when (state.mode) {
                OwnerFormMode.CREATE -> ownerService.registerOwner(
                    fullName = state.fullName,
                    phoneNumber = state.phoneNumber,
                    email = state.email.ifBlank { null }
                )
                OwnerFormMode.EDIT -> ownerService.updateOwner(
                    id = state.ownerId ?: error("Owner ID is required when editing an owner."),
                    fullName = state.fullName,
                    phoneNumber = state.phoneNumber,
                    email = state.email.ifBlank { null }
                )
            }
            state.afterSaved(owner)
        } catch (error: Exception) {
            state.copy(
                isSaving = false,
                errorMessage = error.message ?: "Owner profile could not be saved.",
                successMessage = null,
                savedOwnerId = null
            )
        }
    }

    private fun validate(): Map<OwnerFormField, String> = buildMap {
        if (state.fullName.isBlank()) {
            put(OwnerFormField.FULL_NAME, "Owner name is required.")
        }
        if (state.phoneNumber.isBlank()) {
            put(OwnerFormField.PHONE_NUMBER, "Phone number is required.")
        }
    }

    private fun OwnerFormState.afterSaved(owner: Owner): OwnerFormState = copy(
        mode = OwnerFormMode.EDIT,
        ownerId = owner.id,
        fullName = owner.fullName,
        phoneNumber = owner.phoneNumber,
        email = owner.email.orEmpty(),
        fieldErrors = emptyMap(),
        isSaving = false,
        errorMessage = null,
        successMessage = when (mode) {
            OwnerFormMode.CREATE -> "Owner profile created."
            OwnerFormMode.EDIT -> "Owner profile updated."
        },
        savedOwnerId = owner.id
    )
}

enum class OwnerFormMode {
    CREATE,
    EDIT
}

enum class OwnerFormField {
    FULL_NAME,
    PHONE_NUMBER
}

data class OwnerFormState(
    val mode: OwnerFormMode = OwnerFormMode.CREATE,
    val ownerId: String? = null,
    val fullName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val fieldErrors: Map<OwnerFormField, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val savedOwnerId: String? = null
) {
    val canAttemptSave: Boolean = !isLoading && !isSaving
}
