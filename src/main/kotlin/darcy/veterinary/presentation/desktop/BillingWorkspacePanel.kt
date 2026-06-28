package darcy.veterinary.presentation.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.presentation.desktop.theme.DarcyColor
import darcy.veterinary.presentation.desktop.viewmodel.BillingCheckoutAction
import darcy.veterinary.presentation.desktop.viewmodel.BillingCheckoutField
import darcy.veterinary.presentation.desktop.viewmodel.BillingCheckoutState
import darcy.veterinary.presentation.desktop.viewmodel.DesktopBillingMode

@Composable
internal fun BillingWorkspacePanel(
    mode: DesktopBillingMode,
    state: BillingCheckoutState,
    onStartInvoice: () -> Unit,
    onPatientIdChange: (String) -> Unit,
    onIssuedAtChange: (String) -> Unit,
    onToggleService: (ClinicService) -> Unit,
    onClearServices: () -> Unit,
    onCreateInvoice: () -> Unit,
    onRequestMarkPaid: () -> Unit,
    onRequestVoid: () -> Unit,
    onConfirmPendingAction: () -> Unit,
    onDismissPendingAction: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onStartInvoice) { Text("New invoice") }
        }
        when (mode) {
            DesktopBillingMode.CREATE,
            DesktopBillingMode.VIEW -> BillingCheckoutPanel(
                state = state,
                onPatientIdChange = onPatientIdChange,
                onIssuedAtChange = onIssuedAtChange,
                onToggleService = onToggleService,
                onClearServices = onClearServices,
                onCreateInvoice = onCreateInvoice,
                onRequestMarkPaid = onRequestMarkPaid,
                onRequestVoid = onRequestVoid,
                onConfirmPendingAction = onConfirmPendingAction,
                onDismissPendingAction = onDismissPendingAction
            )
            DesktopBillingMode.LIST -> EmptyState(
                "Start a new invoice, or open a patient chart and create an invoice from patient context."
            )
        }
    }
}

@Composable
private fun BillingCheckoutPanel(
    state: BillingCheckoutState,
    onPatientIdChange: (String) -> Unit,
    onIssuedAtChange: (String) -> Unit,
    onToggleService: (ClinicService) -> Unit,
    onClearServices: () -> Unit,
    onCreateInvoice: () -> Unit,
    onRequestMarkPaid: () -> Unit,
    onRequestVoid: () -> Unit,
    onConfirmPendingAction: () -> Unit,
    onDismissPendingAction: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = state.invoiceId?.let { "Invoice $it" } ?: "New invoice",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = DarcyColor.TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BillingTextField(
                    value = state.patientId,
                    onValueChange = onPatientIdChange,
                    label = "Patient ID",
                    error = state.fieldErrors[BillingCheckoutField.PATIENT_ID],
                    modifier = Modifier.weight(1f)
                )
                BillingTextField(
                    value = state.issuedAt,
                    onValueChange = onIssuedAtChange,
                    label = "Issued at, YYYY-MM-DDTHH:MM optional",
                    error = state.fieldErrors[BillingCheckoutField.ISSUED_AT],
                    modifier = Modifier.weight(1f)
                )
            }
            ServiceSelector(
                selectedServices = state.selectedServices,
                onToggleService = onToggleService,
                onClearServices = onClearServices,
                error = state.fieldErrors[BillingCheckoutField.SERVICES]
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Total", formatCurrency(state.total), Modifier.weight(1f))
                MetricCard("Status", formatEnumLabel(state.paymentStatus), Modifier.weight(1f))
                MetricCard("Services", state.selectedServices.size.toString(), Modifier.weight(1f))
            }
            state.errorMessage?.let { ErrorState(it) }
            state.successMessage?.let { Text(it, color = DarcyColor.ClinicalAmber, fontWeight = FontWeight.Bold) }
            PendingActionPrompt(
                state = state,
                onConfirmPendingAction = onConfirmPendingAction,
                onDismissPendingAction = onDismissPendingAction
            )
            BillingActionRow(
                state = state,
                onCreateInvoice = onCreateInvoice,
                onRequestMarkPaid = onRequestMarkPaid,
                onRequestVoid = onRequestVoid
            )
        }
    }
}

@Composable
private fun BillingActionRow(
    state: BillingCheckoutState,
    onCreateInvoice: () -> Unit,
    onRequestMarkPaid: () -> Unit,
    onRequestVoid: () -> Unit
) {
    val canCreateInvoice = !state.hasInvoice && state.canAttemptSave
    val canUpdateUnpaidInvoice = state.paymentStatus == PaymentStatus.UNPAID && state.canAttemptSave
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onCreateInvoice, enabled = canCreateInvoice) { Text("Create invoice") }
            Button(onClick = onRequestMarkPaid, enabled = canUpdateUnpaidInvoice) { Text("Mark paid") }
            Button(onClick = onRequestVoid, enabled = canUpdateUnpaidInvoice) { Text("Void invoice") }
        }
        billingActionHint(state)?.let { MutedText(it) }
    }
}

private fun billingActionHint(state: BillingCheckoutState): String? = when (state.paymentStatus) {
    PaymentStatus.PAID -> "Paid invoices are locked against voiding."
    PaymentStatus.VOIDED -> "Voided invoices cannot be marked as paid."
    PaymentStatus.UNPAID -> "Unpaid invoices can be marked paid or voided after confirmation."
    null -> if (state.hasInvoice) "Loaded invoice status is unavailable." else "Draft invoice can be created after patient and services are filled."
}

@Composable
private fun ServiceSelector(
    selectedServices: List<ClinicService>,
    onToggleService: (ClinicService) -> Unit,
    onClearServices: () -> Unit,
    error: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Services", style = MaterialTheme.typography.caption, color = DarcyColor.TextMuted)
        ClinicService.values().forEach { service ->
            val selected = service in selectedServices
            TextButton(onClick = { onToggleService(service) }) {
                Text(
                    text = if (selected) {
                        "• ${service.displayName} — ${formatCurrency(service.defaultCost)}"
                    } else {
                        "${service.displayName} — ${formatCurrency(service.defaultCost)}"
                    },
                    color = if (selected) DarcyColor.ClinicalAmber else DarcyColor.TextSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        TextButton(onClick = onClearServices) { Text("Clear services") }
        error?.let { Text(it, color = DarcyColor.SemanticRed, style = MaterialTheme.typography.caption) }
    }
}

@Composable
private fun PendingActionPrompt(
    state: BillingCheckoutState,
    onConfirmPendingAction: () -> Unit,
    onDismissPendingAction: () -> Unit
) {
    val pending = state.pendingAction ?: return
    val label = when (pending.action) {
        BillingCheckoutAction.MARK_PAID -> "mark invoice ${pending.invoiceId} as paid"
        BillingCheckoutAction.VOID -> "void invoice ${pending.invoiceId}"
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Confirm action", fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
            MutedText("Confirm to $label. Total: ${formatCurrency(pending.total)}. Current status: ${formatEnumLabel(pending.paymentStatus)}.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onConfirmPendingAction, enabled = state.canAttemptSave) { Text("Confirm") }
                TextButton(onClick = onDismissPendingAction) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun BillingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            isError = error != null,
            singleLine = true
        )
        error?.let { Text(it, color = DarcyColor.SemanticRed, style = MaterialTheme.typography.caption) }
    }
}
