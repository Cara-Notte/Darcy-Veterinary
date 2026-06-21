package darcy.veterinary.sqlite

import darcy.veterinary.application.BillingService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.Invoice
import darcy.veterinary.domain.model.InvoiceItem
import darcy.veterinary.domain.model.InvoiceStatusHistory
import darcy.veterinary.domain.model.Owner
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.domain.model.Pet
import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.infrastructure.database.DatabaseMigrator
import darcy.veterinary.infrastructure.sqlite.SqliteInvoiceRepository
import darcy.veterinary.infrastructure.sqlite.SqliteInvoiceStatusHistoryRepository
import darcy.veterinary.infrastructure.sqlite.SqliteOwnerRepository
import darcy.veterinary.infrastructure.sqlite.SqlitePetRepository
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SqliteInvoiceStatusHistoryRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `saves and reloads invoice status history with nullable from status`() {
        val fixture = fixture()
        val history = InvoiceStatusHistory(
            id = "HIS-0001",
            invoiceId = "INV-0001",
            fromStatus = null,
            toStatus = PaymentStatus.UNPAID,
            changedAt = LocalDateTime.of(2026, 6, 20, 16, 0),
            reason = "Invoice created"
        )

        fixture.historyRepository.save(history)

        assertEquals(listOf(history), fixture.historyRepository.findAll())
        assertEquals(listOf(history), fixture.historyRepository.findByInvoiceId("INV-0001"))
    }

    @Test
    fun `updates existing invoice status history without creating duplicate rows`() {
        val fixture = fixture()
        val original = InvoiceStatusHistory(
            id = "HIS-0001",
            invoiceId = "INV-0001",
            fromStatus = PaymentStatus.UNPAID,
            toStatus = PaymentStatus.PAID,
            changedAt = LocalDateTime.of(2026, 6, 20, 16, 0),
            reason = "Invoice paid"
        )
        val updated = original.copy(
            toStatus = PaymentStatus.VOIDED,
            changedAt = LocalDateTime.of(2026, 6, 20, 17, 0),
            reason = "Manual correction"
        )

        fixture.historyRepository.save(original)
        fixture.historyRepository.save(updated)

        assertEquals(listOf(updated), fixture.historyRepository.findAll())
        assertEquals(listOf(updated), fixture.historyRepository.findByInvoiceId("INV-0001"))
    }

    @Test
    fun `find by invoice id returns only matching history entries in insertion order`() {
        val fixture = fixture()
        val secondInvoice = Invoice("INV-0002", "PET-0001", listOf(InvoiceItem(ClinicService.GROOMING)), LocalDateTime.of(2026, 6, 21, 15, 0))
        fixture.invoiceRepository.save(secondInvoice)
        val created = InvoiceStatusHistory("HIS-0001", "INV-0001", null, PaymentStatus.UNPAID, LocalDateTime.of(2026, 6, 20, 16, 0), "Invoice created")
        val paid = InvoiceStatusHistory("HIS-0002", "INV-0001", PaymentStatus.UNPAID, PaymentStatus.PAID, LocalDateTime.of(2026, 6, 20, 17, 0), "Invoice paid")
        val other = InvoiceStatusHistory("HIS-0003", "INV-0002", null, PaymentStatus.UNPAID, LocalDateTime.of(2026, 6, 21, 16, 0), "Invoice created")

        fixture.historyRepository.save(created)
        fixture.historyRepository.save(paid)
        fixture.historyRepository.save(other)

        assertEquals(listOf(created, paid), fixture.historyRepository.findByInvoiceId("INV-0001"))
        assertEquals(listOf(other), fixture.historyRepository.findByInvoiceId("INV-0002"))
    }

    @Test
    fun `invoice status history persists after repository is reopened`() {
        val config = databaseConfig()
        val firstFixture = fixture(config)
        val history = InvoiceStatusHistory(
            id = "HIS-0001",
            invoiceId = "INV-0001",
            fromStatus = null,
            toStatus = PaymentStatus.UNPAID,
            changedAt = LocalDateTime.of(2026, 6, 20, 16, 0),
            reason = "Invoice created"
        )

        firstFixture.historyRepository.save(history)

        val reopenedFixture = fixture(config, seedDefaultInvoice = false)
        assertEquals(listOf(history), reopenedFixture.historyRepository.findByInvoiceId("INV-0001"))
    }

    @Test
    fun `database foreign key rejects history for missing invoice`() {
        val fixture = fixture(seedDefaultInvoice = false)
        val history = InvoiceStatusHistory(
            id = "HIS-0001",
            invoiceId = "INV-MISSING",
            fromStatus = null,
            toStatus = PaymentStatus.UNPAID,
            changedAt = LocalDateTime.of(2026, 6, 20, 16, 0),
            reason = "Invalid"
        )

        assertFailsWith<IllegalStateException> {
            fixture.historyRepository.save(history)
        }
    }

    @Test
    fun `billing service stores creation and payment history in SQLite repository`() {
        val fixture = fixture(seedDefaultInvoice = false)
        val service = BillingService(
            invoiceRepository = fixture.invoiceRepository,
            petRepository = fixture.petRepository,
            idGenerator = SequenceIdGenerator(),
            statusHistoryRepository = fixture.historyRepository
        )

        val created = service.createInvoice(
            petId = "PET-0001",
            services = listOf(ClinicService.CONSULTATION),
            issuedAt = LocalDateTime.of(2026, 6, 20, 15, 0)
        )
        val paid = service.markAsPaid(created.id)
        val history = service.listStatusHistory(created.id)

        assertEquals(PaymentStatus.PAID, paid.paymentStatus)
        assertEquals(2, history.size)
        assertEquals("HIS-0001", history[0].id)
        assertEquals(null, history[0].fromStatus)
        assertEquals(PaymentStatus.UNPAID, history[0].toStatus)
        assertEquals("Invoice created", history[0].reason)
        assertEquals("HIS-0002", history[1].id)
        assertEquals(PaymentStatus.UNPAID, history[1].fromStatus)
        assertEquals(PaymentStatus.PAID, history[1].toStatus)
        assertEquals("Invoice paid", history[1].reason)
    }

    private fun fixture(
        config: DatabaseConfig = databaseConfig(),
        seedDefaultInvoice: Boolean = true
    ): Fixture {
        val connectionFactory = DatabaseConnectionFactory(config)
        DatabaseMigrator(connectionFactory).migrate()
        val ownerRepository = SqliteOwnerRepository(connectionFactory)
        val petRepository = SqlitePetRepository(connectionFactory)
        val invoiceRepository = SqliteInvoiceRepository(connectionFactory)
        val historyRepository = SqliteInvoiceStatusHistoryRepository(connectionFactory)

        if (ownerRepository.findById("OWN-0001") == null) {
            ownerRepository.save(Owner("OWN-0001", "Nadia Prasetyo", "0811111111", "nadia@example.com"))
        }
        if (petRepository.findById("PET-0001") == null) {
            petRepository.save(Pet("PET-0001", "OWN-0001", "Milo", "Cat"))
        }
        if (seedDefaultInvoice && invoiceRepository.findById("INV-0001") == null) {
            invoiceRepository.save(
                Invoice(
                    id = "INV-0001",
                    petId = "PET-0001",
                    items = listOf(InvoiceItem(ClinicService.CONSULTATION)),
                    issuedAt = LocalDateTime.of(2026, 6, 20, 15, 0)
                )
            )
        }

        return Fixture(petRepository, invoiceRepository, historyRepository)
    }

    private fun databaseConfig(): DatabaseConfig = DatabaseConfig(
        databasePath = tempDir.resolve("darcy-vet.db"),
        backupDirectory = tempDir.resolve("backups")
    )

    private data class Fixture(
        val petRepository: SqlitePetRepository,
        val invoiceRepository: SqliteInvoiceRepository,
        val historyRepository: SqliteInvoiceStatusHistoryRepository
    )
}
