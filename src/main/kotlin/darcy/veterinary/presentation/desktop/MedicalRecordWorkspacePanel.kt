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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import darcy.veterinary.presentation.desktop.theme.DarcyColor
import darcy.veterinary.presentation.desktop.viewmodel.DesktopMedicalRecordMode
import darcy.veterinary.presentation.desktop.viewmodel.MedicalRecordFormField
import darcy.veterinary.presentation.desktop.viewmodel.MedicalRecordFormMode
import darcy.veterinary.presentation.desktop.viewmodel.MedicalRecordFormState

@Composable
internal fun MedicalRecordWorkspacePanel(
    mode: DesktopMedicalRecordMode,
    state: MedicalRecordFormState,
    onStartCreate: () -> Unit,
    onPatientIdChange: (String) -> Unit,
    onAppointmentIdChange: (String) -> Unit,
    onDiagnosisChange: (String) -> Unit,
    onTreatmentChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onRecordedAtChange: (String) -> Unit,
    onVeterinarianChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onStartCreate) { Text("New medical record") }
        }
        when (mode) {
            DesktopMedicalRecordMode.CREATE,
            DesktopMedicalRecordMode.EDIT -> MedicalRecordFormPanel(
                state = state,
                onPatientIdChange = onPatientIdChange,
                onAppointmentIdChange = onAppointmentIdChange,
                onDiagnosisChange = onDiagnosisChange,
                onTreatmentChange = onTreatmentChange,
                onNotesChange = onNotesChange,
                onRecordedAtChange = onRecordedAtChange,
                onVeterinarianChange = onVeterinarianChange,
                onSave = onSave
            )
            DesktopMedicalRecordMode.LIST -> EmptyState(
                "Start a new medical record, or open a patient chart and create a record from patient context."
            )
        }
    }
}

@Composable
private fun MedicalRecordFormPanel(
    state: MedicalRecordFormState,
    onPatientIdChange: (String) -> Unit,
    onAppointmentIdChange: (String) -> Unit,
    onDiagnosisChange: (String) -> Unit,
    onTreatmentChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onRecordedAtChange: (String) -> Unit,
    onVeterinarianChange: (String) -> Unit,
    onSave: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = when (state.mode) {
                    MedicalRecordFormMode.CREATE -> "New medical record"
                    MedicalRecordFormMode.EDIT -> "Edit medical record"
                },
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = DarcyColor.TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MedicalRecordTextField(
                    value = state.patientId,
                    onValueChange = onPatientIdChange,
                    label = "Patient ID",
                    error = state.fieldErrors[MedicalRecordFormField.PATIENT_ID],
                    modifier = Modifier.weight(1f)
                )
                MedicalRecordTextField(
                    value = state.appointmentId,
                    onValueChange = onAppointmentIdChange,
                    label = "Appointment ID, optional",
                    modifier = Modifier.weight(1f)
                )
            }
            MedicalRecordTextField(
                value = state.diagnosis,
                onValueChange = onDiagnosisChange,
                label = "Diagnosis",
                error = state.fieldErrors[MedicalRecordFormField.DIAGNOSIS]
            )
            MedicalRecordTextField(
                value = state.treatment,
                onValueChange = onTreatmentChange,
                label = "Treatment",
                error = state.fieldErrors[MedicalRecordFormField.TREATMENT]
            )
            MedicalRecordTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                label = "Clinical notes",
                singleLine = false
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MedicalRecordTextField(
                    value = state.recordedAt,
                    onValueChange = onRecordedAtChange,
                    label = "Recorded at, YYYY-MM-DDTHH:MM optional",
                    error = state.fieldErrors[MedicalRecordFormField.RECORDED_AT],
                    modifier = Modifier.weight(1f)
                )
                MedicalRecordTextField(
                    value = state.veterinarianName,
                    onValueChange = onVeterinarianChange,
                    label = "Veterinarian",
                    modifier = Modifier.weight(1f)
                )
            }
            state.errorMessage?.let { ErrorState(it) }
            state.successMessage?.let { Text(it, color = DarcyColor.ClinicalAmber, fontWeight = FontWeight.Bold) }
            Button(onClick = onSave, enabled = state.canAttemptSave) {
                Text(if (state.mode == MedicalRecordFormMode.CREATE) "Create record" else "Save record")
            }
        }
    }
}

@Composable
private fun MedicalRecordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null,
    singleLine: Boolean = true
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            isError = error != null,
            singleLine = singleLine
        )
        error?.let { Text(it, color = DarcyColor.SemanticRed, style = MaterialTheme.typography.caption) }
    }
}
