package darcy.veterinary

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.BillingService
import darcy.veterinary.application.ClinicReportService
import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceStatusHistoryRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRevisionRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import darcy.veterinary.presentation.cli.ConsoleUI

fun main() {
    val ownerRepository = InMemoryOwnerRepository()
    val petRepository = InMemoryPetRepository()
    val appointmentRepository = InMemoryAppointmentRepository()
    val medicalRecordRepository = InMemoryMedicalRecordRepository()
    val invoiceRepository = InMemoryInvoiceRepository()
    val medicalRecordRevisionRepository = InMemoryMedicalRecordRevisionRepository()
    val invoiceStatusHistoryRepository = InMemoryInvoiceStatusHistoryRepository()

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

    ConsoleUI(
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
        reportService = reportService
    ).start()
}
