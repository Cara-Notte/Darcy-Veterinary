package darcy.veterinary.infrastructure.database

import java.sql.Connection
import java.time.Instant

class DatabaseMigrator(
    private val connectionFactory: DatabaseConnectionFactory
) {
    fun migrate() {
        connectionFactory.openConnection().use { connection ->
            ensureMigrationTable(connection)
            val appliedVersions = appliedMigrationVersions(connection)

            MIGRATIONS
                .filterNot { migration -> appliedVersions.contains(migration.version) }
                .sortedBy { migration -> migration.version }
                .forEach { migration -> applyMigration(connection, migration) }
        }
    }

    private fun ensureMigrationTable(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    version INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    applied_at TEXT NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    private fun appliedMigrationVersions(connection: Connection): Set<Int> =
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT version FROM schema_migrations").use { result ->
                buildSet {
                    while (result.next()) {
                        add(result.getInt("version"))
                    }
                }
            }
        }

    private fun applyMigration(connection: Connection, migration: DatabaseMigration) {
        val originalAutoCommit = connection.autoCommit
        connection.autoCommit = false

        try {
            executeSqlScript(connection, migration.sql)
            connection.prepareStatement(
                "INSERT INTO schema_migrations (version, name, applied_at) VALUES (?, ?, ?)"
            ).use { statement ->
                statement.setInt(1, migration.version)
                statement.setString(2, migration.name)
                statement.setString(3, Instant.now().toString())
                statement.executeUpdate()
            }
            connection.commit()
        } catch (error: Exception) {
            connection.rollback()
            throw IllegalStateException("Failed to apply database migration ${migration.version}: ${migration.name}", error)
        } finally {
            connection.autoCommit = originalAutoCommit
        }
    }

    private fun executeSqlScript(connection: Connection, sql: String) {
        sql.split(';')
            .map { statement -> statement.trim() }
            .filter { statement -> statement.isNotBlank() }
            .forEach { statementSql ->
                connection.createStatement().use { statement ->
                    statement.execute(statementSql)
                }
            }
    }

    private data class DatabaseMigration(
        val version: Int,
        val name: String,
        val sql: String
    )

    private companion object {
        val MIGRATIONS = listOf(
            DatabaseMigration(
                version = 1,
                name = "create_initial_schema",
                sql = """
                    CREATE TABLE IF NOT EXISTS owners (
                        id TEXT PRIMARY KEY,
                        full_name TEXT NOT NULL,
                        phone_number TEXT NOT NULL UNIQUE,
                        email TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    );

                    CREATE TABLE IF NOT EXISTS pets (
                        id TEXT PRIMARY KEY,
                        owner_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        species TEXT NOT NULL,
                        breed TEXT,
                        age INTEGER,
                        sex TEXT,
                        date_of_birth TEXT,
                        weight_kg REAL,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        FOREIGN KEY (owner_id) REFERENCES owners(id)
                    );

                    CREATE TABLE IF NOT EXISTS pet_allergies (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        pet_id TEXT NOT NULL,
                        allergy TEXT NOT NULL,
                        FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE
                    );

                    CREATE TABLE IF NOT EXISTS pet_medical_conditions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        pet_id TEXT NOT NULL,
                        condition_name TEXT NOT NULL,
                        FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE
                    );

                    CREATE TABLE IF NOT EXISTS appointments (
                        id TEXT PRIMARY KEY,
                        pet_id TEXT NOT NULL,
                        scheduled_at TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        status TEXT NOT NULL,
                        visit_type TEXT NOT NULL,
                        veterinarian_name TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        FOREIGN KEY (pet_id) REFERENCES pets(id)
                    );

                    CREATE TABLE IF NOT EXISTS medical_records (
                        id TEXT PRIMARY KEY,
                        pet_id TEXT NOT NULL,
                        appointment_id TEXT,
                        diagnosis TEXT NOT NULL,
                        treatment TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        recorded_at TEXT NOT NULL,
                        veterinarian_name TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        FOREIGN KEY (pet_id) REFERENCES pets(id),
                        FOREIGN KEY (appointment_id) REFERENCES appointments(id)
                    );

                    CREATE TABLE IF NOT EXISTS medical_record_revisions (
                        id TEXT PRIMARY KEY,
                        record_id TEXT NOT NULL,
                        diagnosis TEXT NOT NULL,
                        treatment TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        changed_at TEXT NOT NULL,
                        FOREIGN KEY (record_id) REFERENCES medical_records(id) ON DELETE CASCADE
                    );

                    CREATE TABLE IF NOT EXISTS invoices (
                        id TEXT PRIMARY KEY,
                        pet_id TEXT NOT NULL,
                        issued_at TEXT NOT NULL,
                        payment_status TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        FOREIGN KEY (pet_id) REFERENCES pets(id)
                    );

                    CREATE TABLE IF NOT EXISTS invoice_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        invoice_id TEXT NOT NULL,
                        service_code TEXT NOT NULL,
                        service_display_name TEXT NOT NULL,
                        price REAL NOT NULL,
                        FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
                    );

                    CREATE TABLE IF NOT EXISTS invoice_status_history (
                        id TEXT PRIMARY KEY,
                        invoice_id TEXT NOT NULL,
                        from_status TEXT,
                        to_status TEXT NOT NULL,
                        changed_at TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
                    );
                """.trimIndent()
            ),
            DatabaseMigration(
                version = 2,
                name = "add_lookup_indexes",
                sql = """
                    CREATE INDEX IF NOT EXISTS idx_pets_owner_id ON pets(owner_id);
                    CREATE INDEX IF NOT EXISTS idx_pets_name ON pets(name);
                    CREATE INDEX IF NOT EXISTS idx_appointments_pet_id ON appointments(pet_id);
                    CREATE INDEX IF NOT EXISTS idx_appointments_scheduled_at ON appointments(scheduled_at);
                    CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status);
                    CREATE INDEX IF NOT EXISTS idx_medical_records_pet_id ON medical_records(pet_id);
                    CREATE INDEX IF NOT EXISTS idx_medical_records_recorded_at ON medical_records(recorded_at);
                    CREATE INDEX IF NOT EXISTS idx_invoices_pet_id ON invoices(pet_id);
                    CREATE INDEX IF NOT EXISTS idx_invoices_issued_at ON invoices(issued_at);
                    CREATE INDEX IF NOT EXISTS idx_invoices_payment_status ON invoices(payment_status);
                    CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_id ON invoice_items(invoice_id);
                    CREATE INDEX IF NOT EXISTS idx_record_revisions_record_id ON medical_record_revisions(record_id);
                    CREATE INDEX IF NOT EXISTS idx_invoice_history_invoice_id ON invoice_status_history(invoice_id);
                """.trimIndent()
            )
        )
    }
}
