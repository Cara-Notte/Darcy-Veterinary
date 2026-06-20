package darcy.veterinary.sqlite

import darcy.veterinary.application.BillingService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.exception.EntityNotFoundException
import darcy.veterinary.domain.exception.InvalidClinicOperationException
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.Invoice
import darcy.veterinary.domain.model.InvoiceItem
import darcy.veterinary.domain.model.Owner
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.domain.model.Pet
import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.infrastructure.database.DatabaseMigrator
import darcy.veterinary.infrastructure.sqlite.SqliteInvoiceRepository
import darcy.veterinary.infrastructure.sqlite.SqliteOwnerRepository
import darcy.veterinary.infrastructure.sqlite.SqlitePetRepository
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SqliteInvoiceRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `saves and reloads invoice with multiple line items`() {
        val fixture = fixture()
        val invoice = Invoice(
            id = "INV-0001",
            petId = "PET-0001",
            items = listOf(
                InvoiceItem(ClinicService.CONSULTATION),
                InvoiceItem(ClinicService.VACCINATION, description = "Rabies vaccination", cost = 275_000.0)
            ),
            issuedAt = LocalDateTime.of(2026, 6, 20, 15, 0),
            paymentStatus = PaymentStatus.UNPAID
        )

        fixture.invoiceRepository.save(invoice)

        assertEquals(invoice, fixture.invoiceRepository.findById("INV-0001"))
        assertEquals(375_000.0, fixture.invoiceRepository.findById("INV-0001")?.total())
    }

    @Test
    fun `updates invoice header and replaces line items transactionally`() {
        val fixture = fixture()
        val original = Invoice(
            id = "INV-0001",
            petId = "PET-0001",
            items = listOf(InvoiceItem(ClinicService.CONSULTATION), InvoiceItem(ClinicService.BASIC_TREATMENT)),
            issuedAt = LocalDateTime.of(2026, 6, 20, 15, 0),
            paymentStatus = PaymentStatus.UNPAID
        )
        val updated = original.copy(
            items = listOf(InvoiceItem(ClinicService.EXTENDED_CARE, description = "Extended overnight care", cost = 550_000.0)),
            paymentStatus = PaymentStatus.PAID
        )

        fixture.invoiceRepository.save(original)
        fixture.invoiceRepository.save(updated)

        assertEquals(updated, fixture.invoiceRepository.findById("INV-0001"))
        assertEquals(listOf(updated), fixture.invoiceRepository.findAll())
        assertEquals(550_000.0, fixture.invoiceRepository.findById("INV-0001")?.total())
    }

    @Test
    fun `find all returns invoices in insertion order`() {
        val fixture = fixture()
        val first = Invoice(
            id = "INV-0001",
            petId = "PET-0001",
            items = listOf(InvoiceItem(ClinicService.CONSULTATION)),
            issuedAt = LocalDateTime.of(2026, 6, 20, 15, 0)
        )
        val second = Invoice(
            id = "INV-0002",
            petId = "PET-0001",
            items = listOf(InvoiceItem(ClinicService.GROOMING)),
            issuedAt = LocalDateTime.of(2026, 6, 21, 15, 0),
            paymentStatus = PaymentStatus.PAID
        )

        fixture.invoiceRepository.save(first)
        fixture.invoiceRepository.save(second)

        assertEquals(listOf(first, second), fixture.invoiceRepository.findAll())
    }

    @Test
    fun `find by pet id returns only matching invoices`() {
        val fixture = fixture()
        fixture.petRepository.save(Pet("PET-0002", "OWN-0001", "Luna", "Cat"))
        val first = Invoice("INV-0001", "PET-0001", listOf(InvoiceItem(ClinicService.CONSULTATION)), LocalDateTime.of(2026, 6, 20, 15, 0))
        val second = Invoice("INV-0002", "PET-0001", listOf(InvoiceItem(ClinicService.GROOMING)), LocalDateTime.of(2026, 6, 21, 15, 0))
        val third = Invoice("INV-0003", "PET-0002", listOf(InvoiceItem(ClinicService.VACCINATION)), LocalDateTime.of(2026, 6, 22, 15, 0))

        fixture.invoiceRepository.save(first)
        fixture.invoiceRepository.save(second)
        fixture.invoiceRepository.save(third)

        assertEquals(listOf(first, second), fixture.invoiceRepository.findByPetId("PET-0001"))
        assertEquals(listOf(third), fixture.invoiceRepository.findByPetId("PET-0002"))
    }

    @Test
    fun `invoice persists after repository is reopened`() {
        val config = databaseConfig()
        val firstFixture = fixture(config)
        val invoice = Invoice(
            id = "INV-0001",
            petId = "PET-0001",
            items = listOf(
                InvoiceItem(ClinicService.CONSULTATION),
                InvoiceItem(ClinicService.BASIC_TREATMENT, description = "Custom treatment", cost = 90_000.0)
            ),
            issuedAt = LocalDateTime.of(2026, 6, 20, 15, 0),
            paymentStatus = PaymentStatus.VOIDED
        )

        firstFixture.invoiceRepository.save(invoice)

        val reopenedFixture = fixture(config, seedDefaultPet = false)
        assertEquals(invoice, reopenedFixture.invoiceRepository.findById("INV-0001"))
    }

    @Test
    fun `returns null for missing invoice`() {
        val fixture = fixture()

        assertNull(fixture.invoiceRepository.findById("INV-MISSING"))
    }

    @Test
    fun `database foreign key rejects invoice for missing pet outside service layer`() {
        val fixture = fixture(seedDefaultPet = false)
        val invoice = Invoice(
            id = "INV-0001",
            petId = "PET-MISSING",
            items = listOf(InvoiceItem(ClinicService.CONSULTATION)),
            issuedAt = LocalDateTime.of(2026, 6, 20, 15, 0)
        )

        assertFailsWith<IllegalStateException> {
            fixture.invoiceRepository.save(invoice)
        }
    }

    @Test
    fun `billing service works with SQLite invoice repository`() {
        val fixture = fixture()
        val service = BillingService(
            invoiceRepository = fixture.invoiceRepository,
            petRepository = fixture.petRepository,
            idGenerator = SequenceIdGenerator()
        )

        val created = service.createInvoice(
            petId = "PET-0001",
            services = listOf(ClinicService.CONSULTATION, ClinicService.GROOMING),
            issuedAt = LocalDateTime.of(2026, 6, 20, 15, 0)
        )
        val paid = service.markAsPaid(created.id)

        assertEquals("INV-0001", created.id)
        assertEquals(PaymentStatus.UNPAID, created.paymentStatus)
        assertEquals(250_000.0, created.total())
        assertEquals(PaymentStatus.PAID, paid.paymentStatus)
        assertEquals(created.items, paid.items)
        assertFailsWith<EntityNotFoundException> {
            service.createInvoice("PET-MISSING", listOf(ClinicService.CONSULTATION))
        }
        assertFailsWith<InvalidClinicOperationException> {
            service.createInvoice("PET-0001", emptyList())
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
        val invoiceRepository = SqliteInvoiceRepository(connectionFactory)

        if (seedDefaultPet && ownerRepository.findById("OWN-0001") == null) {
            ownerRepository.save(Owner("OWN-0001", "Nadia Prasetyo", "0811111111", "nadia@example.com"))
        }
        if (seedDefaultPet && petRepository.findById("PET-0001") == null) {
            petRepository.save(Pet("PET-0001", "OWN-0001", "Milo", "Cat"))
        }

        return Fixture(petRepository, invoiceRepository)
    }

    private fun databaseConfig(): DatabaseConfig = DatabaseConfig(
        databasePath = tempDir.resolve("darcy-vet.db"),
        backupDirectory = tempDir.resolve("backups")
    )

    private data class Fixture(
        val petRepository: SqlitePetRepository,
        val invoiceRepository: SqliteInvoiceRepository
    )
}
