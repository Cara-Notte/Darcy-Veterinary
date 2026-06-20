package darcy.veterinary.infrastructure.database

import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DatabaseMigratorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `connection factory creates database parent directory and opens SQLite connection`() {
        val databasePath = tempDir.resolve("nested").resolve("darcy-vet.db")
        val config = DatabaseConfig(databasePath = databasePath, backupDirectory = tempDir.resolve("backups"))

        DatabaseConnectionFactory(config).openConnection().use { connection ->
            assertTrue(connection.isValid(2))
            assertEquals(1, connection.foreignKeyStatus())
        }

        assertTrue(databasePath.parent.toFile().exists())
    }

    @Test
    fun `migration runner creates required Stage 2 tables and indexes`() {
        val databasePath = tempDir.resolve("darcy-vet.db")
        val config = DatabaseConfig(databasePath = databasePath, backupDirectory = tempDir.resolve("backups"))
        val connectionFactory = DatabaseConnectionFactory(config)

        DatabaseMigrator(connectionFactory).migrate()

        connectionFactory.openConnection().use { connection ->
            val tables = connection.objectNames("table")
            val indexes = connection.objectNames("index")

            assertTrue(tables.containsAll(REQUIRED_TABLES), "Missing required tables: ${REQUIRED_TABLES - tables}")
            assertTrue(indexes.containsAll(REQUIRED_INDEXES), "Missing required indexes: ${REQUIRED_INDEXES - indexes}")
            assertEquals(listOf(1, 2), connection.appliedMigrationVersions())
        }
    }

    @Test
    fun `migration runner is idempotent`() {
        val databasePath = tempDir.resolve("darcy-vet.db")
        val config = DatabaseConfig(databasePath = databasePath, backupDirectory = tempDir.resolve("backups"))
        val connectionFactory = DatabaseConnectionFactory(config)
        val migrator = DatabaseMigrator(connectionFactory)

        migrator.migrate()
        migrator.migrate()

        connectionFactory.openConnection().use { connection ->
            assertEquals(listOf(1, 2), connection.appliedMigrationVersions())
        }
    }

    private fun Connection.foreignKeyStatus(): Int =
        createStatement().use { statement ->
            statement.executeQuery("PRAGMA foreign_keys").use { result ->
                result.next()
                result.getInt(1)
            }
        }

    private fun Connection.objectNames(type: String): Set<String> =
        prepareStatement("SELECT name FROM sqlite_master WHERE type = ?").use { statement ->
            statement.setString(1, type)
            statement.executeQuery().use { result ->
                buildSet {
                    while (result.next()) {
                        add(result.getString("name"))
                    }
                }
            }
        }

    private fun Connection.appliedMigrationVersions(): List<Int> =
        createStatement().use { statement ->
            statement.executeQuery("SELECT version FROM schema_migrations ORDER BY version").use { result ->
                buildList {
                    while (result.next()) {
                        add(result.getInt("version"))
                    }
                }
            }
        }

    private companion object {
        val REQUIRED_TABLES = setOf(
            "schema_migrations",
            "owners",
            "pets",
            "pet_allergies",
            "pet_medical_conditions",
            "appointments",
            "medical_records",
            "medical_record_revisions",
            "invoices",
            "invoice_items",
            "invoice_status_history"
        )

        val REQUIRED_INDEXES = setOf(
            "idx_pets_owner_id",
            "idx_pets_name",
            "idx_appointments_pet_id",
            "idx_appointments_scheduled_at",
            "idx_appointments_status",
            "idx_medical_records_pet_id",
            "idx_medical_records_recorded_at",
            "idx_invoices_pet_id",
            "idx_invoices_issued_at",
            "idx_invoices_payment_status",
            "idx_invoice_items_invoice_id",
            "idx_record_revisions_record_id",
            "idx_invoice_history_invoice_id"
        )
    }
}
