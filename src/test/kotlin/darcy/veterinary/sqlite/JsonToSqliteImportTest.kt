package darcy.veterinary.sqlite

import darcy.veterinary.domain.model.Appointment
import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.Invoice
import darcy.veterinary.domain.model.InvoiceItem
import darcy.veterinary.domain.model.InvoiceStatusHistory
import darcy.veterinary.domain.model.MedicalRecord
import darcy.veterinary.domain.model.MedicalRecordRevision
import darcy.veterinary.domain.model.Owner
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.domain.model.Pet
import darcy.veterinary.domain.model.PetSex
import darcy.veterinary.domain.model.VisitType
import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.infrastructure.migration.JsonToSqliteImport
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceStatusHistoryRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRevisionRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import darcy.veterinary.infrastructure.sqlite.SqliteAppointmentRepository
import darcy.veterinary.infrastructure.sqlite.SqliteInvoiceRepository
import darcy.veterinary.infrastructure.sqlite.SqliteInvoiceStatusHistoryRepository
import darcy.veterinary.infrastructure.sqlite.SqliteMedicalRecordRepository
import darcy.veterinary.infrastructure.sqlite.SqliteMedicalRecordRevisionRepository
import darcy.veterinary.infrastructure.sqlite.SqliteOwnerRepository
import darcy.veterinary.infrastructure.sqlite.SqlitePetRepository
import darcy.veterinary.infrastructure.storage.JsonClinicStorage
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class JsonToSqliteImportTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `imports JSON snapshot into SQLite repositories`() {
        val jsonDirectory = tempDir.resolve("json")
        val databaseConfig = databaseConfig()
        val snapshot = snapshot()
        writeSnapshot(jsonDirectory, snapshot)

        JsonToSqliteImport(databaseConfig).run(jsonDirectory)

        val sqlite = sqliteRepositories(databaseConfig)
        assertEquals(listOf(snapshot.owner), sqlite.ownerRepository.findAll())
        assertEquals(listOf(snapshot.pet), sqlite.petRepository.findAll())
        assertEquals(listOf(snapshot.appointment), sqlite.appointmentRepository.findAll())
        assertEquals(listOf(snapshot.medicalRecord), sqlite.medicalRecordRepository.findAll())
        assertEquals(listOf(snapshot.revision), sqlite.revisionRepository.findAll())
        assertEquals(listOf(snapshot.invoice), sqlite.invoiceRepository.findAll())
        assertEquals(listOf(snapshot.invoiceHistory), sqlite.invoiceHistoryRepository.findAll())
        assertTrue(Files.exists(databaseConfig.databasePath))
    }

    @Test
    fun `import can be safely run more than once`() {
        val jsonDirectory = tempDir.resolve("json")
        val databaseConfig = databaseConfig()
        val snapshot = snapshot()
        writeSnapshot(jsonDirectory, snapshot)
        val importer = JsonToSqliteImport(databaseConfig)

        importer.run(jsonDirectory)
        importer.run(jsonDirectory)

        val sqlite = sqliteRepositories(databaseConfig)
        assertEquals(listOf(snapshot.owner), sqlite.ownerRepository.findAll())
        assertEquals(listOf(snapshot.pet), sqlite.petRepository.findAll())
        assertEquals(listOf(snapshot.appointment), sqlite.appointmentRepository.findAll())
        assertEquals(listOf(snapshot.medicalRecord), sqlite.medicalRecordRepository.findAll())
        assertEquals(listOf(snapshot.revision), sqlite.revisionRepository.findAll())
        assertEquals(listOf(snapshot.invoice), sqlite.invoiceRepository.findAll())
        assertEquals(listOf(snapshot.invoiceHistory), sqlite.invoiceHistoryRepository.findAll())
    }

    @Test
    fun `missing JSON snapshot leaves migrated empty SQLite database`() {
        val databaseConfig = databaseConfig()

        JsonToSqliteImport(databaseConfig).run(tempDir.resolve("missing-json"))

        val sqlite = sqliteRepositories(databaseConfig)
        assertTrue(Files.exists(databaseConfig.databasePath))
        assertEquals(emptyList(), sqlite.ownerRepository.findAll())
        assertEquals(emptyList(), sqlite.petRepository.findAll())
        assertEquals(emptyList(), sqlite.invoiceRepository.findAll())
    }

    private fun writeSnapshot(jsonDirectory: Path, snapshot: Snapshot) {
        val ownerRepository = InMemoryOwnerRepository().apply { save(snapshot.owner) }
        val petRepository = InMemoryPetRepository().apply { save(snapshot.pet) }
        val appointmentRepository = InMemoryAppointmentRepository().apply { save(snapshot.appointment) }
        val medicalRecordRepository = InMemoryMedicalRecordRepository().apply { save(snapshot.medicalRecord) }
        val revisionRepository = InMemoryMedicalRecordRevisionRepository().apply { save(snapshot.revision) }
        val invoiceRepository = InMemoryInvoiceRepository().apply { save(snapshot.invoice) }
        val invoiceHistoryRepository = InMemoryInvoiceStatusHistoryRepository().apply { save(snapshot.invoiceHistory) }

        JsonClinicStorage(jsonDirectory).saveAll(
            ownerRepository = ownerRepository,
            petRepository = petRepository,
            appointmentRepository = appointmentRepository,
            medicalRecordRepository = medicalRecordRepository,
            invoiceRepository = invoiceRepository,
            revisionRepository = revisionRepository,
            invoiceHistoryRepository = invoiceHistoryRepository
        )
    }

    private fun snapshot(): Snapshot {
        val owner = Owner("OWN-0001", "Nadia Prasetyo", "0811111111", "nadia@example.com")
        val pet = Pet(
            id = "PET-0001",
            ownerId = owner.id,
            name = "Milo",
            species = "Cat",
            breed = "Domestic Shorthair",
            age = 4,
            sex = PetSex.MALE,
            dateOfBirth = LocalDate.of(2022, 2, 14),
            weightKg = 4.7,
            allergies = listOf("Chicken"),
            medicalConditions = listOf("Sensitive skin")
        )
        val appointment = Appointment(
            id = "APT-0001",
            petId = pet.id,
            scheduledAt = LocalDateTime.of(2026, 6, 20, 10, 30),
            reason = "Wellness check",
            status = AppointmentStatus.COMPLETED,
            visitType = VisitType.GENERAL,
            veterinarianName = "dr. Anisa"
        )
        val medicalRecord = MedicalRecord(
            id = "REC-0001",
            petId = pet.id,
            appointmentId = appointment.id,
            diagnosis = "Dermatitis",
            treatment = "Topical medication",
            notes = "Original notes\nwith follow-up.",
            recordedAt = LocalDateTime.of(2026, 6, 20, 11, 15),
            veterinarianName = "dr. Anisa"
        )
        val revision = MedicalRecordRevision(
            id = "REV-0001",
            recordId = medicalRecord.id,
            diagnosis = "Initial dermatitis",
            treatment = "Initial topical medication",
            notes = "Previous notes",
            changedAt = LocalDateTime.of(2026, 6, 20, 12, 0)
        )
        val invoice = Invoice(
            id = "INV-0001",
            petId = pet.id,
            items = listOf(InvoiceItem(ClinicService.CONSULTATION)),
            issuedAt = LocalDateTime.of(2026, 6, 20, 12, 30),
            paymentStatus = PaymentStatus.PAID
        )
        val invoiceHistory = InvoiceStatusHistory(
            id = "HIS-0001",
            invoiceId = invoice.id,
            fromStatus = PaymentStatus.UNPAID,
            toStatus = PaymentStatus.PAID,
            changedAt = LocalDateTime.of(2026, 6, 20, 13, 0),
            reason = "Invoice paid"
        )
        return Snapshot(owner, pet, appointment, medicalRecord, revision, invoice, invoiceHistory)
    }

    private fun sqliteRepositories(config: DatabaseConfig): SqliteRepositories {
        val connectionFactory = DatabaseConnectionFactory(config)
        return SqliteRepositories(
            ownerRepository = SqliteOwnerRepository(connectionFactory),
            petRepository = SqlitePetRepository(connectionFactory),
            appointmentRepository = SqliteAppointmentRepository(connectionFactory),
            medicalRecordRepository = SqliteMedicalRecordRepository(connectionFactory),
            revisionRepository = SqliteMedicalRecordRevisionRepository(connectionFactory),
            invoiceRepository = SqliteInvoiceRepository(connectionFactory),
            invoiceHistoryRepository = SqliteInvoiceStatusHistoryRepository(connectionFactory)
        )
    }

    private fun databaseConfig(): DatabaseConfig = DatabaseConfig(
        databasePath = tempDir.resolve("darcy-vet.db"),
        backupDirectory = tempDir.resolve("backups")
    )

    private data class Snapshot(
        val owner: Owner,
        val pet: Pet,
        val appointment: Appointment,
        val medicalRecord: MedicalRecord,
        val revision: MedicalRecordRevision,
        val invoice: Invoice,
        val invoiceHistory: InvoiceStatusHistory
    )

    private data class SqliteRepositories(
        val ownerRepository: SqliteOwnerRepository,
        val petRepository: SqlitePetRepository,
        val appointmentRepository: SqliteAppointmentRepository,
        val medicalRecordRepository: SqliteMedicalRecordRepository,
        val revisionRepository: SqliteMedicalRecordRevisionRepository,
        val invoiceRepository: SqliteInvoiceRepository,
        val invoiceHistoryRepository: SqliteInvoiceStatusHistoryRepository
    )
}
