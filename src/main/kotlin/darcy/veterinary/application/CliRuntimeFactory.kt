package darcy.veterinary.application

import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.infrastructure.storage.ClinicStorage
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
        val appRuntime = AppRuntimeFactory.sqlite(config)

        val consoleUI = ConsoleUI(
            ownerRepository = appRuntime.ownerRepository,
            petRepository = appRuntime.petRepository,
            appointmentRepository = appRuntime.appointmentRepository,
            medicalRecordRepository = appRuntime.medicalRecordRepository,
            invoiceRepository = appRuntime.invoiceRepository,
            medicalRecordRevisionRepository = appRuntime.medicalRecordRevisionRepository,
            invoiceStatusHistoryRepository = appRuntime.invoiceStatusHistoryRepository,
            ownerService = appRuntime.ownerService,
            patientService = appRuntime.patientService,
            appointmentService = appRuntime.appointmentService,
            medicalRecordService = appRuntime.medicalRecordService,
            billingService = appRuntime.billingService,
            reportService = appRuntime.reportService,
            storage = appRuntime.storage,
            input = input
        )

        return CliRuntime(
            ownerRepository = appRuntime.ownerRepository,
            petRepository = appRuntime.petRepository,
            appointmentRepository = appRuntime.appointmentRepository,
            medicalRecordRepository = appRuntime.medicalRecordRepository,
            invoiceRepository = appRuntime.invoiceRepository,
            medicalRecordRevisionRepository = appRuntime.medicalRecordRevisionRepository,
            invoiceStatusHistoryRepository = appRuntime.invoiceStatusHistoryRepository,
            storage = appRuntime.storage,
            consoleUI = consoleUI
        )
    }
}
