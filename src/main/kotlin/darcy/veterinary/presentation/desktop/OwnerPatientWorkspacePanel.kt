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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import darcy.veterinary.application.AppointmentTimelineRow
import darcy.veterinary.application.InvoiceTimelineRow
import darcy.veterinary.application.OwnerLookupRow
import darcy.veterinary.application.PatientChartViewData
import darcy.veterinary.application.PatientLookupRow
import darcy.veterinary.application.RecordTimelineRow
import darcy.veterinary.domain.model.PetSex
import darcy.veterinary.presentation.desktop.theme.DarcyColor
import darcy.veterinary.presentation.desktop.viewmodel.DesktopWorkspaceMode
import darcy.veterinary.presentation.desktop.viewmodel.OwnerFormState
import darcy.veterinary.presentation.desktop.viewmodel.PatientFormState
import darcy.veterinary.presentation.desktop.viewmodel.PatientSearchState
import java.time.format.DateTimeFormatter

@Composable
internal fun OwnerPatientWorkspacePanel(
    state: PatientSearchState,
    workspaceMode: DesktopWorkspaceMode,
    ownerFormState: OwnerFormState,
    patientFormState: PatientFormState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenPatientChart: (String, String?) -> Unit,
    onClearPatientChart: () -> Unit,
    onStartOwner: () -> Unit,
    onStartPatient: (String?) -> Unit,
    onOwnerNameChange: (String) -> Unit,
    onOwnerPhoneChange: (String) -> Unit,
    onOwnerEmailChange: (String) -> Unit,
    onSaveOwner: () -> Unit,
    onPatientOwnerIdChange: (String) -> Unit,
    onPatientNameChange: (String) -> Unit,
    onPatientSpeciesChange: (String) -> Unit,
    onPatientBreedChange: (String) -> Unit,
    onPatientAgeChange: (String) -> Unit,
    onPatientSexChange: (PetSex?) -> Unit,
    onPatientDateOfBirthChange: (String) -> Unit,
    onPatientWeightChange: (String) -> Unit,
    onPatientAllergiesChange: (String) -> Unit,
    onPatientConditionsChange: (String) -> Unit,
    onSavePatient: () -> Unit,
    onScheduleAppointment: (String?) -> Unit,
    onStartMedicalRecord: (String?) -> Unit,
    onOpenMedicalRecord: (String, String?) -> Unit,
    onStartInvoice: (String?) -> Unit,
    onOpenInvoice: (String, String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                label = { Text("Search owner or patient") },
                singleLine = true
            )
            Button(onClick = onSearch) { Text("Search") }
            Button(onClick = onStartOwner) { Text("New owner") }
            Button(onClick = { onStartPatient(null) }) { Text("New patient") }
        }
        when (workspaceMode) {
            DesktopWorkspaceMode.CREATE_OWNER,
            DesktopWorkspaceMode.EDIT_OWNER -> OwnerProfileFormPanel(
                state = ownerFormState,
                onFullNameChange = onOwnerNameChange,
                onPhoneChange = onOwnerPhoneChange,
                onEmailChange = onOwnerEmailChange,
                onSave = onSaveOwner
            )
            DesktopWorkspaceMode.CREATE_PATIENT -> PatientProfileFormPanel(
                state = patientFormState,
                onOwnerIdChange = onPatientOwnerIdChange,
                onNameChange = onPatientNameChange,
                onSpeciesChange = onPatientSpeciesChange,
                onBreedChange = onPatientBreedChange,
                onAgeChange = onPatientAgeChange,
                onSexChange = onPatientSexChange,
                onDateOfBirthChange = onPatientDateOfBirthChange,
                onWeightChange = onPatientWeightChange,
                onAllergiesChange = onPatientAllergiesChange,
                onConditionsChange = onPatientConditionsChange,
                onSave = onSavePatient
            )
            else -> Unit
        }
        state.validationMessage?.let { ErrorState(it) }
        state.errorMessage?.let { ErrorState(it) }
        state.emptyStateMessage?.let { EmptyState(it) }
        PatientChartPanel(
            chart = state.selectedChart,
            onClearPatientChart = onClearPatientChart,
            onScheduleAppointment = onScheduleAppointment,
            onStartMedicalRecord = onStartMedicalRecord,
            onOpenMedicalRecord = onOpenMedicalRecord,
            onStartInvoice = onStartInvoice,
            onOpenInvoice = onOpenInvoice
        )
        SearchResultsPanel(
            state = state,
            onOpenPatientChart = onOpenPatientChart,
            onStartPatient = onStartPatient
        )
    }
}

@Composable
private fun SearchResultsPanel(
    state: PatientSearchState,
    onOpenPatientChart: (String, String?) -> Unit,
    onStartPatient: (String?) -> Unit
) {
    if (!state.searchResult.hasResults) return

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        GlassCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Owners", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
                if (state.searchResult.owners.isEmpty()) {
                    MutedText("No owner matches in this search.")
                } else {
                    state.searchResult.owners.forEach { owner -> OwnerResultRow(owner, onStartPatient) }
                }
            }
        }
        GlassCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Patients", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
                if (state.searchResult.patients.isEmpty()) {
                    MutedText("No patient matches in this search.")
                } else {
                    state.searchResult.patients.forEach { patient -> PatientResultRow(patient, onOpenPatientChart) }
                }
            }
        }
    }
}

@Composable
private fun OwnerResultRow(owner: OwnerLookupRow, onStartPatient: (String?) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(owner.fullName, fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
            MutedText("Phone: ${owner.phoneNumber}")
            owner.email?.let { MutedText("Email: $it") }
            MutedText("Patients: ${owner.patientCount}")
            TextButton(onClick = { onStartPatient(owner.id) }) { Text("Add patient") }
        }
    }
}

@Composable
private fun PatientResultRow(patient: PatientLookupRow, onOpenPatientChart: (String, String?) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(patient.name, fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
            MutedText("${patient.species}${patient.breed?.let { " • $it" } ?: ""}")
            MutedText("Owner: ${patient.ownerName}")
            if (patient.hasAlerts) {
                Text("Patient alert recorded", color = DarcyColor.SemanticRed)
            }
            TextButton(onClick = { onOpenPatientChart(patient.id, patient.ownerId) }) { Text("Open chart") }
        }
    }
}

@Composable
private fun PatientChartPanel(
    chart: PatientChartViewData?,
    onClearPatientChart: () -> Unit,
    onScheduleAppointment: (String?) -> Unit,
    onStartMedicalRecord: (String?) -> Unit,
    onOpenMedicalRecord: (String, String?) -> Unit,
    onStartInvoice: (String?) -> Unit,
    onOpenInvoice: (String, String?) -> Unit
) {
    if (chart == null) return

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(chart.patient.name, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
                    MutedText("${chart.patient.species}${chart.patient.breed?.let { " • $it" } ?: ""}")
                    MutedText("Owner: ${chart.owner.fullName} (${chart.owner.phoneNumber})")
                    chart.owner.email?.let { MutedText("Email: $it") }
                }
                TextButton(onClick = onClearPatientChart) { Text("Close chart") }
            }
            PatientVitals(chart)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onScheduleAppointment(chart.patient.id) }) { Text("Schedule appointment") }
                Button(onClick = { onStartMedicalRecord(chart.patient.id) }) { Text("New record") }
                Button(onClick = { onStartInvoice(chart.patient.id) }) { Text("New invoice") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TimelinePanel("Appointments", chart.appointments, Modifier.weight(1f)) { AppointmentTimelineItem(it) }
                TimelinePanel("Records", chart.records, Modifier.weight(1f)) { record ->
                    RecordTimelineItem(record) { onOpenMedicalRecord(record.id, chart.patient.id) }
                }
                TimelinePanel("Invoices", chart.invoices, Modifier.weight(1f)) { invoice ->
                    InvoiceTimelineItem(invoice) { onOpenInvoice(invoice.id, chart.patient.id) }
                }
            }
        }
    }
}

@Composable
private fun PatientVitals(chart: PatientChartViewData) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        MetricCard("Age", chart.patient.age?.toString() ?: "—", Modifier.weight(1f))
        MetricCard("Sex", chart.patient.sex?.name ?: "—", Modifier.weight(1f))
        MetricCard("Weight", chart.patient.weightKg?.let { "$it kg" } ?: "—", Modifier.weight(1f))
        MetricCard("Alerts", (chart.patient.allergies.size + chart.patient.medicalConditions.size).toString(), Modifier.weight(1f))
    }
    if (chart.patient.allergies.isNotEmpty()) {
        Text("Allergies: ${chart.patient.allergies.joinToString()}", color = DarcyColor.SemanticRed)
    }
    if (chart.patient.medicalConditions.isNotEmpty()) {
        MutedText("Conditions: ${chart.patient.medicalConditions.joinToString()}")
    }
}

@Composable
private fun <T> TimelinePanel(title: String, items: List<T>, modifier: Modifier = Modifier, itemContent: @Composable (T) -> Unit) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
            if (items.isEmpty()) {
                MutedText("No entries yet.")
            } else {
                items.take(5).forEach { item -> itemContent(item) }
            }
        }
    }
}

@Composable
private fun AppointmentTimelineItem(row: AppointmentTimelineRow) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(row.scheduledAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
        MutedText("${row.visitType} • ${row.reason}")
        MutedText("Status: ${row.status}")
    }
}

@Composable
private fun RecordTimelineItem(row: RecordTimelineRow, onOpenMedicalRecord: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(row.recordedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
        MutedText("Diagnosis: ${row.diagnosis}")
        MutedText("Treatment: ${row.treatment}")
        TextButton(onClick = onOpenMedicalRecord) { Text("Open record") }
    }
}

@Composable
private fun InvoiceTimelineItem(row: InvoiceTimelineRow, onOpenInvoice: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(row.issuedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
        MutedText("${row.paymentStatus} • ${row.total}")
        TextButton(onClick = onOpenInvoice) { Text("Open invoice") }
    }
}
