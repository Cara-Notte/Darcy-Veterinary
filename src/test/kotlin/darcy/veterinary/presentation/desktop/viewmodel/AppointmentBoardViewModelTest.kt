package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.AppointmentBoardFacade
import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class AppointmentBoardViewModelTest {
    private fun fixture(initialDate: LocalDate = LocalDate.of(2026, 6, 23)): Fixture {
        val ids = SequenceIdGenerator()
        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val appointmentRepository = InMemoryAppointmentRepository()
        val ownerService = OwnerService(ownerRepository, ids)
        val patientService = PatientService(petRepository, ownerRepository, ids)
        val appointmentService = AppointmentService(appointmentRepository, petRepository, ids)
        val facade = AppointmentBoardFacade(ownerService, patientService, appointmentService)

        return Fixture(
            ownerService = ownerService,
            patientService = patientService,
            appointmentService = appointmentService,
            viewModel = AppointmentBoardViewModel(facade, initialDate)
        )
    }

    @Test
    fun `load populates daily appointment board state`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        val pet = app.patientService.registerPet(owner.id, "Darcy", "Dog")
        app.appointmentService.scheduleAppointment(
            petId = pet.id,
            scheduledAt = LocalDateTime.of(2026, 6, 23, 9, 30),
            reason = "Vaccination"
        )

        app.viewModel.load()

        assertFalse(app.viewModel.state.isLoading)
        assertNull(app.viewModel.state.errorMessage)
        assertTrue(app.viewModel.state.board?.hasAppointments == true)
        assertEquals("Darcy", app.viewModel.state.board?.rows?.single()?.patientName)
        assertNull(app.viewModel.state.emptyStateMessage)
    }

    @Test
    fun `status filter reloads board without losing selected date`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Nadia Prasetyo", "0811111111")
        val pet = app.patientService.registerPet(owner.id, "Miso", "Cat")
        val completed = app.appointmentService.scheduleAppointment(
            petId = pet.id,
            scheduledAt = LocalDateTime.of(2026, 6, 23, 9, 0),
            reason = "Completed visit"
        )
        app.appointmentService.completeAppointment(completed.id)
        app.appointmentService.scheduleAppointment(
            petId = pet.id,
            scheduledAt = LocalDateTime.of(2026, 6, 23, 10, 0),
            reason = "Upcoming visit"
        )

        app.viewModel.applyStatusFilter(AppointmentStatus.SCHEDULED)

        assertEquals(LocalDate.of(2026, 6, 23), app.viewModel.state.selectedDate)
        assertEquals(AppointmentStatus.SCHEDULED, app.viewModel.state.statusFilter)
        assertEquals(1, app.viewModel.state.board?.rows?.size)
        assertEquals("Upcoming visit", app.viewModel.state.board?.rows?.single()?.reason)
        assertEquals(2, app.viewModel.state.board?.summary?.totalCount)
    }

    @Test
    fun `empty state distinguishes no appointments from no filtered matches`() {
        val app = fixture()
        app.viewModel.load()

        assertEquals("No appointments scheduled for this date.", app.viewModel.state.emptyStateMessage)

        app.viewModel.applyStatusFilter(AppointmentStatus.CANCELLED)

        assertEquals("No appointments match the selected status for this date.", app.viewModel.state.emptyStateMessage)
    }

    @Test
    fun `cancel appointment requires pending confirmation before mutation`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Lia Santoso", "0822222222")
        val pet = app.patientService.registerPet(owner.id, "Bento", "Dog")
        val appointment = app.appointmentService.scheduleAppointment(
            petId = pet.id,
            scheduledAt = LocalDateTime.of(2026, 6, 23, 13, 0),
            reason = "Dental check"
        )
        app.viewModel.load()

        app.viewModel.requestCancelAppointment(appointment.id)

        assertNotNull(app.viewModel.state.pendingAction)
        assertEquals(AppointmentBoardAction.CANCEL, app.viewModel.state.pendingAction?.action)
        assertEquals(AppointmentStatus.SCHEDULED, app.appointmentService.getAppointment(appointment.id).status)

        app.viewModel.confirmPendingAction()

        assertNull(app.viewModel.state.pendingAction)
        assertEquals("Appointment cancelled.", app.viewModel.state.successMessage)
        assertEquals(AppointmentStatus.CANCELLED, app.appointmentService.getAppointment(appointment.id).status)
        assertEquals(1, app.viewModel.state.board?.summary?.cancelledCount)
    }

    @Test
    fun `complete appointment reloads board and reports success after confirmation`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        val pet = app.patientService.registerPet(owner.id, "Darcy", "Dog")
        val appointment = app.appointmentService.scheduleAppointment(
            petId = pet.id,
            scheduledAt = LocalDateTime.of(2026, 6, 23, 9, 30),
            reason = "Vaccination"
        )
        app.viewModel.load()

        app.viewModel.requestCompleteAppointment(appointment.id)
        app.viewModel.confirmPendingAction()

        assertEquals("Appointment marked as completed.", app.viewModel.state.successMessage)
        assertEquals(AppointmentStatus.COMPLETED, app.appointmentService.getAppointment(appointment.id).status)
        assertEquals(1, app.viewModel.state.board?.summary?.completedCount)
    }

    @Test
    fun `non scheduled appointment actions are rejected before confirmation`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Nadia Prasetyo", "0811111111")
        val pet = app.patientService.registerPet(owner.id, "Miso", "Cat")
        val appointment = app.appointmentService.scheduleAppointment(
            petId = pet.id,
            scheduledAt = LocalDateTime.of(2026, 6, 23, 9, 0),
            reason = "Completed visit"
        )
        app.appointmentService.completeAppointment(appointment.id)
        app.viewModel.load()

        app.viewModel.requestCancelAppointment(appointment.id)

        assertNull(app.viewModel.state.pendingAction)
        assertEquals("Only scheduled appointments can be updated.", app.viewModel.state.errorMessage)
    }

    private data class Fixture(
        val ownerService: OwnerService,
        val patientService: PatientService,
        val appointmentService: AppointmentService,
        val viewModel: AppointmentBoardViewModel
    )
}
