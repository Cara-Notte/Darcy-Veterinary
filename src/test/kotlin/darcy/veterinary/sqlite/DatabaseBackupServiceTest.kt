package darcy.veterinary.sqlite

import darcy.veterinary.domain.model.Owner
import darcy.veterinary.infrastructure.database.DatabaseBackupService
import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.infrastructure.database.DatabaseMigrator
import darcy.veterinary.infrastructure.sqlite.SqliteOwnerRepository
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DatabaseBackupServiceTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `creates timestamped backup file in configured backup directory`() {
        val config = databaseConfig()
        seedOwner(config, Owner("OWN-0001", "Nadia Prasetyo", "0811111111", "nadia@example.com"))
        val service = backupService(config)

        val backupPath = service.createBackup()

        assertEquals(config.backupDirectory.resolve("darcy-vet-20260621-153000.db"), backupPath)
        assertTrue(Files.exists(backupPath))
        assertTrue(Files.size(backupPath) > 0)
    }

    @Test
    fun `restore replaces current database with selected backup`() {
        val config = databaseConfig()
        val originalOwner = Owner("OWN-0001", "Nadia Prasetyo", "0811111111", "nadia@example.com")
        val replacementOwner = Owner("OWN-0001", "Updated Owner", "0899999999", "updated@example.com")
        seedOwner(config, originalOwner)
        val backupPath = backupService(config).createBackup()

        seedOwner(config, replacementOwner)
        assertEquals(replacementOwner, ownerRepository(config).findById("OWN-0001"))

        backupService(config).restoreFrom(backupPath)

        assertEquals(originalOwner, ownerRepository(config).findById("OWN-0001"))
    }

    @Test
    fun `backup directory is created when missing`() {
        val config = databaseConfig()
        seedOwner(config, Owner("OWN-0001", "Nadia Prasetyo", "0811111111", null))
        assertTrue(Files.notExists(config.backupDirectory))

        val backupPath = backupService(config).createBackup()

        assertTrue(Files.exists(config.backupDirectory))
        assertTrue(Files.exists(backupPath))
    }

    @Test
    fun `creating backup fails when database file is missing`() {
        val config = databaseConfig()

        assertFailsWith<IllegalArgumentException> {
            backupService(config).createBackup()
        }
    }

    @Test
    fun `restore fails when backup file is missing`() {
        val config = databaseConfig()

        assertFailsWith<IllegalArgumentException> {
            backupService(config).restoreFrom(tempDir.resolve("missing.db"))
        }
    }

    private fun seedOwner(config: DatabaseConfig, owner: Owner) {
        DatabaseMigrator(DatabaseConnectionFactory(config)).migrate()
        ownerRepository(config).save(owner)
    }

    private fun ownerRepository(config: DatabaseConfig): SqliteOwnerRepository =
        SqliteOwnerRepository(DatabaseConnectionFactory(config))

    private fun backupService(config: DatabaseConfig): DatabaseBackupService = DatabaseBackupService(
        config = config,
        clock = Clock.fixed(Instant.parse("2026-06-21T15:30:00Z"), ZoneOffset.UTC)
    )

    private fun databaseConfig(): DatabaseConfig = DatabaseConfig(
        databasePath = tempDir.resolve("data/darcy-vet.db"),
        backupDirectory = tempDir.resolve("data/backups")
    )
}
