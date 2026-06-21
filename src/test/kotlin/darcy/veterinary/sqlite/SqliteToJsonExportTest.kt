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
import darcy.veterinary.infrastructure.database.DatabaseMigrator
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceStatusHistoryRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRevisionRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import darcy.veterinary.infrastructure.migration.SqliteToJsonExport
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

class SqliteToJsonExportTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `exports SQLite repositories into JSON snapshot`() {
        val databaseConfig = databaseConfig()
        val jsonDirectory = tempDir.resolve("json")
        val snapshot = snapshot()
        seedSqlite(databaseConfig, snapshot)

        SqliteToJsonExport(databaseConfig).run(jsonDirectory)

        val memory = loadJson(jsonDirectory)
        assertEquals(listOf(snapshot.owner), memory.ownerRepository.findAll())
        assertEquals(listOf(snapshot.pet), memory.petRepository.findAll())
        assertEquals(listOf(snapshot.appointment), memory.appointmentRepository.findAll())
        assertEquals(listOf(snapshot.medicalRecord), memory.medicalRecordRepository.findAll())
        assertEquals(listOf(snapshot.revision), memory.revisionRepository.findAll())
        assertEquals(listOf(snapshot.invoice), memory.invoiceRepository.findAll())
        assertEquals(listOf(snapshot.invoiceHistory), memory.invoiceHistoryRepository.findAll())
        assertTrue(Files.exists(jsonDirectory.resolve("clinic-data.json")))
    }

    @Test
    fun `export can be safely run more than once`() {
        val databaseConfig = databaseConfig()
        val jsonDirectory = tempDir.resolve("json")
        val snapshot = snapshot()
        seedSqlite(databaseConfig, snapshot)
        val export = SqliteToJsonExport(databaseConfig)

        export.run(jsonDirectory)
        export.run(jsonDirectory)

        val memory = loadJson(jsonDirectory)
        assertEquals(listOf(snapshot.owner), memory.ownerRepository.findAll())
        assertEquals(listOf(snapshot.pet), memory.petRepository.findAll())
        assertEquals(listOf(snapshot.appointment), memory.appointmentRepository.findAll())
        assertEquals(listOf(snapshot.medicalRecord), memory.medicalRecordRepository.findAll())
        assertEquals(listOf(snapshot.revision), memory.revisionRepository.findAll())
        assertEquals(listOf(snapshot.invoice), memory.invoiceRepository.findAll())
        assertEquals(listOf(snapshot.invoiceHistory), memory.invoiceHistoryRepository.findAll())
    }

    @Test
    fun `empty SQLite database exports empty JSON snapshot`() {
        val databaseConfig = databaseConfig()
        val jsonDirectory = tempDir.resolve("json")

        SqliteToJsonExport(databaseConfig).run(jsonDirectory)

        val memory = loadJson(jsonDirectory)
        assertTrue(Files.exists(databaseConfig.databasePath))
        assertTrue(Files.exists(jsonDirectory.resolve("clinic-data.json")))
        assertEquals(emptyList(), memory.ownerRepository.findAll())
        assertEquals(emptyList(), memory.petRepository.findAll())
        assertEquals(emptyList(), memory.invoiceRepository.findAll())
    }

    private fun seedSqlite(config: DatabaseConfig, snapshot: Snapshot) {
        val connectionFactory = DatabaseConnectionFactory(config)
        DatabaseMigrator(connectionFactory).migrate()
        SqliteOwnerRepository(connectionFactory).save(snapshot.owner)
        SqlitePetRepository(connectionFactory).save(snapshot.pet)
        SqliteAppointmentRepository(connectionFactory).save(snapshot.appointment)
        SqliteMedicalRecordRepository(connectionFactory).save(snapshot.medicalRecord)
        SqliteMedicalRecordRevisionRepository(connectionFactory).save(snapshot.revision)
        SqliteInvoiceRepository(connectionFactory).save(snapshot.invoice)
        SqliteInvoiceStatusHistoryRepository(connectionFactory).save(snapshot.invoiceHistory)
    }

    private fun loadJson(jsonDirectory: Path): MemoryRepositories {
        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val appointmentRepository = InMemoryAppointmentRepository()
        val medicalRecordRepository = InMemoryMedicalRecordRepository()
        val revisionRepository = InMemoryMedicalRecordRevisionRepository()
        val invoiceRepository = InMemoryInvoiceRepository()
        val invoiceHistoryRepository = InMemoryInvoiceStatusHistoryRepository()

        JsonClinicStorage(jsonDirectory).loadAll(
            ownerRepository = ownerRepository,
            petRepository = petRepository,
            appointmentRepository = appointmentRepository,
            medicalRecordRepository = medicalRecordRepository,
            invoiceRepository = invoiceRepository,
            revisionRepository = revisionRepository,
            invoiceHistoryRepository = invoiceHistoryRepository
        )

        return MemoryRepositories(
            ownerRepository,
            petRepository,
            appointmentRepository,
            medicalRecordRepository,
            revisionRepository,
            invoiceRepository,
            invoiceHistoryRepository
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

    private data class MemoryRepositories(
        val ownerRepository: InMemoryOwnerRepository,
        val petRepository: InMemoryPetRepository,
        val appointmentRepository: InMemoryAppointmentRepository,
        val medicalRecordRepository: InMemoryMedicalRecordRepository,
        val revisionRepository: InMemoryMedicalRecordRevisionRepository,
        val invoiceRepository: InMemoryInvoiceRepository,
        val invoiceHistoryRepository: InMemoryInvoiceStatusHistoryRepository
    )
}
