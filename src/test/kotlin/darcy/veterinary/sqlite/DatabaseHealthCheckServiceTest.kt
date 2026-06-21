package darcy.veterinary.sqlite

import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.infrastructure.database.DatabaseHealthCheckService
import darcy.veterinary.infrastructure.database.DatabaseMigrator
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DatabaseHealthCheckServiceTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `healthy migrated database passes all checks`() {
        val config = databaseConfig()
        DatabaseMigrator(DatabaseConnectionFactory(config)).migrate()

        val report = DatabaseHealthCheckService(config).check()

        assertTrue(report.healthy)
        assertEquals(
            setOf(
                "database file exists",
                "sqlite connection opens",
                "foreign keys enabled",
                "integrity check passes",
                "schema migrations table exists",
                "all required tables exist",
                "all required migrations applied"
            ),
            report.checks.map { it.name }.toSet()
        )
        assertTrue(report.checks.all { it.message == "OK" })
    }

    private fun databaseConfig(): DatabaseConfig = DatabaseConfig(
        databasePath = tempDir.resolve("data/darcy-vet.db"),
        backupDirectory = tempDir.resolve("data/backups")
    )
}
