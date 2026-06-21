package darcy.veterinary.infrastructure.database

import java.nio.file.Files
import java.sql.Connection

class DatabaseHealthCheckService(
    private val config: DatabaseConfig = DatabaseConfig(),
    private val connectionFactory: DatabaseConnectionFactory = DatabaseConnectionFactory(config)
) {
    fun check(): DatabaseHealthReport {
        val checks = mutableListOf<DatabaseHealthCheck>()

        checks += checkResult("database file exists") {
            Files.exists(config.databasePath)
        }

        try {
            connectionFactory.openConnection().use { connection ->
                checks += checkResult("sqlite connection opens") { true }
                checks += checkResult("foreign keys enabled") { connection.foreignKeysEnabled() }
                checks += checkResult("integrity check passes") { connection.integrityCheckPasses() }
                checks += checkResult("schema migrations table exists") { connection.tableExists("schema_migrations") }
                checks += checkResult("all required tables exist") {
                    REQUIRED_TABLES.all { tableName -> connection.tableExists(tableName) }
                }
                checks += checkResult("all required migrations applied") { connection.appliedMigrationVersions().containsAll(REQUIRED_MIGRATIONS) }
            }
        } catch (error: Exception) {
            checks += DatabaseHealthCheck(
                name = "sqlite connection opens",
                passed = false,
                message = error.message ?: error::class.simpleName ?: "Unknown database error"
            )
        }

        return DatabaseHealthReport(checks)
    }

    private fun checkResult(name: String, block: () -> Boolean): DatabaseHealthCheck = try {
        val passed = block()
        DatabaseHealthCheck(
            name = name,
            passed = passed,
            message = if (passed) "OK" else "Failed"
        )
    } catch (error: Exception) {
        DatabaseHealthCheck(
            name = name,
            passed = false,
            message = error.message ?: error::class.simpleName ?: "Unknown database error"
        )
    }

    private fun Connection.foreignKeysEnabled(): Boolean =
        createStatement().use { statement ->
            statement.executeQuery("PRAGMA foreign_keys").use { result ->
                result.next() && result.getInt(1) == 1
            }
        }

    private fun Connection.integrityCheckPasses(): Boolean =
        createStatement().use { statement ->
            statement.executeQuery("PRAGMA integrity_check").use { result ->
                result.next() && result.getString(1).equals("ok", ignoreCase = true)
            }
        }

    private fun Connection.tableExists(tableName: String): Boolean =
        prepareStatement(
            """
            SELECT name
            FROM sqlite_master
            WHERE type = 'table' AND name = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, tableName)
            statement.executeQuery().use { result -> result.next() }
        }

    private fun Connection.appliedMigrationVersions(): Set<Int> =
        createStatement().use { statement ->
            statement.executeQuery("SELECT version FROM schema_migrations").use { result ->
                buildSet {
                    while (result.next()) {
                        add(result.getInt("version"))
                    }
                }
            }
        }

    private companion object {
        private val REQUIRED_MIGRATIONS = setOf(1, 2)
        private val REQUIRED_TABLES = setOf(
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
    }
}

data class DatabaseHealthReport(
    val checks: List<DatabaseHealthCheck>
) {
    val healthy: Boolean = checks.all { it.passed }
}

data class DatabaseHealthCheck(
    val name: String,
    val passed: Boolean,
    val message: String
)
