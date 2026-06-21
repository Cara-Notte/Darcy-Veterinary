package darcy.veterinary.sqlite

import darcy.veterinary.application.CliRuntimeFactory
import darcy.veterinary.domain.model.Owner
import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.infrastructure.sqlite.SqliteAppointmentRepository
import darcy.veterinary.infrastructure.sqlite.SqliteInvoiceRepository
import darcy.veterinary.infrastructure.sqlite.SqliteInvoiceStatusHistoryRepository
import darcy.veterinary.infrastructure.sqlite.SqliteMedicalRecordRepository
import darcy.veterinary.infrastructure.sqlite.SqliteMedicalRecordRevisionRepository
import darcy.veterinary.infrastructure.sqlite.SqliteOwnerRepository
import darcy.veterinary.infrastructure.sqlite.SqlitePetRepository
import darcy.veterinary.infrastructure.storage.NoOpClinicStorage
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CliRuntimeFactoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `sqlite runtime runs migrations and wires CLI to SQLite repositories`() {
        val config = DatabaseConfig(
            databasePath = tempDir.resolve("darcy-vet.db"),
            backupDirectory = tempDir.resolve("backups")
        )

        val runtime = CliRuntimeFactory.sqlite(config)

        assertTrue(Files.exists(config.databasePath))
        assertTrue(runtime.ownerRepository is SqliteOwnerRepository)
        assertTrue(runtime.petRepository is SqlitePetRepository)
        assertTrue(runtime.appointmentRepository is SqliteAppointmentRepository)
        assertTrue(runtime.medicalRecordRepository is SqliteMedicalRecordRepository)
        assertTrue(runtime.medicalRecordRevisionRepository is SqliteMedicalRecordRevisionRepository)
        assertTrue(runtime.invoiceRepository is SqliteInvoiceRepository)
        assertTrue(runtime.invoiceStatusHistoryRepository is SqliteInvoiceStatusHistoryRepository)
        assertSame(NoOpClinicStorage, runtime.storage)
    }

    @Test
    fun `sqlite runtime persists data after reopening`() {
        val config = DatabaseConfig(
            databasePath = tempDir.resolve("darcy-vet.db"),
            backupDirectory = tempDir.resolve("backups")
        )
        val owner = Owner("OWN-0001", "Nadia Prasetyo", "0811111111", "nadia@example.com")

        CliRuntimeFactory.sqlite(config).ownerRepository.save(owner)

        val reopened = CliRuntimeFactory.sqlite(config)
        assertEquals(owner, reopened.ownerRepository.findById("OWN-0001"))
    }
}
