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
import darcy.veterinary.application.AppointmentBoardRow
import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.VisitType
import darcy.veterinary.presentation.desktop.theme.DarcyColor
import darcy.veterinary.presentation.desktop.viewmodel.AppointmentBoardAction
import darcy.veterinary.presentation.desktop.viewmodel.AppointmentBoardState
import darcy.veterinary.presentation.desktop.viewmodel.AppointmentFormField
import darcy.veterinary.presentation.desktop.viewmodel.AppointmentFormMode
import darcy.veterinary.presentation.desktop.viewmodel.AppointmentFormState
import darcy.veterinary.presentation.desktop.viewmodel.PendingAppointmentAction
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun AppointmentWorkspacePanel(
    boardState: AppointmentBoardState,
    formState: AppointmentFormState,
    onRefresh: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onApplyStatusFilter: (AppointmentStatus?) -> Unit,
    onStartCreate: (String?) -> Unit,
    onLoadAppointment: (String) -> Unit,
    onRequestComplete: (String) -> Unit,
    onRequestCancel: (String) -> Unit,
    onConfirmPendingAction: () -> Unit,
    onDismissPendingAction: () -> Unit,
    onPatientIdChange: (String) -> Unit,
    onScheduledAtChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onVisitTypeChange: (VisitType) -> Unit,
    onVeterinarianChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRefresh) { Text("Refresh appointments") }
            Button(onClick = { onStartCreate(null) }) { Text("Schedule appointment") }
        }
        AppointmentBoardControls(
            state = boardState,
            onSelectDate = onSelectDate,
            onApplyStatusFilter = onApplyStatusFilter
        )
        AppointmentFormPanel(
            state = formState,
            onPatientIdChange = onPatientIdChange,
            onScheduledAtChange = onScheduledAtChange,
            onReasonChange = onReasonChange,
            onVisitTypeChange = onVisitTypeChange,
            onVeterinarianChange = onVeterinarianChange,
            onSave = onSave
        )
        boardState.board?.let { board ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Scheduled", board.summary.scheduledCount.toString(), Modifier.weight(1f))
                MetricCard("Completed", board.summary.completedCount.toString(), Modifier.weight(1f))
                MetricCard("Cancelled", board.summary.cancelledCount.toString(), Modifier.weight(1f))
            }
            if (board.rows.isEmpty()) {
                EmptyState(boardState.emptyStateMessage ?: "No appointments to show.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    board.rows.forEach { row ->
                        AppointmentBoardRowCard(
                            row = row,
                            onLoadAppointment = onLoadAppointment,
                            onRequestComplete = onRequestComplete,
                            onRequestCancel = onRequestCancel
                        )
                    }
                }
            }
        } ?: boardState.emptyStateMessage?.let { EmptyState(it) }
        boardState.pendingAction?.let { pending ->
            PendingAppointmentActionPrompt(
                pending = pending,
                onConfirmPendingAction = onConfirmPendingAction,
                onDismissPendingAction = onDismissPendingAction
            )
        }
        boardState.successMessage?.let { Text(it, color = DarcyColor.ClinicalAmber, fontWeight = FontWeight.Bold) }
        boardState.errorMessage?.let { ErrorState(it) }
    }
}

@Composable
private fun AppointmentBoardControls(
    state: AppointmentBoardState,
    onSelectDate: (LocalDate) -> Unit,
    onApplyStatusFilter: (AppointmentStatus?) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Board filters", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = { onSelectDate(state.selectedDate.minusDays(1)) }) { Text("Previous day") }
                TextButton(onClick = { onSelectDate(LocalDate.now()) }) { Text("Today") }
                TextButton(onClick = { onSelectDate(state.selectedDate.plusDays(1)) }) { Text("Next day") }
                MutedText("Selected date: ${state.selectedDate}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppointmentStatusFilterButton("All", state.statusFilter == null) { onApplyStatusFilter(null) }
                AppointmentStatus.values().forEach { status ->
                    AppointmentStatusFilterButton(
                        label = formatAppointmentStatus(status),
                        selected = state.statusFilter == status,
                        onClick = { onApplyStatusFilter(status) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentStatusFilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = if (selected) "• $label" else label,
            color = if (selected) DarcyColor.ClinicalAmber else DarcyColor.TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun AppointmentFormPanel(
    state: AppointmentFormState,
    onPatientIdChange: (String) -> Unit,
    onScheduledAtChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onVisitTypeChange: (VisitType) -> Unit,
    onVeterinarianChange: (String) -> Unit,
    onSave: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = when (state.mode) {
                    AppointmentFormMode.CREATE -> "Schedule appointment"
                    AppointmentFormMode.EDIT -> "Edit appointment"
                },
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = DarcyColor.TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                AppointmentTextField(
                    value = state.patientId,
                    onValueChange = onPatientIdChange,
                    label = "Patient ID",
                    error = state.fieldErrors[AppointmentFormField.PATIENT_ID],
                    modifier = Modifier.weight(1f)
                )
                AppointmentTextField(
                    value = state.scheduledAt,
                    onValueChange = onScheduledAtChange,
                    label = "Scheduled at, YYYY-MM-DDTHH:MM",
                    error = state.fieldErrors[AppointmentFormField.SCHEDULED_AT],
                    modifier = Modifier.weight(1f)
                )
            }
            AppointmentTextField(
                value = state.reason,
                onValueChange = onReasonChange,
                label = "Reason",
                error = state.fieldErrors[AppointmentFormField.REASON]
            )
            VisitTypeSelector(state.visitType, onVisitTypeChange)
            AppointmentTextField(
                value = state.veterinarianName,
                onValueChange = onVeterinarianChange,
                label = "Veterinarian"
            )
            state.errorMessage?.let { ErrorState(it) }
            state.successMessage?.let { Text(it, color = DarcyColor.ClinicalAmber, fontWeight = FontWeight.Bold) }
            Button(onClick = onSave, enabled = state.canAttemptSave) {
                Text(if (state.mode == AppointmentFormMode.CREATE) "Schedule" else "Save appointment")
            }
        }
    }
}

@Composable
private fun AppointmentTextField(
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

@Composable
private fun VisitTypeSelector(selected: VisitType, onVisitTypeChange: (VisitType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Visit type", style = MaterialTheme.typography.caption, color = DarcyColor.TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VisitType.values().forEach { type ->
                TextButton(onClick = { onVisitTypeChange(type) }) {
                    Text(
                        text = if (selected == type) "• ${type.name}" else type.name,
                        color = if (selected == type) DarcyColor.ClinicalAmber else DarcyColor.TextSecondary,
                        fontWeight = if (selected == type) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentBoardRowCard(
    row: AppointmentBoardRow,
    onLoadAppointment: (String) -> Unit,
    onRequestComplete: (String) -> Unit,
    onRequestCancel: (String) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${row.scheduledAt.format(DateTimeFormatter.ofPattern("HH:mm"))} — ${row.patientName}",
                fontWeight = FontWeight.Bold,
                color = DarcyColor.TextPrimary
            )
            MutedText("Owner: ${row.ownerName} (${row.ownerPhoneNumber})")
            MutedText("Reason: ${row.reason}")
            MutedText("Visit type: ${row.visitType}")
            MutedText("Status: ${formatAppointmentStatus(row.status)}")
            row.veterinarianName?.let { MutedText("Veterinarian: $it") }
            if (row.hasPatientAlerts) {
                Text("Patient alert: allergies or medical conditions recorded", color = DarcyColor.SemanticRed)
            }
            if (row.status == AppointmentStatus.SCHEDULED) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { onLoadAppointment(row.id) }) { Text("Edit appointment") }
                    TextButton(onClick = { onRequestComplete(row.id) }) { Text("Complete") }
                    TextButton(onClick = { onRequestCancel(row.id) }) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun PendingAppointmentActionPrompt(
    pending: PendingAppointmentAction,
    onConfirmPendingAction: () -> Unit,
    onDismissPendingAction: () -> Unit
) {
    val label = when (pending.action) {
        AppointmentBoardAction.COMPLETE -> "mark this appointment as completed"
        AppointmentBoardAction.CANCEL -> "cancel this appointment"
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Confirm appointment action", fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
            MutedText("Confirm to $label for ${pending.patientName} at ${pending.scheduledAt}.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onConfirmPendingAction) { Text("Confirm") }
                TextButton(onClick = onDismissPendingAction) { Text("Cancel") }
            }
        }
    }
}

private fun formatAppointmentStatus(status: AppointmentStatus): String =
    status.name.lowercase().replaceFirstChar { it.uppercase() }
