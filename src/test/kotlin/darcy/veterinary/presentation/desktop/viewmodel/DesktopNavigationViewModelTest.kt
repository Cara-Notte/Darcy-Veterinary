package darcy.veterinary.presentation.desktop.viewmodel

import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class DesktopNavigationViewModelTest {
    @Test
    fun `initial state opens dashboard`() {
        val viewModel = DesktopNavigationViewModel()

        assertEquals(DesktopSection.DASHBOARD, viewModel.state.currentSection)
        assertEquals("Dashboard", viewModel.state.title)
        assertFalse(viewModel.state.hasPatientContext)
    }

    @Test
    fun `owner and patient workspace transitions keep the intended context`() {
        val viewModel = DesktopNavigationViewModel()

        viewModel.startNewOwner()
        assertEquals(DesktopSection.OWNERS_AND_PATIENTS, viewModel.state.currentSection)
        assertEquals(DesktopWorkspaceMode.CREATE_OWNER, viewModel.state.activeWorkspaceMode)
        assertNull(viewModel.state.selectedOwnerId)
        assertNull(viewModel.state.selectedPatientId)

        viewModel.editOwner("OWN-0001")
        assertEquals(DesktopWorkspaceMode.EDIT_OWNER, viewModel.state.activeWorkspaceMode)
        assertEquals("OWN-0001", viewModel.state.selectedOwnerId)
        assertNull(viewModel.state.selectedPatientId)

        viewModel.startNewPatient("OWN-0001")
        assertEquals(DesktopWorkspaceMode.CREATE_PATIENT, viewModel.state.activeWorkspaceMode)
        assertEquals("OWN-0001", viewModel.state.selectedOwnerId)
        assertNull(viewModel.state.selectedPatientId)

        viewModel.openPatientChart("PET-0001", "OWN-0001")
        assertEquals(DesktopWorkspaceMode.PATIENT_CHART, viewModel.state.activeWorkspaceMode)
        assertEquals("OWN-0001", viewModel.state.selectedOwnerId)
        assertEquals("PET-0001", viewModel.state.selectedPatientId)
        assertTrue(viewModel.state.hasPatientContext)
    }

    @Test
    fun `appointment transitions preserve selected patient context`() {
        val viewModel = DesktopNavigationViewModel()
        viewModel.openPatientChart("PET-0001")

        viewModel.openAppointments(LocalDate.of(2026, 6, 23))
        assertEquals(DesktopSection.APPOINTMENTS, viewModel.state.currentSection)
        assertEquals(LocalDate.of(2026, 6, 23), viewModel.state.selectedAppointmentDate)
        assertEquals("PET-0001", viewModel.state.selectedPatientId)

        viewModel.scheduleAppointment()
        assertEquals(DesktopAppointmentMode.CREATE, viewModel.state.activeAppointmentMode)
        assertEquals("PET-0001", viewModel.state.selectedPatientId)
        assertNull(viewModel.state.selectedAppointmentId)

        viewModel.editAppointment("APT-0001")
        assertEquals(DesktopAppointmentMode.EDIT, viewModel.state.activeAppointmentMode)
        assertEquals("APT-0001", viewModel.state.selectedAppointmentId)
        assertEquals("PET-0001", viewModel.state.selectedPatientId)
    }

    @Test
    fun `medical record transitions preserve appointment and patient context`() {
        val viewModel = DesktopNavigationViewModel()
        viewModel.editAppointment("APT-0001", "PET-0001")

        viewModel.startMedicalRecord()
        assertEquals(DesktopSection.MEDICAL_RECORDS, viewModel.state.currentSection)
        assertEquals(DesktopMedicalRecordMode.CREATE, viewModel.state.activeMedicalRecordMode)
        assertEquals("PET-0001", viewModel.state.selectedPatientId)
        assertEquals("APT-0001", viewModel.state.selectedAppointmentId)
        assertNull(viewModel.state.selectedRecordId)

        viewModel.editMedicalRecord("REC-0001")
        assertEquals(DesktopMedicalRecordMode.EDIT, viewModel.state.activeMedicalRecordMode)
        assertEquals("REC-0001", viewModel.state.selectedRecordId)
        assertEquals("PET-0001", viewModel.state.selectedPatientId)
    }

    @Test
    fun `billing transitions preserve patient context and invoice selection`() {
        val viewModel = DesktopNavigationViewModel()
        viewModel.openPatientChart("PET-0001")

        viewModel.startInvoice()
        assertEquals(DesktopSection.BILLING, viewModel.state.currentSection)
        assertEquals(DesktopBillingMode.CREATE, viewModel.state.activeBillingMode)
        assertEquals("PET-0001", viewModel.state.selectedPatientId)
        assertNull(viewModel.state.selectedInvoiceId)

        viewModel.openInvoice("INV-0001")
        assertEquals(DesktopBillingMode.VIEW, viewModel.state.activeBillingMode)
        assertEquals("INV-0001", viewModel.state.selectedInvoiceId)
        assertEquals("PET-0001", viewModel.state.selectedPatientId)
    }

    @Test
    fun `top level navigation switches sections without dropping selected context`() {
        val viewModel = DesktopNavigationViewModel()
        viewModel.openPatientChart("PET-0001")

        viewModel.openReports()
        assertEquals(DesktopSection.REPORTS, viewModel.state.currentSection)
        assertEquals("PET-0001", viewModel.state.selectedPatientId)

        viewModel.openAdmin()
        assertEquals(DesktopSection.ADMIN, viewModel.state.currentSection)
        assertEquals("PET-0001", viewModel.state.selectedPatientId)

        viewModel.openDashboard()
        assertEquals(DesktopSection.DASHBOARD, viewModel.state.currentSection)
        assertNull(viewModel.state.selectedPatientId)
    }
}
