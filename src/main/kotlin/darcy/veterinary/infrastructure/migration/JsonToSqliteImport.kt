package darcy.veterinary.infrastructure.migration

import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.infrastructure.database.DatabaseMigrator
import darcy.veterinary.infrastructure.sqlite.SqliteAppointmentRepository
import darcy.veterinary.infrastructure.sqlite.SqliteInvoiceRepository
import darcy.veterinary.infrastructure.sqlite.SqliteInvoiceStatusHistoryRepository
import darcy.veterinary.infrastructure.sqlite.SqliteMedicalRecordRepository
import darcy.veterinary.infrastructure.sqlite.SqliteMedicalRecordRevisionRepository
import darcy.veterinary.infrastructure.sqlite.SqliteOwnerRepository
import darcy.veterinary.infrastructure.sqlite.SqlitePetRepository
import darcy.veterinary.infrastructure.storage.JsonClinicStorage
import java.nio.file.Path

class JsonToSqliteImport(
    private val databaseConfig: DatabaseConfig = DatabaseConfig()
) {
    fun run(
        jsonDirectory: Path = Path.of("data"),
        fileName: String = "clinic-data.json"
    ) {
        val connectionFactory = DatabaseConnectionFactory(databaseConfig)
        DatabaseMigrator(connectionFactory).migrate()

        JsonClinicStorage(jsonDirectory, fileName).loadAll(
            ownerRepository = SqliteOwnerRepository(connectionFactory),
            petRepository = SqlitePetRepository(connectionFactory),
            appointmentRepository = SqliteAppointmentRepository(connectionFactory),
            medicalRecordRepository = SqliteMedicalRecordRepository(connectionFactory),
            invoiceRepository = SqliteInvoiceRepository(connectionFactory),
            revisionRepository = SqliteMedicalRecordRevisionRepository(connectionFactory),
            invoiceHistoryRepository = SqliteInvoiceStatusHistoryRepository(connectionFactory)
        )
    }
}
