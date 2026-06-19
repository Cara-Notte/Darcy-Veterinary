package darcy.veterinary

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.BillingService
import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.exception.DuplicateEntityException
import darcy.veterinary.domain.exception.InvalidClinicOperationException
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import darcy.veterinary.infrastructure.storage.JsonClinicStorage
import java.nio.file.Files
import java.time.LocalDateTime
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ClinicEdgeCaseTest {
    @Test
    fun `owner update cannot introduce duplicate phone number`() {
        val fixture = fixture()
        val firstOwner = fixture.ownerService.registerOwner("First Owner", "0811111111")
        fixture.ownerService.registerOwner("Second Owner", "0822222222")

        assertFailsWith<DuplicateEntityException> {
            fixture.ownerService.updateOwner(
                id = firstOwner.id,
                fullName = "First Owner",
                phoneNumber = "0822222222"
            )
        }
    }

    @Test
    fun `cancelled appointment cannot be completed`() {
        val fixture = fixture()
        val pet = fixture.registerPet("Cancelled Pet")
        val appointment = fixture.appointmentService.scheduleAppointment(
            pet.id,
            LocalDateTime.of(2026, 6, 23, 9, 0),
            "Checkup"
        )
        fixture.appointmentService.cancelAppointment(appointment.id)

        assertFailsWith<InvalidClinicOperationException> {
            fixture.appointmentService.completeAppointment(appointment.id)
        }
    }

    @Test
    fun `medical record cannot link appointment from another pet`() {
        val fixture = fixture()
        val firstPet = fixture.registerPet("First Pet")
        val secondPet = fixture.registerPet("Second Pet")
        val appointmentForSecondPet = fixture.appointmentService.scheduleAppointment(
            secondPet.id,
            LocalDateTime.of(2026, 6, 23, 10, 0),
            "Second pet checkup"
        )

        assertFailsWith<InvalidClinicOperationException> {
            fixture.medicalRecordService.createRecord(
                petId = firstPet.id,
                appointmentId = appointmentForSecondPet.id,
                diagnosis = "Wrong link",
                treatment = "Should fail",
                notes = "Appointment belongs to another pet"
            )
        }
    }

    @Test
    fun `voided invoice cannot be marked as paid`() {
        val fixture = fixture()
        val pet = fixture.registerPet("Invoice Pet")
        val invoice = fixture.billingService.createInvoice(pet.id, listOf(ClinicService.CONSULTATION))
        fixture.billingService.voidInvoice(invoice.id)

        assertFailsWith<InvalidClinicOperationException> {
            fixture.billingService.markAsPaid(invoice.id)
        }
    }

    @Test
    fun `json reload preserves voided invoice status`() {
        val fixture = fixture()
        val pet = fixture.registerPet("Voided Storage Pet")
        val invoice = fixture.billingService.createInvoice(pet.id, listOf(ClinicService.CONSULTATION))
        fixture.billingService.voidInvoice(invoice.id)

        val directory = Files.createTempDirectory("darcy-edge-json-test")
        JsonClinicStorage(directory).saveAll(
            fixture.ownerRepository,
            fixture.petRepository,
            fixture.appointmentRepository,
            fixture.medicalRecordRepository,
            fixture.invoiceRepository,
            null,
            null
        )

        val loadedOwners = InMemoryOwnerRepository()
        val loadedPets = InMemoryPetRepository()
        val loadedAppointments = InMemoryAppointmentRepository()
        val loadedRecords = InMemoryMedicalRecordRepository()
        val loadedInvoices = InMemoryInvoiceRepository()

        JsonClinicStorage(directory).loadAll(
            loadedOwners,
            loadedPets,
            loadedAppointments,
            loadedRecords,
            loadedInvoices,
            null,
            null
        )

        assertEquals(PaymentStatus.VOIDED, loadedInvoices.findAll().first().paymentStatus)
    }

    private fun fixture(): Fixture {
        val ids = SequenceIdGenerator()
        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val appointmentRepository = InMemoryAppointmentRepository()
        val medicalRecordRepository = InMemoryMedicalRecordRepository()
        val invoiceRepository = InMemoryInvoiceRepository()

        return Fixture(
            ownerRepository = ownerRepository,
            petRepository = petRepository,
            appointmentRepository = appointmentRepository,
            medicalRecordRepository = medicalRecordRepository,
            invoiceRepository = invoiceRepository,
            ownerService = OwnerService(ownerRepository, ids),
            patientService = PatientService(petRepository, ownerRepository, ids),
            appointmentService = AppointmentService(appointmentRepository, petRepository, ids),
            medicalRecordService = MedicalRecordService(medicalRecordRepository, petRepository, appointmentRepository, ids),
            billingService = BillingService(invoiceRepository, petRepository, ids)
        )
    }

    private data class Fixture(
        val ownerRepository: InMemoryOwnerRepository,
        val petRepository: InMemoryPetRepository,
        val appointmentRepository: InMemoryAppointmentRepository,
        val medicalRecordRepository: InMemoryMedicalRecordRepository,
        val invoiceRepository: InMemoryInvoiceRepository,
        val ownerService: OwnerService,
        val patientService: PatientService,
        val appointmentService: AppointmentService,
        val medicalRecordService: MedicalRecordService,
        val billingService: BillingService
    ) {
        fun registerPet(name: String) = patientService.registerPet(
            ownerId = ownerService.registerOwner("Owner for $name", "08${name.hashCode().toString().filter(Char::isDigit).take(8).padEnd(8, '0')}").id,
            name = name,
            species = "Dog"
        )
    }
}
