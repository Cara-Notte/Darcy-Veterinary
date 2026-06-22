package darcy.veterinary.presentation.desktop

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.BillingService
import darcy.veterinary.application.ClinicReportService
import darcy.veterinary.application.ClinicWorkspaceFacade
import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
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
import darcy.veterinary.presentation.desktop.viewmodel.PatientSearchViewModel

data class DesktopRuntime(
    val ownerService: OwnerService,
    val patientService: PatientService,
    val appointmentService: AppointmentService,
    val recordService: MedicalRecordService,
    val billingService: BillingService,
    val reportService: ClinicReportService,
    val workspaceFacade: ClinicWorkspaceFacade,
    val patientSearchViewModel: PatientSearchViewModel
)

object DesktopRuntimeFactory {
    fun sqlite(config: DatabaseConfig = DatabaseConfig()): DesktopRuntime {
        val connectionFactory = DatabaseConnectionFactory(config)
        DatabaseMigrator(connectionFactory).migrate()

        val ownerRepository = SqliteOwnerRepository(connectionFactory)
        val petRepository = SqlitePetRepository(connectionFactory)
        val appointmentRepository = SqliteAppointmentRepository(connectionFactory)
        val recordRepository = SqliteMedicalRecordRepository(connectionFactory)
        val invoiceRepository = SqliteInvoiceRepository(connectionFactory)
        val revisionRepository = SqliteMedicalRecordRevisionRepository(connectionFactory)
        val invoiceHistoryRepository = SqliteInvoiceStatusHistoryRepository(connectionFactory)

        val ownerService = OwnerService(ownerRepository)
        val patientService = PatientService(petRepository, ownerRepository)
        val appointmentService = AppointmentService(appointmentRepository, petRepository)
        val recordService = MedicalRecordService(
            recordRepository,
            petRepository,
            appointmentRepository,
            revisionRepository = revisionRepository
        )
        val billingService = BillingService(
            invoiceRepository,
            petRepository,
            statusHistoryRepository = invoiceHistoryRepository
        )
        val reportService = ClinicReportService(ownerRepository, petRepository, appointmentRepository, invoiceRepository)
        val workspaceFacade = ClinicWorkspaceFacade(
            ownerService = ownerService,
            patientService = patientService,
            appointmentService = appointmentService,
            recordService = recordService,
            billingService = billingService
        )

        return DesktopRuntime(
            ownerService = ownerService,
            patientService = patientService,
            appointmentService = appointmentService,
            recordService = recordService,
            billingService = billingService,
            reportService = reportService,
            workspaceFacade = workspaceFacade,
            patientSearchViewModel = PatientSearchViewModel(workspaceFacade)
        )
    }
}
