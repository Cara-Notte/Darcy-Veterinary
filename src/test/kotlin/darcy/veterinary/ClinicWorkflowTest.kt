package darcy.veterinary

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.BillingService
import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.exception.DuplicateEntityException
import darcy.veterinary.domain.exception.EntityNotFoundException
import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import java.time.LocalDateTime
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ClinicWorkflowTest {
    private fun fixture(): Fixture {
        val ids = SequenceIdGenerator()
        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val appointmentRepository = InMemoryAppointmentRepository()
        val medicalRecordRepository = InMemoryMedicalRecordRepository()
        val invoiceRepository = InMemoryInvoiceRepository()

        return Fixture(
            ownerService = OwnerService(ownerRepository, ids),
            patientService = PatientService(petRepository, ownerRepository, ids),
            appointmentService = AppointmentService(appointmentRepository, petRepository, ids),
            medicalRecordService = MedicalRecordService(medicalRecordRepository, petRepository, appointmentRepository, ids),
            billingService = BillingService(invoiceRepository, petRepository, ids)
        )
    }

    @Test
    fun `owner and pet registration creates linked records`() {
        val app = fixture()

        val owner = app.ownerService.registerOwner("Nadia Prasetyo", "0811111111", "nadia@example.com")
        val pet = app.patientService.registerPet(owner.id, "Milo", "Cat", "Persian", 2)

        assertEquals("OWN-0001", owner.id)
        assertEquals("PET-0001", pet.id)
        assertEquals(owner.id, pet.ownerId)
        assertEquals(listOf(pet), app.patientService.listPetsByOwner(owner.id))
    }

    @Test
    fun `duplicate owner phone number is rejected`() {
        val app = fixture()

        app.ownerService.registerOwner("First Owner", "0822222222")

        assertFailsWith<DuplicateEntityException> {
            app.ownerService.registerOwner("Second Owner", "0822222222")
        }
    }

    @Test
    fun `pet cannot be registered for missing owner`() {
        val app = fixture()

        assertFailsWith<EntityNotFoundException> {
            app.patientService.registerPet("OWN-MISSING", "Ghost", "Dog")
        }
    }

    @Test
    fun `appointment can be scheduled and completed for an existing pet`() {
        val app = fixture()
        val pet = app.registerDefaultPet()
        val schedule = LocalDateTime.of(2026, 6, 17, 10, 30)

        val appointment = app.appointmentService.scheduleAppointment(pet.id, schedule, "Annual vaccination")
        val completed = app.appointmentService.completeAppointment(appointment.id)

        assertEquals("APT-0001", appointment.id)
        assertEquals(AppointmentStatus.COMPLETED, completed.status)
    }

    @Test
    fun `medical record requires existing pet`() {
        val app = fixture()

        assertFailsWith<EntityNotFoundException> {
            app.medicalRecordService.createRecord(
                petId = "PET-MISSING",
                diagnosis = "Healthy",
                treatment = "Observation",
                notes = "No issue"
            )
        }
    }

    @Test
    fun `medical record can be attached to completed appointment`() {
        val app = fixture()
        val pet = app.registerDefaultPet()
        val appointment = app.appointmentService.scheduleAppointment(
            pet.id,
            LocalDateTime.of(2026, 6, 17, 12, 0),
            "Skin irritation"
        )
        app.appointmentService.completeAppointment(appointment.id)

        val record = app.medicalRecordService.createRecord(
            petId = pet.id,
            appointmentId = appointment.id,
            diagnosis = "Mild dermatitis",
            treatment = "Topical cream",
            notes = "Return if symptoms continue",
            recordedAt = LocalDateTime.of(2026, 6, 17, 12, 45)
        )

        assertEquals("REC-0001", record.id)
        assertEquals(appointment.id, record.appointmentId)
        assertTrue(app.medicalRecordService.listRecordsByPet(pet.id).contains(record))
    }

    @Test
    fun `invoice calculates service total and can be marked paid`() {
        val app = fixture()
        val pet = app.registerDefaultPet()

        val invoice = app.billingService.createInvoice(
            petId = pet.id,
            services = listOf(ClinicService.CONSULTATION, ClinicService.VACCINATION),
            issuedAt = LocalDateTime.of(2026, 6, 17, 13, 0)
        )
        val paid = app.billingService.markAsPaid(invoice.id)

        assertEquals(350_000.0, invoice.total())
        assertEquals(PaymentStatus.PAID, paid.paymentStatus)
    }

    private data class Fixture(
        val ownerService: OwnerService,
        val patientService: PatientService,
        val appointmentService: AppointmentService,
        val medicalRecordService: MedicalRecordService,
        val billingService: BillingService
    ) {
        fun registerDefaultPet() = patientService.registerPet(
            ownerId = ownerService.registerOwner("Maya Hartono", "0833333333").id,
            name = "Darcy",
            species = "Dog",
            breed = "Golden Retriever",
            age = 3
        )
    }
}
