package darcy.veterinary.sqlite

import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.exception.EntityNotFoundException
import darcy.veterinary.domain.exception.InvalidClinicOperationException
import darcy.veterinary.domain.model.Appointment
import darcy.veterinary.domain.model.MedicalRecord
import darcy.veterinary.domain.model.Owner
import darcy.veterinary.domain.model.Pet
import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.infrastructure.database.DatabaseMigrator
import darcy.veterinary.infrastructure.sqlite.SqliteAppointmentRepository
import darcy.veterinary.infrastructure.sqlite.SqliteMedicalRecordRepository
import darcy.veterinary.infrastructure.sqlite.SqliteOwnerRepository
import darcy.veterinary.infrastructure.sqlite.SqlitePetRepository
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SqliteMedicalRecordRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `saves and reloads medical record with appointment link`() {
        val fixture = fixture()
        val record = MedicalRecord(
            id = "REC-0001",
            petId = "PET-0001",
            appointmentId = "APT-0001",
            diagnosis = "Dermatitis",
            treatment = "Topical medication",
            notes = "Owner reports itching.\nFollow up in 7 days.",
            recordedAt = LocalDateTime.of(2026, 6, 20, 11, 15),
            veterinarianName = "dr. Anisa"
        )

        fixture.medicalRecordRepository.save(record)

        assertEquals(record, fixture.medicalRecordRepository.findById("REC-0001"))
    }

    @Test
    fun `saves and reloads medical record without appointment link or veterinarian name`() {
        val fixture = fixture()
        val record = MedicalRecord(
            id = "REC-0001",
            petId = "PET-0001",
            appointmentId = null,
            diagnosis = "Healthy",
            treatment = "Routine observation",
            notes = "No major findings.",
            recordedAt = LocalDateTime.of(2026, 6, 20, 11, 15),
            veterinarianName = null
        )

        fixture.medicalRecordRepository.save(record)

        assertEquals(record, fixture.medicalRecordRepository.findById("REC-0001"))
    }

    @Test
    fun `updates existing medical record without creating duplicate rows`() {
        val fixture = fixture()
        val original = MedicalRecord(
            id = "REC-0001",
            petId = "PET-0001",
            appointmentId = "APT-0001",
            diagnosis = "Dermatitis",
            treatment = "Topical medication",
            notes = "Initial notes",
            recordedAt = LocalDateTime.of(2026, 6, 20, 11, 15),
            veterinarianName = "dr. Anisa"
        )
        val updated = original.copy(
            diagnosis = "Allergic dermatitis",
            treatment = "Topical medication and dietary review",
            notes = "Updated multiline notes.\nOwner instructed to monitor symptoms.",
            veterinarianName = "dr. Bima"
        )

        fixture.medicalRecordRepository.save(original)
        fixture.medicalRecordRepository.save(updated)

        assertEquals(updated, fixture.medicalRecordRepository.findById("REC-0001"))
        assertEquals(listOf(updated), fixture.medicalRecordRepository.findAll())
    }

    @Test
    fun `find all returns records in insertion order`() {
        val fixture = fixture()
        val first = MedicalRecord("REC-0001", "PET-0001", null, "Healthy", "Observation", "No issue", LocalDateTime.of(2026, 6, 20, 11, 15))
        val second = MedicalRecord("REC-0002", "PET-0001", "APT-0001", "Dermatitis", "Medication", "Follow up", LocalDateTime.of(2026, 6, 21, 12, 0))

        fixture.medicalRecordRepository.save(first)
        fixture.medicalRecordRepository.save(second)

        assertEquals(listOf(first, second), fixture.medicalRecordRepository.findAll())
    }

    @Test
    fun `find by pet id returns only matching records`() {
        val fixture = fixture()
        fixture.petRepository.save(Pet("PET-0002", "OWN-0001", "Luna", "Cat"))
        val first = MedicalRecord("REC-0001", "PET-0001", null, "Healthy", "Observation", "No issue", LocalDateTime.of(2026, 6, 20, 11, 15))
        val second = MedicalRecord("REC-0002", "PET-0001", "APT-0001", "Dermatitis", "Medication", "Follow up", LocalDateTime.of(2026, 6, 21, 12, 0))
        val third = MedicalRecord("REC-0003", "PET-0002", null, "Dental plaque", "Cleaning", "Schedule dental care", LocalDateTime.of(2026, 6, 22, 9, 30))

        fixture.medicalRecordRepository.save(first)
        fixture.medicalRecordRepository.save(second)
        fixture.medicalRecordRepository.save(third)

        assertEquals(listOf(first, second), fixture.medicalRecordRepository.findByPetId("PET-0001"))
        assertEquals(listOf(third), fixture.medicalRecordRepository.findByPetId("PET-0002"))
    }

    @Test
    fun `medical record persists after repository is reopened`() {
        val config = databaseConfig()
        val firstFixture = fixture(config)
        val record = MedicalRecord(
            id = "REC-0001",
            petId = "PET-0001",
            appointmentId = "APT-0001",
            diagnosis = "Dermatitis",
            treatment = "Topical medication",
            notes = "Persistent notes",
            recordedAt = LocalDateTime.of(2026, 6, 20, 11, 15),
            veterinarianName = "dr. Anisa"
        )

        firstFixture.medicalRecordRepository.save(record)

        val reopenedFixture = fixture(config, seedDefaultAppointment = false)
        assertEquals(record, reopenedFixture.medicalRecordRepository.findById("REC-0001"))
    }

    @Test
    fun `returns null for missing medical record`() {
        val fixture = fixture()

        assertNull(fixture.medicalRecordRepository.findById("REC-MISSING"))
    }

    @Test
    fun `database foreign key rejects record for missing pet outside service layer`() {
        val fixture = fixture(seedDefaultAppointment = false)
        val record = MedicalRecord(
            id = "REC-0001",
            petId = "PET-MISSING",
            appointmentId = null,
            diagnosis = "Ghost diagnosis",
            treatment = "None",
            notes = "Invalid",
            recordedAt = LocalDateTime.of(2026, 6, 20, 11, 15)
        )

        assertFailsWith<IllegalStateException> {
            fixture.medicalRecordRepository.save(record)
        }
    }

    @Test
    fun `database foreign key rejects record for missing appointment outside service layer`() {
        val fixture = fixture()
        val record = MedicalRecord(
            id = "REC-0001",
            petId = "PET-0001",
            appointmentId = "APT-MISSING",
            diagnosis = "Ghost appointment",
            treatment = "None",
            notes = "Invalid",
            recordedAt = LocalDateTime.of(2026, 6, 20, 11, 15)
        )

        assertFailsWith<IllegalStateException> {
            fixture.medicalRecordRepository.save(record)
        }
    }

    @Test
    fun `medical record service works with SQLite repositories`() {
        val fixture = fixture()
        val service = MedicalRecordService(
            medicalRecordRepository = fixture.medicalRecordRepository,
            petRepository = fixture.petRepository,
            appointmentRepository = fixture.appointmentRepository,
            idGenerator = SequenceIdGenerator()
        )

        val created = service.createRecord(
            petId = "PET-0001",
            appointmentId = "APT-0001",
            diagnosis = "  Dermatitis  ",
            treatment = "  Topical medication  ",
            notes = "  Owner reports itching.  ",
            recordedAt = LocalDateTime.of(2026, 6, 20, 11, 15),
            veterinarianName = "  dr. Anisa  "
        )
        val updated = service.updateRecord(
            id = created.id,
            diagnosis = " Allergic dermatitis ",
            treatment = " Medication and diet review ",
            notes = " Updated notes ",
            veterinarianName = " dr. Bima "
        )

        assertEquals("REC-0001", created.id)
        assertEquals("Dermatitis", created.diagnosis)
        assertEquals("Topical medication", created.treatment)
        assertEquals("Owner reports itching.", created.notes)
        assertEquals("dr. Anisa", created.veterinarianName)
        assertEquals("Allergic dermatitis", updated.diagnosis)
        assertEquals("Medication and diet review", updated.treatment)
        assertEquals("Updated notes", updated.notes)
        assertEquals("dr. Bima", updated.veterinarianName)
        assertFailsWith<EntityNotFoundException> {
            service.createRecord("PET-MISSING", "Diagnosis", "Treatment", "Notes")
        }
        assertFailsWith<InvalidClinicOperationException> {
            service.createRecord("PET-0002", "Diagnosis", "Treatment", "Notes", appointmentId = "APT-0001")
        }
    }

    private fun fixture(
        config: DatabaseConfig = databaseConfig(),
        seedDefaultAppointment: Boolean = true
    ): Fixture {
        val connectionFactory = DatabaseConnectionFactory(config)
        DatabaseMigrator(connectionFactory).migrate()
        val ownerRepository = SqliteOwnerRepository(connectionFactory)
        val petRepository = SqlitePetRepository(connectionFactory)
        val appointmentRepository = SqliteAppointmentRepository(connectionFactory)
        val medicalRecordRepository = SqliteMedicalRecordRepository(connectionFactory)

        if (ownerRepository.findById("OWN-0001") == null) {
            ownerRepository.save(Owner("OWN-0001", "Nadia Prasetyo", "0811111111", "nadia@example.com"))
        }
        if (petRepository.findById("PET-0001") == null) {
            petRepository.save(Pet("PET-0001", "OWN-0001", "Milo", "Cat"))
        }
        if (petRepository.findById("PET-0002") == null) {
            petRepository.save(Pet("PET-0002", "OWN-0001", "Luna", "Cat"))
        }
        if (seedDefaultAppointment && appointmentRepository.findById("APT-0001") == null) {
            appointmentRepository.save(Appointment("APT-0001", "PET-0001", LocalDateTime.of(2026, 6, 20, 10, 30), "Wellness check"))
        }

        return Fixture(petRepository, appointmentRepository, medicalRecordRepository)
    }

    private fun databaseConfig(): DatabaseConfig = DatabaseConfig(
        databasePath = tempDir.resolve("darcy-vet.db"),
        backupDirectory = tempDir.resolve("backups")
    )

    private data class Fixture(
        val petRepository: SqlitePetRepository,
        val appointmentRepository: SqliteAppointmentRepository,
        val medicalRecordRepository: SqliteMedicalRecordRepository
    )
}
