package darcy.veterinary.sqlite

import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.model.Appointment
import darcy.veterinary.domain.model.MedicalRecord
import darcy.veterinary.domain.model.MedicalRecordRevision
import darcy.veterinary.domain.model.Owner
import darcy.veterinary.domain.model.Pet
import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.infrastructure.database.DatabaseMigrator
import darcy.veterinary.infrastructure.sqlite.SqliteAppointmentRepository
import darcy.veterinary.infrastructure.sqlite.SqliteMedicalRecordRepository
import darcy.veterinary.infrastructure.sqlite.SqliteMedicalRecordRevisionRepository
import darcy.veterinary.infrastructure.sqlite.SqliteOwnerRepository
import darcy.veterinary.infrastructure.sqlite.SqlitePetRepository
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SqliteMedicalRecordRevisionRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `saves and reloads medical record revision`() {
        val fixture = fixture()
        val revision = MedicalRecordRevision(
            id = "REV-0001",
            recordId = "REC-0001",
            diagnosis = "Dermatitis",
            treatment = "Topical medication",
            notes = "Original notes\nwith second line.",
            changedAt = LocalDateTime.of(2026, 6, 20, 12, 30)
        )

        fixture.revisionRepository.save(revision)

        assertEquals(listOf(revision), fixture.revisionRepository.findAll())
        assertEquals(listOf(revision), fixture.revisionRepository.findByRecordId("REC-0001"))
    }

    @Test
    fun `updates existing revision without creating duplicate rows`() {
        val fixture = fixture()
        val original = MedicalRecordRevision(
            id = "REV-0001",
            recordId = "REC-0001",
            diagnosis = "Dermatitis",
            treatment = "Topical medication",
            notes = "Original notes",
            changedAt = LocalDateTime.of(2026, 6, 20, 12, 30)
        )
        val updated = original.copy(
            diagnosis = "Allergic dermatitis",
            treatment = "Medication and diet review",
            notes = "Updated revision notes",
            changedAt = LocalDateTime.of(2026, 6, 20, 13, 0)
        )

        fixture.revisionRepository.save(original)
        fixture.revisionRepository.save(updated)

        assertEquals(listOf(updated), fixture.revisionRepository.findAll())
        assertEquals(listOf(updated), fixture.revisionRepository.findByRecordId("REC-0001"))
    }

    @Test
    fun `find by record id returns only matching revisions in insertion order`() {
        val fixture = fixture()
        val first = MedicalRecordRevision("REV-0001", "REC-0001", "Diagnosis 1", "Treatment 1", "Notes 1", LocalDateTime.of(2026, 6, 20, 12, 30))
        val second = MedicalRecordRevision("REV-0002", "REC-0001", "Diagnosis 2", "Treatment 2", "Notes 2", LocalDateTime.of(2026, 6, 20, 13, 0))
        val otherRecord = MedicalRecord("REC-0002", "PET-0001", null, "Healthy", "Observation", "No issues", LocalDateTime.of(2026, 6, 20, 14, 0))
        val third = MedicalRecordRevision("REV-0003", "REC-0002", "Diagnosis 3", "Treatment 3", "Notes 3", LocalDateTime.of(2026, 6, 20, 13, 30))

        fixture.recordRepository.save(otherRecord)
        fixture.revisionRepository.save(first)
        fixture.revisionRepository.save(second)
        fixture.revisionRepository.save(third)

        assertEquals(listOf(first, second), fixture.revisionRepository.findByRecordId("REC-0001"))
        assertEquals(listOf(third), fixture.revisionRepository.findByRecordId("REC-0002"))
    }

    @Test
    fun `revision persists after repository is reopened`() {
        val config = databaseConfig()
        val firstFixture = fixture(config)
        val revision = MedicalRecordRevision(
            id = "REV-0001",
            recordId = "REC-0001",
            diagnosis = "Dermatitis",
            treatment = "Topical medication",
            notes = "Original notes",
            changedAt = LocalDateTime.of(2026, 6, 20, 12, 30)
        )

        firstFixture.revisionRepository.save(revision)

        val reopenedFixture = fixture(config, seedDefaultRecord = false)
        assertEquals(listOf(revision), reopenedFixture.revisionRepository.findByRecordId("REC-0001"))
    }

    @Test
    fun `database foreign key rejects revision for missing medical record`() {
        val fixture = fixture(seedDefaultRecord = false)
        val revision = MedicalRecordRevision(
            id = "REV-0001",
            recordId = "REC-MISSING",
            diagnosis = "Ghost diagnosis",
            treatment = "Ghost treatment",
            notes = "Invalid",
            changedAt = LocalDateTime.of(2026, 6, 20, 12, 30)
        )

        assertFailsWith<IllegalStateException> {
            fixture.revisionRepository.save(revision)
        }
    }

    @Test
    fun `medical record service stores previous values in SQLite revision repository`() {
        val fixture = fixture()
        val service = MedicalRecordService(
            medicalRecordRepository = fixture.recordRepository,
            petRepository = fixture.petRepository,
            appointmentRepository = fixture.appointmentRepository,
            idGenerator = SequenceIdGenerator(),
            revisionRepository = fixture.revisionRepository
        )

        val updated = service.updateRecord(
            id = "REC-0001",
            diagnosis = " Allergic dermatitis ",
            treatment = " Medication and diet review ",
            notes = " Updated notes ",
            veterinarianName = " dr. Bima "
        )
        val revisions = service.listRevisions("REC-0001")

        assertEquals("Allergic dermatitis", updated.diagnosis)
        assertEquals(1, revisions.size)
        assertEquals("REV-0001", revisions.single().id)
        assertEquals("REC-0001", revisions.single().recordId)
        assertEquals("Dermatitis", revisions.single().diagnosis)
        assertEquals("Topical medication", revisions.single().treatment)
        assertEquals("Original notes", revisions.single().notes)
    }

    private fun fixture(
        config: DatabaseConfig = databaseConfig(),
        seedDefaultRecord: Boolean = true
    ): Fixture {
        val connectionFactory = DatabaseConnectionFactory(config)
        DatabaseMigrator(connectionFactory).migrate()
        val ownerRepository = SqliteOwnerRepository(connectionFactory)
        val petRepository = SqlitePetRepository(connectionFactory)
        val appointmentRepository = SqliteAppointmentRepository(connectionFactory)
        val recordRepository = SqliteMedicalRecordRepository(connectionFactory)
        val revisionRepository = SqliteMedicalRecordRevisionRepository(connectionFactory)

        if (ownerRepository.findById("OWN-0001") == null) {
            ownerRepository.save(Owner("OWN-0001", "Nadia Prasetyo", "0811111111", "nadia@example.com"))
        }
        if (petRepository.findById("PET-0001") == null) {
            petRepository.save(Pet("PET-0001", "OWN-0001", "Milo", "Cat"))
        }
        if (appointmentRepository.findById("APT-0001") == null) {
            appointmentRepository.save(Appointment("APT-0001", "PET-0001", LocalDateTime.of(2026, 6, 20, 10, 30), "Wellness check"))
        }
        if (seedDefaultRecord && recordRepository.findById("REC-0001") == null) {
            recordRepository.save(
                MedicalRecord(
                    id = "REC-0001",
                    petId = "PET-0001",
                    appointmentId = "APT-0001",
                    diagnosis = "Dermatitis",
                    treatment = "Topical medication",
                    notes = "Original notes",
                    recordedAt = LocalDateTime.of(2026, 6, 20, 11, 15),
                    veterinarianName = "dr. Anisa"
                )
            )
        }

        return Fixture(petRepository, appointmentRepository, recordRepository, revisionRepository)
    }

    private fun databaseConfig(): DatabaseConfig = DatabaseConfig(
        databasePath = tempDir.resolve("darcy-vet.db"),
        backupDirectory = tempDir.resolve("backups")
    )

    private data class Fixture(
        val petRepository: SqlitePetRepository,
        val appointmentRepository: SqliteAppointmentRepository,
        val recordRepository: SqliteMedicalRecordRepository,
        val revisionRepository: SqliteMedicalRecordRevisionRepository
    )
}
