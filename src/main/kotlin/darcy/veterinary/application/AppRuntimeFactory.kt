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
import darcy.veterinary.infrastructure.storage.NoOpClinicStorage

object AppRuntimeFactory {
    fun sqlite(config: DatabaseConfig = DatabaseConfig()): AppRuntime {
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
        val clinicWorkspaceFacade = ClinicWorkspaceFacade(
            ownerService = ownerService,
            patientService = patientService,
            appointmentService = appointmentService,
            recordService = medicalRecordService,
            billingService = billingService
        )

        return AppRuntime(
            ownerRepository = ownerRepository,
            petRepository = petRepository,
            appointmentRepository = appointmentRepository,
            medicalRecordRepository = medicalRecordRepository,
            invoiceRepository = invoiceRepository,
            medicalRecordRevisionRepository = medicalRecordRevisionRepository,
            invoiceStatusHistoryRepository = invoiceStatusHistoryRepository,
            storage = NoOpClinicStorage,
            ownerService = ownerService,
            patientService = patientService,
            appointmentService = appointmentService,
            medicalRecordService = medicalRecordService,
            billingService = billingService,
            reportService = reportService,
            clinicWorkspaceFacade = clinicWorkspaceFacade
        )
    }
}
