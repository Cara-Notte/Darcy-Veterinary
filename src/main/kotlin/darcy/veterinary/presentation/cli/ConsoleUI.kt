package darcy.veterinary.presentation.cli

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.BillingService
import darcy.veterinary.application.ClinicReportService
import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.infrastructure.storage.ClinicStorage
import darcy.veterinary.infrastructure.storage.JsonClinicStorage
import darcy.veterinary.repository.AppointmentRepository
import darcy.veterinary.repository.InvoiceRepository
import darcy.veterinary.repository.InvoiceStatusHistoryRepository
import darcy.veterinary.repository.MedicalRecordRepository
import darcy.veterinary.repository.MedicalRecordRevisionRepository
import darcy.veterinary.repository.OwnerRepository
import darcy.veterinary.repository.PetRepository

class ConsoleUI(
    private val ownerRepository: OwnerRepository,
    private val petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
    private val medicalRecordRepository: MedicalRecordRepository,
    private val invoiceRepository: InvoiceRepository,
    private val medicalRecordRevisionRepository: MedicalRecordRevisionRepository? = null,
    private val invoiceStatusHistoryRepository: InvoiceStatusHistoryRepository? = null,
    private val ownerService: OwnerService,
    private val patientService: PatientService,
    private val appointmentService: AppointmentService,
    private val medicalRecordService: MedicalRecordService,
    private val billingService: BillingService,
    private val reportService: ClinicReportService,
    private val storage: ClinicStorage = JsonClinicStorage(),
    private val input: InputReader = InputReader()
) {
    private val patientMenu = PatientMenu(ownerService, patientService, input)
    private val appointmentMenu = AppointmentMenu(appointmentService, patientService, input)
    private val medicalRecordMenu = MedicalRecordMenu(medicalRecordService, patientService, appointmentService, input)
    private val billingMenu = BillingMenu(billingService, patientService, input)
    private val reportMenu = ReportMenu(reportService, input)

    fun start() {
        storage.loadAll(
            ownerRepository,
            petRepository,
            appointmentRepository,
            medicalRecordRepository,
            invoiceRepository,
            medicalRecordRevisionRepository,
            invoiceStatusHistoryRepository
        )
        println("Darcy Vet Clinic Management")

        var running = true
        while (running) {
            try {
                println("\nMain Menu")
                println("1. Patients and owners")
                println("2. Appointments")
                println("3. Medical records")
                println("4. Billing")
                println("5. Reports")
                println("6. Save and exit")
                when (input.choice("Choose menu: ", 1..6)) {
                    1 -> patientMenu.show()
                    2 -> appointmentMenu.show()
                    3 -> medicalRecordMenu.show()
                    4 -> billingMenu.show()
                    5 -> reportMenu.show()
                    6 -> running = false
                }
            } catch (_: EndOfInputException) {
                println("No console input is available. Exiting.")
                running = false
            } catch (error: Exception) {
                println("Error: ${error.message}")
            }
        }

        storage.saveAll(
            ownerRepository,
            petRepository,
            appointmentRepository,
            medicalRecordRepository,
            invoiceRepository,
            medicalRecordRevisionRepository,
            invoiceStatusHistoryRepository
        )
        println("Data saved. Goodbye.")
    }
}
