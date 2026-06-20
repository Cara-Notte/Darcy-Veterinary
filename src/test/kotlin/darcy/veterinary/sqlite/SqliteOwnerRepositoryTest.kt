package darcy.veterinary.sqlite

import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.exception.DuplicateEntityException
import darcy.veterinary.domain.model.Owner
import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.infrastructure.database.DatabaseMigrator
import darcy.veterinary.infrastructure.sqlite.SqliteOwnerRepository
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SqliteOwnerRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `saves and reloads owner by id`() {
        val repository = ownerRepository()
        val owner = Owner(
            id = "OWN-0001",
            fullName = "Nadia Prasetyo",
            phoneNumber = "0811111111",
            email = "nadia@example.com"
        )

        repository.save(owner)

        assertEquals(owner, repository.findById("OWN-0001"))
    }

    @Test
    fun `updates existing owner without creating duplicate rows`() {
        val repository = ownerRepository()
        repository.save(Owner("OWN-0001", "Nadia Prasetyo", "0811111111", "nadia@example.com"))
        val updated = Owner("OWN-0001", "Nadia Santoso", "0822222222", null)

        repository.save(updated)

        assertEquals(updated, repository.findById("OWN-0001"))
        assertEquals(listOf(updated), repository.findAll())
    }

    @Test
    fun `find all returns owners in insertion order`() {
        val repository = ownerRepository()
        val first = Owner("OWN-0001", "Nadia Prasetyo", "0811111111", null)
        val second = Owner("OWN-0002", "Bima Hartono", "0822222222", "bima@example.com")

        repository.save(first)
        repository.save(second)

        assertEquals(listOf(first, second), repository.findAll())
    }

    @Test
    fun `search matches name phone and email case insensitively`() {
        val repository = ownerRepository()
        val nadia = Owner("OWN-0001", "Nadia Prasetyo", "0811111111", "nadia@example.com")
        val bima = Owner("OWN-0002", "Bima Hartono", "0822222222", "bima@example.com")
        repository.save(nadia)
        repository.save(bima)

        assertEquals(listOf(nadia), repository.search("nadia"))
        assertEquals(listOf(bima), repository.search("2222"))
        assertEquals(listOf(bima), repository.search("BIMA@EXAMPLE"))
        assertEquals(emptyList(), repository.search("   "))
    }

    @Test
    fun `owner persists after repository is reopened`() {
        val config = databaseConfig()
        val firstRepository = ownerRepository(config)
        val owner = Owner("OWN-0001", "Nadia Prasetyo", "0811111111", "nadia@example.com")

        firstRepository.save(owner)

        val reopenedRepository = ownerRepository(config)
        assertEquals(owner, reopenedRepository.findById("OWN-0001"))
    }

    @Test
    fun `database unique phone constraint rejects duplicates outside service layer`() {
        val repository = ownerRepository()
        repository.save(Owner("OWN-0001", "First Owner", "0811111111", null))

        assertFailsWith<IllegalStateException> {
            repository.save(Owner("OWN-0002", "Second Owner", "0811111111", null))
        }
    }

    @Test
    fun `owner service duplicate phone rule still works with SQLite repository`() {
        val service = OwnerService(ownerRepository(), SequenceIdGenerator())
        service.registerOwner("First Owner", "0811111111")

        assertFailsWith<DuplicateEntityException> {
            service.registerOwner("Second Owner", "0811111111")
        }
    }

    @Test
    fun `returns null for missing owner`() {
        val repository = ownerRepository()

        assertNull(repository.findById("OWN-MISSING"))
    }

    private fun ownerRepository(config: DatabaseConfig = databaseConfig()): SqliteOwnerRepository {
        val connectionFactory = DatabaseConnectionFactory(config)
        DatabaseMigrator(connectionFactory).migrate()
        return SqliteOwnerRepository(connectionFactory)
    }

    private fun databaseConfig(): DatabaseConfig = DatabaseConfig(
        databasePath = tempDir.resolve("darcy-vet.db"),
        backupDirectory = tempDir.resolve("backups")
    )
}
