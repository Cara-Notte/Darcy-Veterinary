package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.BillingService
import darcy.veterinary.application.ClinicReportService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class DashboardSummaryViewModelTest {
    private fun fixture(initialDate: LocalDate = LocalDate.of(2026, 6, 23)): Fixture {
        val ids = SequenceIdGenerator()
        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val appointmentRepository = InMemoryAppointmentRepository()
        val invoiceRepository = InMemoryInvoiceRepository()
        val ownerService = OwnerService(ownerRepository, ids)
        val patientService = PatientService(petRepository, ownerRepository, ids)
        val appointmentService = AppointmentService(appointmentRepository, petRepository, ids)
        val billingService = BillingService(invoiceRepository, petRepository, ids)
        val reportService = ClinicReportService(
            ownerRepository,
            petRepository,
            appointmentRepository,
            invoiceRepository
        )

        return Fixture(
            ownerService = ownerService,
            patientService = patientService,
            appointmentService = appointmentService,
            billingService = billingService,
            viewModel = DashboardSummaryViewModel(reportService, initialDate)
        )
    }

    @Test
    fun `initial state prompts dashboard summary load`() {
        val app = fixture()

        assertEquals("Load the dashboard summary to view clinic activity.", app.viewModel.state.emptyStateMessage)
        assertFalse(app.viewModel.state.hasTodaySchedule)
        assertFalse(app.viewModel.state.hasOpenBillingWork)
        assertEquals(0.0, app.viewModel.state.collectedRevenue)
    }

    @Test
    fun `load populates dashboard summary metrics`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        val patient = app.patientService.registerPet(owner.id, "Darcy", "Dog")
        val completed = app.appointmentService.scheduleAppointment(
            petId = patient.id,
            scheduledAt = LocalDateTime.of(2026, 6, 23, 9, 30),
            reason = "Vaccination"
        )
        app.appointmentService.completeAppointment(completed.id)
        app.appointmentService.scheduleAppointment(
            petId = patient.id,
            scheduledAt = LocalDateTime.of(2026, 6, 24, 10, 0),
            reason = "Follow up"
        )
        val paidInvoice = app.billingService.createInvoice(
            petId = patient.id,
            services = listOf(ClinicService.CONSULTATION, ClinicService.VACCINATION)
        )
        app.billingService.markAsPaid(paidInvoice.id)
        app.billingService.createInvoice(patient.id, listOf(ClinicService.GROOMING))

        app.viewModel.load()

        val report = app.viewModel.state.report
        assertNull(app.viewModel.state.errorMessage)
        assertEquals(LocalDate.of(2026, 6, 23), app.viewModel.state.reportDate)
        assertEquals(1, report?.totalOwners)
        assertEquals(1, report?.totalPets)
        assertEquals(2, report?.totalAppointments)
        assertEquals(1, report?.todayAppointments)
        assertEquals(1, report?.completedAppointments)
        assertEquals(1, report?.unpaidInvoices)
        assertEquals(1, report?.paidInvoices)
        assertEquals(350_000.0, app.viewModel.state.collectedRevenue)
        assertTrue(app.viewModel.state.hasTodaySchedule)
        assertTrue(app.viewModel.state.hasOpenBillingWork)
        assertNull(app.viewModel.state.emptyStateMessage)
    }

    @Test
    fun `select date reloads summary for that report date`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Nadia Prasetyo", "0811111111")
        val patient = app.patientService.registerPet(owner.id, "Miso", "Cat")
        app.appointmentService.scheduleAppointment(
            petId = patient.id,
            scheduledAt = LocalDateTime.of(2026, 6, 24, 10, 0),
            reason = "Skin check"
        )

        app.viewModel.selectDate(LocalDate.of(2026, 6, 24))

        assertEquals(LocalDate.of(2026, 6, 24), app.viewModel.state.reportDate)
        assertEquals(1, app.viewModel.state.report?.todayAppointments)
        assertTrue(app.viewModel.state.hasTodaySchedule)
    }

    @Test
    fun `load empty clinic shows no activity message`() {
        val app = fixture()

        app.viewModel.load()

        assertEquals("No clinic activity has been recorded yet.", app.viewModel.state.emptyStateMessage)
        assertFalse(app.viewModel.state.hasTodaySchedule)
        assertFalse(app.viewModel.state.hasOpenBillingWork)
    }

    private data class Fixture(
        val ownerService: OwnerService,
        val patientService: PatientService,
        val appointmentService: AppointmentService,
        val billingService: BillingService,
        val viewModel: DashboardSummaryViewModel
    )
}
