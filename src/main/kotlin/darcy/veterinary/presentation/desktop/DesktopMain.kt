package darcy.veterinary.presentation.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import darcy.veterinary.application.AppointmentBoardRow
import darcy.veterinary.application.ClinicOverviewReport
import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.PetSex
import darcy.veterinary.domain.model.VisitType
import darcy.veterinary.presentation.desktop.theme.DarcyColor
import darcy.veterinary.presentation.desktop.theme.DarcyVetTheme
import darcy.veterinary.presentation.desktop.viewmodel.AppointmentBoardState
import darcy.veterinary.presentation.desktop.viewmodel.AppointmentFormState
import darcy.veterinary.presentation.desktop.viewmodel.DashboardSummaryState
import darcy.veterinary.presentation.desktop.viewmodel.DesktopNavigationState
import darcy.veterinary.presentation.desktop.viewmodel.DesktopSection
import darcy.veterinary.presentation.desktop.viewmodel.OwnerFormState
import darcy.veterinary.presentation.desktop.viewmodel.PatientFormState
import darcy.veterinary.presentation.desktop.viewmodel.PatientSearchState
import java.time.format.DateTimeFormatter

private val LargeGlassShape = RoundedCornerShape(28.dp)
private val MediumGlassShape = RoundedCornerShape(20.dp)
private val SmallGlassShape = RoundedCornerShape(14.dp)

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Darcy Vet",
        state = WindowState(width = 1200.dp, height = 760.dp)
    ) {
        DarcyVetDesktopApp()
    }
}

@Composable
@Preview
fun DarcyVetDesktopApp() {
    val runtime = remember { DesktopRuntimeFactory.sqlite() }
    var navigationState by remember { mutableStateOf(runtime.navigationViewModel.state) }
    var dashboardState by remember { mutableStateOf(runtime.dashboardSummaryViewModel.state) }
    var appointmentBoardState by remember { mutableStateOf(runtime.appointmentBoardViewModel.state) }
    var appointmentFormState by remember { mutableStateOf(runtime.appointmentFormViewModel.state) }
    var patientSearchState by remember { mutableStateOf(runtime.patientSearchViewModel.state) }
    var ownerFormState by remember { mutableStateOf(runtime.ownerFormViewModel.state) }
    var patientFormState by remember { mutableStateOf(runtime.patientFormViewModel.state) }

    fun refreshNavigation() {
        navigationState = runtime.navigationViewModel.state
    }

    fun refreshAppointmentForm() {
        appointmentFormState = runtime.appointmentFormViewModel.state
    }

    fun refreshPatientSearch() {
        patientSearchState = runtime.patientSearchViewModel.state
    }

    fun refreshOwnerForm() {
        ownerFormState = runtime.ownerFormViewModel.state
    }

    fun refreshPatientForm() {
        patientFormState = runtime.patientFormViewModel.state
    }

    fun loadDashboard() {
        runtime.dashboardSummaryViewModel.load()
        dashboardState = runtime.dashboardSummaryViewModel.state
    }

    fun loadAppointmentBoard() {
        runtime.appointmentBoardViewModel.load()
        appointmentBoardState = runtime.appointmentBoardViewModel.state
    }

    fun startAppointment(patientId: String?) {
        runtime.appointmentFormViewModel.startCreate(patientId)
        refreshAppointmentForm()
        runtime.navigationViewModel.scheduleAppointment(patientId)
        refreshNavigation()
    }

    LaunchedEffect(Unit) {
        loadDashboard()
        loadAppointmentBoard()
    }

    DarcyVetTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(DarcyColor.AppBackground),
            color = DarcyColor.AppBackground
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarcyColor.AppBackground)
            ) {
                Sidebar(
                    navigationState = navigationState,
                    onOpenDashboard = {
                        runtime.navigationViewModel.openDashboard()
                        refreshNavigation()
                        loadDashboard()
                    },
                    onOpenOwnersPatients = {
                        runtime.navigationViewModel.openOwnersAndPatients()
                        refreshNavigation()
                    },
                    onOpenAppointments = {
                        runtime.navigationViewModel.openAppointments()
                        refreshNavigation()
                        loadAppointmentBoard()
                    },
                    onOpenMedicalRecords = {
                        runtime.navigationViewModel.openMedicalRecords()
                        refreshNavigation()
                    },
                    onOpenBilling = {
                        runtime.navigationViewModel.openBilling()
                        refreshNavigation()
                    },
                    onOpenReports = {
                        runtime.navigationViewModel.openReports()
                        refreshNavigation()
                    },
                    onOpenAdmin = {
                        runtime.navigationViewModel.openAdmin()
                        refreshNavigation()
                    }
                )
                MainContent(
                    navigationState = navigationState,
                    dashboardState = dashboardState,
                    appointmentBoardState = appointmentBoardState,
                    appointmentFormState = appointmentFormState,
                    patientSearchState = patientSearchState,
                    ownerFormState = ownerFormState,
                    patientFormState = patientFormState,
                    onRefreshDashboard = ::loadDashboard,
                    onRefreshAppointments = ::loadAppointmentBoard,
                    onStartAppointment = ::startAppointment,
                    onLoadAppointment = { appointmentId ->
                        runtime.appointmentFormViewModel.load(appointmentId)
                        refreshAppointmentForm()
                        runtime.navigationViewModel.editAppointment(appointmentId, runtime.appointmentFormViewModel.state.patientId)
                        refreshNavigation()
                    },
                    onAppointmentPatientIdChange = { value ->
                        runtime.appointmentFormViewModel.updatePatientId(value)
                        refreshAppointmentForm()
                    },
                    onAppointmentScheduledAtChange = { value ->
                        runtime.appointmentFormViewModel.updateScheduledAt(value)
                        refreshAppointmentForm()
                    },
                    onAppointmentReasonChange = { value ->
                        runtime.appointmentFormViewModel.updateReason(value)
                        refreshAppointmentForm()
                    },
                    onAppointmentVisitTypeChange = { value ->
                        runtime.appointmentFormViewModel.updateVisitType(value)
                        refreshAppointmentForm()
                    },
                    onAppointmentVeterinarianChange = { value ->
                        runtime.appointmentFormViewModel.updateVeterinarianName(value)
                        refreshAppointmentForm()
                    },
                    onSaveAppointment = {
                        runtime.appointmentFormViewModel.save()
                        refreshAppointmentForm()
                        loadAppointmentBoard()
                    },
                    onSearchQueryChange = { query ->
                        runtime.patientSearchViewModel.updateQuery(query)
                        refreshPatientSearch()
                    },
                    onSearchPatients = {
                        runtime.patientSearchViewModel.search()
                        refreshPatientSearch()
                    },
                    onOpenPatientChart = { patientId, ownerId ->
                        runtime.patientSearchViewModel.openPatientChart(patientId)
                        refreshPatientSearch()
                        runtime.navigationViewModel.openPatientChart(patientId, ownerId)
                        refreshNavigation()
                    },
                    onClearPatientChart = {
                        runtime.patientSearchViewModel.clearSelectedChart()
                        refreshPatientSearch()
                    },
                    onStartOwner = {
                        runtime.ownerFormViewModel.startCreate()
                        refreshOwnerForm()
                        runtime.navigationViewModel.startNewOwner()
                        refreshNavigation()
                    },
                    onStartPatient = { ownerId ->
                        runtime.patientFormViewModel.startCreate(ownerId)
                        refreshPatientForm()
                        runtime.navigationViewModel.startNewPatient(ownerId)
                        refreshNavigation()
                    },
                    onOwnerNameChange = { value ->
                        runtime.ownerFormViewModel.updateFullName(value)
                        refreshOwnerForm()
                    },
                    onOwnerPhoneChange = { value ->
                        runtime.ownerFormViewModel.updatePhoneNumber(value)
                        refreshOwnerForm()
                    },
                    onOwnerEmailChange = { value ->
                        runtime.ownerFormViewModel.updateEmail(value)
                        refreshOwnerForm()
                    },
                    onSaveOwner = {
                        runtime.ownerFormViewModel.save()
                        refreshOwnerForm()
                    },
                    onPatientOwnerIdChange = { value ->
                        runtime.patientFormViewModel.updateOwnerId(value)
                        refreshPatientForm()
                    },
                    onPatientNameChange = { value ->
                        runtime.patientFormViewModel.updateName(value)
                        refreshPatientForm()
                    },
                    onPatientSpeciesChange = { value ->
                        runtime.patientFormViewModel.updateSpecies(value)
                        refreshPatientForm()
                    },
                    onPatientBreedChange = { value ->
                        runtime.patientFormViewModel.updateBreed(value)
                        refreshPatientForm()
                    },
                    onPatientAgeChange = { value ->
                        runtime.patientFormViewModel.updateAge(value)
                        refreshPatientForm()
                    },
                    onPatientSexChange = { value ->
                        runtime.patientFormViewModel.updateSex(value)
                        refreshPatientForm()
                    },
                    onPatientDateOfBirthChange = { value ->
                        runtime.patientFormViewModel.updateDateOfBirth(value)
                        refreshPatientForm()
                    },
                    onPatientWeightChange = { value ->
                        runtime.patientFormViewModel.updateWeightKg(value)
                        refreshPatientForm()
                    },
                    onPatientAllergiesChange = { value ->
                        runtime.patientFormViewModel.updateAllergies(value)
                        refreshPatientForm()
                    },
                    onPatientConditionsChange = { value ->
                        runtime.patientFormViewModel.updateMedicalConditions(value)
                        refreshPatientForm()
                    },
                    onSavePatient = {
                        runtime.patientFormViewModel.save()
                        refreshPatientForm()
                    },
                    onStartInvoice = { patientId ->
                        runtime.navigationViewModel.startInvoice(patientId)
                        refreshNavigation()
                    },
                    onStartMedicalRecord = { patientId ->
                        runtime.navigationViewModel.startMedicalRecord(patientId)
                        refreshNavigation()
                    }
                )
            }
        }
    }
}

@Composable
private fun Sidebar(
    navigationState: DesktopNavigationState,
    onOpenDashboard: () -> Unit,
    onOpenOwnersPatients: () -> Unit,
    onOpenAppointments: () -> Unit,
    onOpenMedicalRecords: () -> Unit,
    onOpenBilling: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(264.dp)
            .fillMaxHeight()
            .background(DarcyColor.GlassSurface, LargeGlassShape)
            .border(BorderStroke(1.dp, DarcyColor.GlassBorder), LargeGlassShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Darcy Vet", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
        Text("Local clinic workstation", style = MaterialTheme.typography.caption, color = DarcyColor.TextMuted)
        Spacer(Modifier.height(18.dp))
        SidebarItem("Dashboard", navigationState.currentSection == DesktopSection.DASHBOARD, onOpenDashboard)
        SidebarItem("Owners & Patients", navigationState.currentSection == DesktopSection.OWNERS_AND_PATIENTS, onOpenOwnersPatients)
        SidebarItem("Appointments", navigationState.currentSection == DesktopSection.APPOINTMENTS, onOpenAppointments)
        SidebarItem("Medical Records", navigationState.currentSection == DesktopSection.MEDICAL_RECORDS, onOpenMedicalRecords)
        SidebarItem("Billing & Checkout", navigationState.currentSection == DesktopSection.BILLING, onOpenBilling)
        SidebarItem("Reports", navigationState.currentSection == DesktopSection.REPORTS, onOpenReports)
        SidebarItem("Admin", navigationState.currentSection == DesktopSection.ADMIN, onOpenAdmin)
    }
}

@Composable
private fun SidebarItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val itemShape = SmallGlassShape
    val background = if (selected) DarcyColor.GlassSurfaceStrong else Color.Transparent
    val border = if (selected) DarcyColor.ClinicalAmber.copy(alpha = 0.62f) else Color.Transparent
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .background(background, itemShape)
            .border(BorderStroke(1.dp, border), itemShape)
    ) {
        Text(
            label,
            color = if (selected) DarcyColor.ClinicalAmber else DarcyColor.TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun MainContent(
    navigationState: DesktopNavigationState,
    dashboardState: DashboardSummaryState,
    appointmentBoardState: AppointmentBoardState,
    appointmentFormState: AppointmentFormState,
    patientSearchState: PatientSearchState,
    ownerFormState: OwnerFormState,
    patientFormState: PatientFormState,
    onRefreshDashboard: () -> Unit,
    onRefreshAppointments: () -> Unit,
    onStartAppointment: (String?) -> Unit,
    onLoadAppointment: (String) -> Unit,
    onAppointmentPatientIdChange: (String) -> Unit,
    onAppointmentScheduledAtChange: (String) -> Unit,
    onAppointmentReasonChange: (String) -> Unit,
    onAppointmentVisitTypeChange: (VisitType) -> Unit,
    onAppointmentVeterinarianChange: (String) -> Unit,
    onSaveAppointment: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchPatients: () -> Unit,
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
    onStartInvoice: (String?) -> Unit,
    onStartMedicalRecord: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(navigationState.title, style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
        Text("Stage 3 desktop shell bound to tested clinic workflow state.", style = MaterialTheme.typography.body2, color = DarcyColor.TextMuted)
        Divider(color = DarcyColor.GlassBorder)
        when (navigationState.currentSection) {
            DesktopSection.DASHBOARD -> DashboardPanel(dashboardState, onRefreshDashboard)
            DesktopSection.OWNERS_AND_PATIENTS -> OwnerPatientWorkspacePanel(
                state = patientSearchState,
                workspaceMode = navigationState.activeWorkspaceMode,
                ownerFormState = ownerFormState,
                patientFormState = patientFormState,
                onQueryChange = onSearchQueryChange,
                onSearch = onSearchPatients,
                onOpenPatientChart = onOpenPatientChart,
                onClearPatientChart = onClearPatientChart,
                onStartOwner = onStartOwner,
                onStartPatient = onStartPatient,
                onOwnerNameChange = onOwnerNameChange,
                onOwnerPhoneChange = onOwnerPhoneChange,
                onOwnerEmailChange = onOwnerEmailChange,
                onSaveOwner = onSaveOwner,
                onPatientOwnerIdChange = onPatientOwnerIdChange,
                onPatientNameChange = onPatientNameChange,
                onPatientSpeciesChange = onPatientSpeciesChange,
                onPatientBreedChange = onPatientBreedChange,
                onPatientAgeChange = onPatientAgeChange,
                onPatientSexChange = onPatientSexChange,
                onPatientDateOfBirthChange = onPatientDateOfBirthChange,
                onPatientWeightChange = onPatientWeightChange,
                onPatientAllergiesChange = onPatientAllergiesChange,
                onPatientConditionsChange = onPatientConditionsChange,
                onSavePatient = onSavePatient,
                onScheduleAppointment = onStartAppointment,
                onStartMedicalRecord = onStartMedicalRecord,
                onStartInvoice = onStartInvoice
            )
            DesktopSection.APPOINTMENTS -> AppointmentWorkspacePanel(
                boardState = appointmentBoardState,
                formState = appointmentFormState,
                onRefresh = onRefreshAppointments,
                onStartCreate = onStartAppointment,
                onLoadAppointment = onLoadAppointment,
                onPatientIdChange = onAppointmentPatientIdChange,
                onScheduledAtChange = onAppointmentScheduledAtChange,
                onReasonChange = onAppointmentReasonChange,
                onVisitTypeChange = onAppointmentVisitTypeChange,
                onVeterinarianChange = onAppointmentVeterinarianChange,
                onSave = onSaveAppointment
            )
            DesktopSection.MEDICAL_RECORDS -> PlaceholderWorkflowPanel(
                title = "Medical Records",
                body = "Create and edit clinical notes using the MedicalRecordFormViewModel.",
                actionLabel = "New medical record",
                onAction = { onStartMedicalRecord(null) }
            )
            DesktopSection.BILLING -> PlaceholderWorkflowPanel(
                title = "Billing & Checkout",
                body = "Create invoices and confirm payment or void actions using BillingCheckoutViewModel.",
                actionLabel = "New invoice",
                onAction = { onStartInvoice(null) }
            )
            DesktopSection.REPORTS -> PlaceholderWorkflowPanel(
                title = "Reports",
                body = "Dashboard metrics are live. Detailed report screens can bind to ClinicReportService next.",
                actionLabel = "Refresh dashboard metrics",
                onAction = onRefreshDashboard
            )
            DesktopSection.ADMIN -> PlaceholderWorkflowPanel(
                title = "Admin",
                body = "Maintenance tools should live here later: health checks, backup, restore, and import/export.",
                actionLabel = "No admin action yet",
                onAction = {}
            )
        }
    }
}

@Composable
private fun DashboardPanel(state: DashboardSummaryState, onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Owners", state.report?.totalOwners?.toString() ?: "—", Modifier.weight(1f))
            MetricCard("Patients", state.report?.totalPets?.toString() ?: "—", Modifier.weight(1f))
            MetricCard("Today", state.report?.todayAppointments?.toString() ?: "—", Modifier.weight(1f))
            MetricCard("Unpaid", state.report?.unpaidInvoices?.toString() ?: "—", Modifier.weight(1f))
        }
        state.report?.let { DashboardReportDetails(it) }
        state.emptyStateMessage?.let { EmptyState(it) }
        state.errorMessage?.let { ErrorState(it) }
        Button(onClick = onRefresh) { Text("Refresh dashboard") }
    }
}

@Composable
private fun DashboardReportDetails(report: ClinicOverviewReport) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Report date: ${report.reportDate}", fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
            MutedText("Total appointments: ${report.totalAppointments}")
            MutedText("Completed appointments: ${report.completedAppointments}")
            MutedText("Cancelled appointments: ${report.cancelledAppointments}")
            MutedText("Paid invoices: ${report.paidInvoices}")
            MutedText("Voided invoices: ${report.voidedInvoices}")
            MutedText("Collected revenue: ${report.paidRevenue}")
        }
    }
}

@Composable
internal fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.caption, color = DarcyColor.TextMuted)
            Text(value, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
        }
    }
}

@Composable
private fun PlaceholderWorkflowPanel(title: String, body: String, actionLabel: String, onAction: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = DarcyColor.TextPrimary)
            MutedText(body)
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
internal fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.border(BorderStroke(1.dp, DarcyColor.GlassBorder), MediumGlassShape),
        shape = MediumGlassShape,
        backgroundColor = DarcyColor.GlassSurface,
        contentColor = DarcyColor.TextPrimary,
        elevation = 10.dp,
        content = content
    )
}

@Composable
internal fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarcyColor.GlassSurfaceSubtle, MediumGlassShape)
            .border(BorderStroke(1.dp, DarcyColor.GlassBorder), MediumGlassShape)
            .padding(18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(message, style = MaterialTheme.typography.body2, color = DarcyColor.TextMuted)
    }
}

@Composable
internal fun ErrorState(message: String) {
    Text(message, color = DarcyColor.SemanticRed, fontWeight = FontWeight.Bold)
}

@Composable
internal fun MutedText(text: String) {
    Text(text, style = MaterialTheme.typography.body2, color = DarcyColor.TextSecondary)
}
