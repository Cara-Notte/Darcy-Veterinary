package darcy.veterinary.presentation.desktop.viewmodel

import java.time.LocalDate

class DesktopNavigationViewModel(
    initialSection: DesktopSection = DesktopSection.DASHBOARD
) {
    var state: DesktopNavigationState = DesktopNavigationState(currentSection = initialSection)
        private set

    fun openDashboard() {
        state = DesktopNavigationState(currentSection = DesktopSection.DASHBOARD)
    }

    fun openOwnersAndPatients() {
        state = state.copy(currentSection = DesktopSection.OWNERS_AND_PATIENTS)
    }

    fun startNewOwner() {
        state = state.copy(
            currentSection = DesktopSection.OWNERS_AND_PATIENTS,
            activeWorkspaceMode = DesktopWorkspaceMode.CREATE_OWNER,
            selectedOwnerId = null,
            selectedPatientId = null
        )
    }

    fun editOwner(ownerId: String) {
        state = state.copy(
            currentSection = DesktopSection.OWNERS_AND_PATIENTS,
            activeWorkspaceMode = DesktopWorkspaceMode.EDIT_OWNER,
            selectedOwnerId = ownerId,
            selectedPatientId = null
        )
    }

    fun startNewPatient(ownerId: String? = null) {
        state = state.copy(
            currentSection = DesktopSection.OWNERS_AND_PATIENTS,
            activeWorkspaceMode = DesktopWorkspaceMode.CREATE_PATIENT,
            selectedOwnerId = ownerId,
            selectedPatientId = null
        )
    }

    fun openPatientChart(patientId: String, ownerId: String? = null) {
        state = state.copy(
            currentSection = DesktopSection.OWNERS_AND_PATIENTS,
            activeWorkspaceMode = DesktopWorkspaceMode.PATIENT_CHART,
            selectedOwnerId = ownerId ?: state.selectedOwnerId,
            selectedPatientId = patientId
        )
    }

    fun openAppointments(date: LocalDate? = null) {
        state = state.copy(
            currentSection = DesktopSection.APPOINTMENTS,
            selectedAppointmentDate = date ?: state.selectedAppointmentDate
        )
    }

    fun scheduleAppointment(patientId: String? = null) {
        state = state.copy(
            currentSection = DesktopSection.APPOINTMENTS,
            activeAppointmentMode = DesktopAppointmentMode.CREATE,
            selectedPatientId = patientId ?: state.selectedPatientId,
            selectedAppointmentId = null
        )
    }

    fun editAppointment(appointmentId: String, patientId: String? = null) {
        state = state.copy(
            currentSection = DesktopSection.APPOINTMENTS,
            activeAppointmentMode = DesktopAppointmentMode.EDIT,
            selectedAppointmentId = appointmentId,
            selectedPatientId = patientId ?: state.selectedPatientId
        )
    }

    fun openMedicalRecords(patientId: String? = null) {
        state = state.copy(
            currentSection = DesktopSection.MEDICAL_RECORDS,
            selectedPatientId = patientId ?: state.selectedPatientId
        )
    }

    fun startMedicalRecord(patientId: String? = null, appointmentId: String? = null) {
        state = state.copy(
            currentSection = DesktopSection.MEDICAL_RECORDS,
            activeMedicalRecordMode = DesktopMedicalRecordMode.CREATE,
            selectedPatientId = patientId ?: state.selectedPatientId,
            selectedAppointmentId = appointmentId ?: state.selectedAppointmentId,
            selectedRecordId = null
        )
    }

    fun editMedicalRecord(recordId: String, patientId: String? = null) {
        state = state.copy(
            currentSection = DesktopSection.MEDICAL_RECORDS,
            activeMedicalRecordMode = DesktopMedicalRecordMode.EDIT,
            selectedRecordId = recordId,
            selectedPatientId = patientId ?: state.selectedPatientId
        )
    }

    fun openBilling(patientId: String? = null) {
        state = state.copy(
            currentSection = DesktopSection.BILLING,
            selectedPatientId = patientId ?: state.selectedPatientId
        )
    }

    fun startInvoice(patientId: String? = null) {
        state = state.copy(
            currentSection = DesktopSection.BILLING,
            activeBillingMode = DesktopBillingMode.CREATE,
            selectedPatientId = patientId ?: state.selectedPatientId,
            selectedInvoiceId = null
        )
    }

    fun openInvoice(invoiceId: String, patientId: String? = null) {
        state = state.copy(
            currentSection = DesktopSection.BILLING,
            activeBillingMode = DesktopBillingMode.VIEW,
            selectedInvoiceId = invoiceId,
            selectedPatientId = patientId ?: state.selectedPatientId
        )
    }

    fun openReports() {
        state = state.copy(currentSection = DesktopSection.REPORTS)
    }

    fun openAdmin() {
        state = state.copy(currentSection = DesktopSection.ADMIN)
    }
}

enum class DesktopSection(val title: String) {
    DASHBOARD("Dashboard"),
    OWNERS_AND_PATIENTS("Owners & Patients"),
    APPOINTMENTS("Appointments"),
    MEDICAL_RECORDS("Medical Records"),
    BILLING("Billing & Checkout"),
    REPORTS("Reports"),
    ADMIN("Admin")
}

enum class DesktopWorkspaceMode {
    SEARCH,
    CREATE_OWNER,
    EDIT_OWNER,
    CREATE_PATIENT,
    PATIENT_CHART
}

enum class DesktopAppointmentMode {
    BOARD,
    CREATE,
    EDIT
}

enum class DesktopMedicalRecordMode {
    LIST,
    CREATE,
    EDIT
}

enum class DesktopBillingMode {
    LIST,
    CREATE,
    VIEW
}

data class DesktopNavigationState(
    val currentSection: DesktopSection,
    val activeWorkspaceMode: DesktopWorkspaceMode = DesktopWorkspaceMode.SEARCH,
    val activeAppointmentMode: DesktopAppointmentMode = DesktopAppointmentMode.BOARD,
    val activeMedicalRecordMode: DesktopMedicalRecordMode = DesktopMedicalRecordMode.LIST,
    val activeBillingMode: DesktopBillingMode = DesktopBillingMode.LIST,
    val selectedOwnerId: String? = null,
    val selectedPatientId: String? = null,
    val selectedAppointmentId: String? = null,
    val selectedAppointmentDate: LocalDate? = null,
    val selectedRecordId: String? = null,
    val selectedInvoiceId: String? = null
) {
    val title: String = currentSection.title
    val hasPatientContext: Boolean = selectedPatientId != null
}
