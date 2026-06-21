package darcy.veterinary.application

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
import darcy.veterinary.infrastructure.storage.ClinicStorage
import darcy.veterinary.infrastructure.storage.NoOpClinicStorage
import darcy.veterinary.presentation.cli.ConsoleUI
import darcy.veterinary.presentation.cli.InputReader
import darcy.veterinary.repository.AppointmentRepository
import darcy.veterinary.repository.InvoiceRepository
import darcy.veterinary.repository.InvoiceStatusHistoryRepository
import darcy.veterinary.repository.MedicalRecordRepository
import darcy.veterinary.repository.MedicalRecordRevisionRepository
import darcy.veterinary.repository.OwnerRepository
import darcy.veterinary.repository.PetRepository

data class CliRuntime(
    val ownerRepository: OwnerRepository,
    val petRepository: PetRepository,
    val appointmentRepository: AppointmentRepository,
    val medicalRecordRepository: MedicalRecordRepository,
    val invoiceRepository: InvoiceRepository,
    val medicalRecordRevisionRepository: MedicalRecordRevisionRepository,
    val invoiceStatusHistoryRepository: InvoiceStatusHistoryRepository,
    val storage: ClinicStorage,
    val consoleUI: ConsoleUI
)

object CliRuntimeFactory {
    fun sqlite(
        config: DatabaseConfig = DatabaseConfig(),
        input: InputReader = InputReader()
    ): CliRuntime {
        val connectionFactory = DatabaseConnectionFactory(config)
        DatabaseMigrator(connectionFactory).migrate()

        val ownerRepository = SqliteOwnerRepository(connectionFactory)
        val petRepository = SqlitePetRepository(connectionFactory)
        val appointmentRepository = SqliteAppointmentRepository(connectionFactory)
        val medicalRecordRepository = SqliteMedicalRecordRepository(connectionFactory)
        val invoiceRepository = SqliteInvoiceRepository(connectionFactory)
        val medicalRecordRevisionRepository = SqliteMedicalRecordRevisionRepository(connectionFactory)
        val invoiceStatusHistoryRepository = SqliteInvoiceStatusHistoryRepository(connectionFactory)

        val ownerService = OwnerService(ownerRepository)
        val patientService = PatientService(petRepository, ownerRepository)
        val appointmentService = AppointmentService(appointmentRepository, petRepository)
        val medicalRecordService = MedicalRecordService(
            medicalRecordRepository,
            petRepository,
            appointmentRepository,
            revisionRepository = medicalRecordRevisionRepository
        )
        val billingService = BillingService(
            invoiceRepository,
            petRepository,
            statusHistoryRepository = invoiceStatusHistoryRepository
        )
        val reportService = ClinicReportService(ownerRepository, petRepository, appointmentRepository, invoiceRepository)
        val storage = NoOpClinicStorage

        val consoleUI = ConsoleUI(
            ownerRepository = ownerRepository,
            petRepository = petRepository,
            appointmentRepository = appointmentRepository,
            medicalRecordRepository = medicalRecordRepository,
            invoiceRepository = invoiceRepository,
            medicalRecordRevisionRepository = medicalRecordRevisionRepository,
            invoiceStatusHistoryRepository = invoiceStatusHistoryRepository,
            ownerService = ownerService,
            patientService = patientService,
            appointmentService = appointmentService,
            medicalRecordService = medicalRecordService,
            billingService = billingService,
            reportService = reportService,
            storage = storage,
            input = input
        )

        return CliRuntime(
            ownerRepository = ownerRepository,
            petRepository = petRepository,
            appointmentRepository = appointmentRepository,
            medicalRecordRepository = medicalRecordRepository,
            invoiceRepository = invoiceRepository,
            medicalRecordRevisionRepository = medicalRecordRevisionRepository,
            invoiceStatusHistoryRepository = invoiceStatusHistoryRepository,
            storage = storage,
            consoleUI = consoleUI
        )
    }
}
