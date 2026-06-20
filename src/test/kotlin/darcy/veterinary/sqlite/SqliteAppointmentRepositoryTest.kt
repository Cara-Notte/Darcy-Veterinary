package darcy.veterinary.sqlite

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.exception.EntityNotFoundException
import darcy.veterinary.domain.exception.InvalidClinicOperationException
import darcy.veterinary.domain.model.Appointment
import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.Owner
import darcy.veterinary.domain.model.Pet
import darcy.veterinary.domain.model.VisitType
import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.infrastructure.database.DatabaseMigrator
import darcy.veterinary.infrastructure.sqlite.SqliteAppointmentRepository
import darcy.veterinary.infrastructure.sqlite.SqliteOwnerRepository
import darcy.veterinary.infrastructure.sqlite.SqlitePetRepository
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SqliteAppointmentRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `saves and reloads scheduled appointment`() {
        val fixture = fixture()
        val appointment = Appointment(
            id = "APT-0001",
            petId = "PET-0001",
            scheduledAt = LocalDateTime.of(2026, 6, 20, 10, 30),
            reason = "Annual wellness check",
            status = AppointmentStatus.SCHEDULED,
            visitType = VisitType.GENERAL,
            veterinarianName = "dr. Anisa"
        )

        fixture.appointmentRepository.save(appointment)

        assertEquals(appointment, fixture.appointmentRepository.findById("APT-0001"))
    }

    @Test
    fun `updates appointment status and reschedule fields`() {
        val fixture = fixture()
        val original = Appointment(
            id = "APT-0001",
            petId = "PET-0001",
            scheduledAt = LocalDateTime.of(2026, 6, 20, 10, 30),
            reason = "Annual wellness check",
            status = AppointmentStatus.SCHEDULED,
            visitType = VisitType.GENERAL,
            veterinarianName = null
        )
        val updated = original.copy(
            scheduledAt = LocalDateTime.of(2026, 6, 21, 14, 0),
            reason = "Follow-up check",
            status = AppointmentStatus.COMPLETED,
            visitType = VisitType.FOLLOW_UP,
            veterinarianName = "dr. Bima"
        )

        fixture.appointmentRepository.save(original)
        fixture.appointmentRepository.save(updated)

        assertEquals(updated, fixture.appointmentRepository.findById("APT-0001"))
        assertEquals(listOf(updated), fixture.appointmentRepository.findAll())
    }

    @Test
    fun `find all returns appointments in insertion order`() {
        val fixture = fixture()
        val first = Appointment(
            id = "APT-0001",
            petId = "PET-0001",
            scheduledAt = LocalDateTime.of(2026, 6, 20, 10, 30),
            reason = "Wellness check"
        )
        val second = Appointment(
            id = "APT-0002",
            petId = "PET-0001",
            scheduledAt = LocalDateTime.of(2026, 6, 21, 9, 0),
            reason = "Vaccination",
            visitType = VisitType.VACCINATION
        )

        fixture.appointmentRepository.save(first)
        fixture.appointmentRepository.save(second)

        assertEquals(listOf(first, second), fixture.appointmentRepository.findAll())
    }

    @Test
    fun `find by pet id returns only matching appointments`() {
        val fixture = fixture()
        fixture.petRepository.save(Pet("PET-0002", "OWN-0001", "Luna", "Cat"))
        val first = Appointment("APT-0001", "PET-0001", LocalDateTime.of(2026, 6, 20, 10, 30), "Wellness check")
        val second = Appointment("APT-0002", "PET-0001", LocalDateTime.of(2026, 6, 21, 9, 0), "Vaccination")
        val third = Appointment("APT-0003", "PET-0002", LocalDateTime.of(2026, 6, 22, 9, 0), "Grooming", visitType = VisitType.GROOMING)

        fixture.appointmentRepository.save(first)
        fixture.appointmentRepository.save(second)
        fixture.appointmentRepository.save(third)

        assertEquals(listOf(first, second), fixture.appointmentRepository.findByPetId("PET-0001"))
        assertEquals(listOf(third), fixture.appointmentRepository.findByPetId("PET-0002"))
    }

    @Test
    fun `appointment persists after repository is reopened`() {
        val config = databaseConfig()
        val firstFixture = fixture(config)
        val appointment = Appointment(
            id = "APT-0001",
            petId = "PET-0001",
            scheduledAt = LocalDateTime.of(2026, 6, 20, 10, 30),
            reason = "Annual wellness check",
            visitType = VisitType.EMERGENCY,
            veterinarianName = "dr. Anisa"
        )

        firstFixture.appointmentRepository.save(appointment)

        val reopenedFixture = fixture(config, seedDefaultPet = false)
        assertEquals(appointment, reopenedFixture.appointmentRepository.findById("APT-0001"))
    }

    @Test
    fun `returns null for missing appointment`() {
        val fixture = fixture()

        assertNull(fixture.appointmentRepository.findById("APT-MISSING"))
    }

    @Test
    fun `database foreign key rejects appointment for missing pet outside service layer`() {
        val fixture = fixture(seedDefaultPet = false)
        val appointment = Appointment(
            id = "APT-0001",
            petId = "PET-MISSING",
            scheduledAt = LocalDateTime.of(2026, 6, 20, 10, 30),
            reason = "Ghost appointment"
        )

        assertFailsWith<IllegalStateException> {
            fixture.appointmentRepository.save(appointment)
        }
    }

    @Test
    fun `appointment service works with SQLite repositories`() {
        val fixture = fixture()
        val service = AppointmentService(fixture.appointmentRepository, fixture.petRepository, SequenceIdGenerator())

        val appointment = service.scheduleAppointment(
            petId = "PET-0001",
            scheduledAt = LocalDateTime.of(2026, 6, 20, 10, 30),
            reason = "  Annual wellness check  ",
            visitType = VisitType.VACCINATION,
            veterinarianName = "  dr. Anisa  "
        )
        val rescheduled = service.rescheduleAppointment(
            id = appointment.id,
            scheduledAt = LocalDateTime.of(2026, 6, 21, 14, 0),
            reason = " Follow-up check ",
            visitType = VisitType.FOLLOW_UP,
            veterinarianName = " dr. Bima "
        )
        val completed = service.completeAppointment(appointment.id)

        assertEquals("APT-0001", appointment.id)
        assertEquals(AppointmentStatus.SCHEDULED, appointment.status)
        assertEquals("Annual wellness check", appointment.reason)
        assertEquals("dr. Anisa", appointment.veterinarianName)
        assertEquals(VisitType.FOLLOW_UP, rescheduled.visitType)
        assertEquals("dr. Bima", rescheduled.veterinarianName)
        assertEquals(AppointmentStatus.COMPLETED, completed.status)
        assertFailsWith<EntityNotFoundException> {
            service.scheduleAppointment("PET-MISSING", LocalDateTime.of(2026, 6, 20, 10, 30), "Missing pet")
        }
        assertFailsWith<InvalidClinicOperationException> {
            service.cancelAppointment(appointment.id)
        }
    }

    private fun fixture(
        config: DatabaseConfig = databaseConfig(),
        seedDefaultPet: Boolean = true
    ): Fixture {
        val connectionFactory = DatabaseConnectionFactory(config)
        DatabaseMigrator(connectionFactory).migrate()
        val ownerRepository = SqliteOwnerRepository(connectionFactory)
        val petRepository = SqlitePetRepository(connectionFactory)
        val appointmentRepository = SqliteAppointmentRepository(connectionFactory)

        if (seedDefaultPet && ownerRepository.findById("OWN-0001") == null) {
            ownerRepository.save(Owner("OWN-0001", "Nadia Prasetyo", "0811111111", "nadia@example.com"))
        }
        if (seedDefaultPet && petRepository.findById("PET-0001") == null) {
            petRepository.save(Pet("PET-0001", "OWN-0001", "Milo", "Cat"))
        }

        return Fixture(petRepository, appointmentRepository)
    }

    private fun databaseConfig(): DatabaseConfig = DatabaseConfig(
        databasePath = tempDir.resolve("darcy-vet.db"),
        backupDirectory = tempDir.resolve("backups")
    )

    private data class Fixture(
        val petRepository: SqlitePetRepository,
        val appointmentRepository: SqliteAppointmentRepository
    )
}
