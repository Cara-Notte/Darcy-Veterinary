package darcy.veterinary.application

import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.VisitType
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class AppointmentBoardFacadeTest {
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
            facade = AppointmentBoardFacade(ownerService, patientService, appointmentService)
        )
    }

    @Test
    fun `day board returns enriched appointment rows sorted by time`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        val darcy = app.patientService.registerPet(
            ownerId = owner.id,
            name = "Darcy",
            species = "Dog",
            allergies = listOf("Chicken")
        )
        val miso = app.patientService.registerPet(owner.id, "Miso", "Cat")
        val date = LocalDate.of(2026, 6, 23)

        app.appointmentService.scheduleAppointment(
            petId = miso.id,
            scheduledAt = LocalDateTime.of(2026, 6, 23, 11, 0),
            reason = "Skin check",
            visitType = VisitType.FOLLOW_UP,
            veterinarianName = "Dr. Sari"
        )
        app.appointmentService.scheduleAppointment(
            petId = darcy.id,
            scheduledAt = LocalDateTime.of(2026, 6, 23, 9, 30),
            reason = "Vaccination",
            visitType = VisitType.VACCINATION,
            veterinarianName = "Dr. Bima"
        )

        val board = app.facade.dayBoard(date)

        assertTrue(board.hasAppointments)
        assertEquals(2, board.summary.totalCount)
        assertEquals(listOf("Darcy", "Miso"), board.rows.map { it.patientName })
        assertEquals("Maya Hartono", board.rows.first().ownerName)
        assertEquals("0833333333", board.rows.first().ownerPhoneNumber)
        assertTrue(board.rows.first().hasPatientAlerts)
        assertFalse(board.rows.last().hasPatientAlerts)
    }

    @Test
    fun `day board summary counts all appointments before status filter`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Nadia Prasetyo", "0811111111")
        val pet = app.patientService.registerPet(owner.id, "Miso", "Cat")
        val date = LocalDate.of(2026, 6, 23)
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
        app.appointmentService.scheduleAppointment(
            petId = pet.id,
            scheduledAt = LocalDateTime.of(2026, 6, 24, 10, 0),
            reason = "Tomorrow visit"
        )

        val board = app.facade.dayBoard(date, AppointmentStatus.SCHEDULED)

        assertEquals(2, board.summary.totalCount)
        assertEquals(1, board.summary.scheduledCount)
        assertEquals(1, board.summary.completedCount)
        assertEquals(0, board.summary.cancelledCount)
        assertEquals(1, board.rows.size)
        assertEquals(AppointmentStatus.SCHEDULED, board.rows.single().status)
        assertEquals("Upcoming visit", board.rows.single().reason)
    }

    private data class Fixture(
        val ownerService: OwnerService,
        val patientService: PatientService,
        val appointmentService: AppointmentService,
        val facade: AppointmentBoardFacade
    )
}
