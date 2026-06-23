package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.BillingService
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.Invoice
import darcy.veterinary.domain.model.PaymentStatus
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class BillingCheckoutViewModel(
    private val billingService: BillingService
) {
    var state: BillingCheckoutState = BillingCheckoutState()
        private set

    fun startInvoice(patientId: String? = null) {
        state = BillingCheckoutState(patientId = patientId.orEmpty())
    }

    fun loadInvoice(invoiceId: String) {
        state = state.copy(isLoading = true, errorMessage = null, successMessage = null, pendingAction = null)
        state = try {
            val invoice = billingService.getInvoice(invoiceId)
            state.fromInvoice(invoice, successMessage = null)
        } catch (error: Exception) {
            state.copy(
                isLoading = false,
                errorMessage = error.message ?: "Invoice could not be loaded."
            )
        }
    }

    fun updatePatientId(value: String) {
        state = state.copy(
            patientId = value,
            fieldErrors = state.fieldErrors - BillingCheckoutField.PATIENT_ID,
            errorMessage = null,
            successMessage = null,
            pendingAction = null
        )
    }

    fun updateIssuedAt(value: String) {
        state = state.copy(
            issuedAt = value,
            fieldErrors = state.fieldErrors - BillingCheckoutField.ISSUED_AT,
            errorMessage = null,
            successMessage = null,
            pendingAction = null
        )
    }

    fun toggleService(service: ClinicService) {
        val nextServices = if (service in state.selectedServices) {
            state.selectedServices - service
        } else {
            state.selectedServices + service
        }
        state = state.copy(
            selectedServices = nextServices,
            fieldErrors = state.fieldErrors - BillingCheckoutField.SERVICES,
            errorMessage = null,
            successMessage = null,
            pendingAction = null
        )
    }

    fun clearServices() {
        state = state.copy(
            selectedServices = emptyList(),
            fieldErrors = state.fieldErrors - BillingCheckoutField.SERVICES,
            errorMessage = null,
            successMessage = null,
            pendingAction = null
        )
    }

    fun createInvoice() {
        val parsed = parseAndValidateInvoiceDraft()
        if (parsed.fieldErrors.isNotEmpty()) {
            state = state.copy(
                fieldErrors = parsed.fieldErrors,
                errorMessage = null,
                successMessage = null,
                pendingAction = null,
                isSaving = false
            )
            return
        }

        state = state.copy(isSaving = true, errorMessage = null, successMessage = null, pendingAction = null)

        state = try {
            val invoice = billingService.createInvoice(
                petId = state.patientId,
                services = state.selectedServices,
                issuedAt = parsed.issuedAt ?: LocalDateTime.now()
            )
            state.fromInvoice(invoice, successMessage = "Invoice created.")
        } catch (error: Exception) {
            state.copy(
                isSaving = false,
                errorMessage = error.message ?: "Invoice could not be created.",
                successMessage = null,
                pendingAction = null
            )
        }
    }

    fun requestMarkPaid() {
        requestAction(BillingCheckoutAction.MARK_PAID)
    }

    fun requestVoidInvoice() {
        requestAction(BillingCheckoutAction.VOID)
    }

    fun dismissPendingAction() {
        state = state.copy(pendingAction = null)
    }

    fun confirmPendingAction() {
        val pendingAction = state.pendingAction
        val invoiceId = state.invoiceId
        if (pendingAction == null || invoiceId == null) {
            state = state.copy(
                errorMessage = "No invoice action is awaiting confirmation.",
                successMessage = null,
                pendingAction = null
            )
            return
        }

        state = state.copy(isSaving = true, errorMessage = null, successMessage = null)
        state = try {
            val invoice = when (pendingAction.action) {
                BillingCheckoutAction.MARK_PAID -> billingService.markAsPaid(invoiceId)
                BillingCheckoutAction.VOID -> billingService.voidInvoice(invoiceId)
            }
            state.fromInvoice(
                invoice = invoice,
                successMessage = when (pendingAction.action) {
                    BillingCheckoutAction.MARK_PAID -> "Invoice marked as paid."
                    BillingCheckoutAction.VOID -> "Invoice voided."
                }
            )
        } catch (error: Exception) {
            state.copy(
                isSaving = false,
                pendingAction = null,
                errorMessage = error.message ?: "Invoice action failed.",
                successMessage = null
            )
        }
    }

    private fun requestAction(action: BillingCheckoutAction) {
        val invoiceId = state.invoiceId
        if (invoiceId == null) {
            state = state.copy(
                pendingAction = null,
                errorMessage = "Create or load an invoice before taking payment actions.",
                successMessage = null
            )
            return
        }

        state = state.copy(
            pendingAction = PendingBillingCheckoutAction(
                action = action,
                invoiceId = invoiceId,
                total = state.total,
                paymentStatus = state.paymentStatus
            ),
            errorMessage = null,
            successMessage = null
        )
    }

    private fun parseAndValidateInvoiceDraft(): ParsedBillingCheckoutForm {
        val errors = linkedMapOf<BillingCheckoutField, String>()
        if (state.patientId.isBlank()) {
            errors[BillingCheckoutField.PATIENT_ID] = "Patient is required."
        }
        if (state.selectedServices.isEmpty()) {
            errors[BillingCheckoutField.SERVICES] = "At least one service is required."
        }
        val parsedIssuedAt = state.issuedAt.toOptionalDateTime(errors)

        return ParsedBillingCheckoutForm(
            fieldErrors = errors,
            issuedAt = parsedIssuedAt
        )
    }

    private fun String.toOptionalDateTime(errors: MutableMap<BillingCheckoutField, String>): LocalDateTime? {
        val trimmed = trim()
        if (trimmed.isEmpty()) return null

        return try {
            LocalDateTime.parse(trimmed)
        } catch (_: DateTimeParseException) {
            errors[BillingCheckoutField.ISSUED_AT] = "Issued date and time must use YYYY-MM-DDTHH:MM."
            null
        }
    }

    private fun BillingCheckoutState.fromInvoice(invoice: Invoice, successMessage: String?): BillingCheckoutState = copy(
        invoiceId = invoice.id,
        patientId = invoice.petId,
        issuedAt = invoice.issuedAt.toString(),
        selectedServices = invoice.items.map { it.service },
        paymentStatus = invoice.paymentStatus,
        fieldErrors = emptyMap(),
        isLoading = false,
        isSaving = false,
        errorMessage = null,
        successMessage = successMessage,
        pendingAction = null
    )
}

enum class BillingCheckoutAction {
    MARK_PAID,
    VOID
}

enum class BillingCheckoutField {
    PATIENT_ID,
    SERVICES,
    ISSUED_AT
}

data class PendingBillingCheckoutAction(
    val action: BillingCheckoutAction,
    val invoiceId: String,
    val total: Double,
    val paymentStatus: PaymentStatus?
)

data class BillingCheckoutState(
    val invoiceId: String? = null,
    val patientId: String = "",
    val issuedAt: String = "",
    val selectedServices: List<ClinicService> = emptyList(),
    val paymentStatus: PaymentStatus? = null,
    val fieldErrors: Map<BillingCheckoutField, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val pendingAction: PendingBillingCheckoutAction? = null
) {
    val total: Double = selectedServices.sumOf { it.defaultCost }
    val hasInvoice: Boolean = invoiceId != null
    val canAttemptSave: Boolean = !isLoading && !isSaving
}

private data class ParsedBillingCheckoutForm(
    val fieldErrors: Map<BillingCheckoutField, String>,
    val issuedAt: LocalDateTime?
)
