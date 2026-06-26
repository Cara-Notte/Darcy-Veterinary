package darcy.veterinary.application

import darcy.veterinary.infrastructure.storage.ClinicStorage
import darcy.veterinary.repository.AppointmentRepository
import darcy.veterinary.repository.InvoiceRepository
import darcy.veterinary.repository.InvoiceStatusHistoryRepository
import darcy.veterinary.repository.MedicalRecordRepository
import darcy.veterinary.repository.MedicalRecordRevisionRepository
import darcy.veterinary.repository.OwnerRepository
import darcy.veterinary.repository.PetRepository

data class AppRuntime(
    val ownerRepository: OwnerRepository,
    val petRepository: PetRepository,
    val appointmentRepository: AppointmentRepository,
    val medicalRecordRepository: MedicalRecordRepository,
    val invoiceRepository: InvoiceRepository,
    val medicalRecordRevisionRepository: MedicalRecordRevisionRepository,
    val invoiceStatusHistoryRepository: InvoiceStatusHistoryRepository,
    val storage: ClinicStorage,
    val ownerService: OwnerService,
    val patientService: PatientService,
    val appointmentService: AppointmentService,
    val medicalRecordService: MedicalRecordService,
    val billingService: BillingService,
    val reportService: ClinicReportService,
    val adminMaintenanceService: AdminMaintenanceService,
    val clinicWorkspaceFacade: ClinicWorkspaceFacade,
    val appointmentBoardFacade: AppointmentBoardFacade
)
