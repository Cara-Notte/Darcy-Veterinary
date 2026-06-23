package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.VisitType
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class AppointmentFormViewModelTest {
    private fun fixture(): Fixture {
        val ids = SequenceIdGenerator()
        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val appointmentRepository = InMemoryAppointmentRepository()
        val ownerService = OwnerService(ownerRepository, ids)
        val patientService = PatientService(petRepository, ownerRepository, ids)
        val appointmentService = AppointmentService(appointmentRepository, petRepository, ids)
        return Fixture(
            ownerService = ownerService,
            patientService = patientService,
            appointmentService = appointmentService,
            viewModel = AppointmentFormViewModel(appointmentService)
        )
    }

    @Test
    fun `blank appointment fields produce inline validation errors without saving`() {
        val app = fixture()

        app.viewModel.save()

        assertEquals("Patient is required.", app.viewModel.state.fieldErrors[AppointmentFormField.PATIENT_ID])
        assertEquals("Scheduled date and time are required.", app.viewModel.state.fieldErrors[AppointmentFormField.SCHEDULED_AT])
        assertEquals("Appointment reason is required.", app.viewModel.state.fieldErrors[AppointmentFormField.REASON])
        assertNull(app.viewModel.state.savedAppointmentId)
        assertTrue(app.appointmentService.listAppointments().isEmpty())
    }

    @Test
    fun `invalid date time produces inline validation error`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        val patient = app.patientService.registerPet(owner.id, "Darcy", "Dog")
        app.viewModel.startCreate(patient.id)
        app.viewModel.updateScheduledAt("2026/06/23 09:30")
        app.viewModel.updateReason("Vaccination")

        app.viewModel.save()

        assertEquals(
            "Scheduled date and time must use YYYY-MM-DDTHH:MM.",
            app.viewModel.state.fieldErrors[AppointmentFormField.SCHEDULED_AT]
        )
        assertTrue(app.appointmentService.listAppointments().isEmpty())
    }

    @Test
    fun `create mode schedules appointment and switches to edit mode`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        val patient = app.patientService.registerPet(owner.id, "Darcy", "Dog")
        app.viewModel.startCreate(patient.id)
        app.viewModel.updateScheduledAt("2026-06-23T09:30")
        app.viewModel.updateReason("  Vaccination  ")
        app.viewModel.updateVisitType(VisitType.VACCINATION)
        app.viewModel.updateVeterinarianName(" Dr. Sari ")

        app.viewModel.save()

        val appointment = app.appointmentService.getAppointment("APT-0001")
        assertFalse(app.viewModel.state.isSaving)
        assertEquals(AppointmentFormMode.EDIT, app.viewModel.state.mode)
        assertEquals("APT-0001", app.viewModel.state.savedAppointmentId)
        assertEquals("Appointment scheduled.", app.viewModel.state.successMessage)
        assertEquals(patient.id, appointment.petId)
        assertEquals(LocalDateTime.of(2026, 6, 23, 9, 30), appointment.scheduledAt)
        assertEquals("Vaccination", appointment.reason)
        assertEquals(VisitType.VACCINATION, appointment.visitType)
        assertEquals("Dr. Sari", appointment.veterinarianName)
    }

    @Test
    fun `load and reschedule existing appointment`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Nadia Prasetyo", "0811111111")
        val patient = app.patientService.registerPet(owner.id, "Miso", "Cat")
        val appointment = app.appointmentService.scheduleAppointment(
            petId = patient.id,
            scheduledAt = LocalDateTime.of(2026, 6, 23, 10, 0),
            reason = "Skin check",
            visitType = VisitType.GENERAL,
            veterinarianName = "Dr. Bima"
        )

        app.viewModel.load(appointment.id)
        app.viewModel.updateScheduledAt("2026-06-23T11:30")
        app.viewModel.updateReason("Skin follow up")
        app.viewModel.updateVisitType(VisitType.FOLLOW_UP)
        app.viewModel.save()

        val updated = app.appointmentService.getAppointment(appointment.id)
        assertEquals(AppointmentFormMode.EDIT, app.viewModel.state.mode)
        assertEquals("Appointment updated.", app.viewModel.state.successMessage)
        assertEquals(LocalDateTime.of(2026, 6, 23, 11, 30), updated.scheduledAt)
        assertEquals("Skin follow up", updated.reason)
        assertEquals(VisitType.FOLLOW_UP, updated.visitType)
        assertEquals("Dr. Bima", updated.veterinarianName)
    }

    @Test
    fun `missing patient from service is shown as form error message`() {
        val app = fixture()
        app.viewModel.updatePatientId("PET-404")
        app.viewModel.updateScheduledAt("2026-06-23T09:30")
        app.viewModel.updateReason("Vaccination")

        app.viewModel.save()

        assertEquals("Pet with ID PET-404 was not found.", app.viewModel.state.errorMessage)
        assertNull(app.viewModel.state.successMessage)
        assertNull(app.viewModel.state.savedAppointmentId)
        assertTrue(app.appointmentService.listAppointments().isEmpty())
    }

    @Test
    fun `completed appointment cannot be rescheduled`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Lia Santoso", "0822222222")
        val patient = app.patientService.registerPet(owner.id, "Bento", "Dog")
        val appointment = app.appointmentService.scheduleAppointment(
            petId = patient.id,
            scheduledAt = LocalDateTime.of(2026, 6, 23, 13, 0),
            reason = "Dental check"
        )
        app.appointmentService.completeAppointment(appointment.id)
        app.viewModel.load(appointment.id)
        app.viewModel.updateScheduledAt("2026-06-23T14:00")
        app.viewModel.updateReason("Dental follow up")

        app.viewModel.save()

        assertEquals("Only scheduled appointments can be rescheduled.", app.viewModel.state.errorMessage)
        assertEquals(AppointmentStatus.COMPLETED, app.appointmentService.getAppointment(appointment.id).status)
    }

    @Test
    fun `start create can prefill patient and clears loaded appointment state`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        val patient = app.patientService.registerPet(owner.id, "Darcy", "Dog")
        val appointment = app.appointmentService.scheduleAppointment(
            petId = patient.id,
            scheduledAt = LocalDateTime.of(2026, 6, 23, 9, 30),
            reason = "Vaccination"
        )
        app.viewModel.load(appointment.id)

        app.viewModel.startCreate(patient.id)

        assertEquals(AppointmentFormMode.CREATE, app.viewModel.state.mode)
        assertNull(app.viewModel.state.appointmentId)
        assertEquals(patient.id, app.viewModel.state.patientId)
        assertEquals("", app.viewModel.state.scheduledAt)
        assertEquals("", app.viewModel.state.reason)
    }

    private data class Fixture(
        val ownerService: OwnerService,
        val patientService: PatientService,
        val appointmentService: AppointmentService,
        val viewModel: AppointmentFormViewModel
    )
}
