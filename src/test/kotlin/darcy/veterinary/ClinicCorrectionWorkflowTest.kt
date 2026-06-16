package darcy.veterinary

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.BillingService
import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.exception.InvalidClinicOperationException
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

class ClinicCorrectionWorkflowTest {
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
    fun `owner profile can be corrected without changing id`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Old Name", "0810000000")

        val updated = app.ownerService.updateOwner(
            id = owner.id,
            fullName = "Correct Name",
            phoneNumber = "0819999999",
            email = "correct@example.com"
        )

        assertEquals(owner.id, updated.id)
        assertEquals("Correct Name", app.ownerService.getOwner(owner.id).fullName)
        assertEquals("0819999999", updated.phoneNumber)
    }

    @Test
    fun `pet profile can be corrected without changing owner link`() {
        val app = fixture()
        val pet = app.registerDefaultPet()

        val updated = app.patientService.updatePet(
            id = pet.id,
            name = "Darcy Jr",
            species = "Dog",
            breed = "Mixed Breed",
            age = 4
        )

        assertEquals(pet.id, updated.id)
        assertEquals(pet.ownerId, updated.ownerId)
        assertEquals("Darcy Jr", updated.name)
        assertEquals(4, updated.age)
    }

    @Test
    fun `scheduled appointment can be rescheduled`() {
        val app = fixture()
        val pet = app.registerDefaultPet()
        val appointment = app.appointmentService.scheduleAppointment(
            pet.id,
            LocalDateTime.of(2026, 6, 18, 9, 0),
            "Checkup"
        )

        val rescheduled = app.appointmentService.rescheduleAppointment(
            id = appointment.id,
            scheduledAt = LocalDateTime.of(2026, 6, 19, 11, 30),
            reason = "Follow-up checkup"
        )

        assertEquals(appointment.id, rescheduled.id)
        assertEquals(LocalDateTime.of(2026, 6, 19, 11, 30), rescheduled.scheduledAt)
        assertEquals("Follow-up checkup", rescheduled.reason)
    }

    @Test
    fun `completed appointment cannot be rescheduled`() {
        val app = fixture()
        val pet = app.registerDefaultPet()
        val appointment = app.appointmentService.scheduleAppointment(
            pet.id,
            LocalDateTime.of(2026, 6, 18, 9, 0),
            "Checkup"
        )
        app.appointmentService.completeAppointment(appointment.id)

        assertFailsWith<InvalidClinicOperationException> {
            app.appointmentService.rescheduleAppointment(
                id = appointment.id,
                scheduledAt = LocalDateTime.of(2026, 6, 19, 11, 30),
                reason = "Late correction"
            )
        }
    }

    @Test
    fun `medical record can be corrected`() {
        val app = fixture()
        val pet = app.registerDefaultPet()
        val record = app.medicalRecordService.createRecord(
            petId = pet.id,
            diagnosis = "Typo diagnosis",
            treatment = "Old treatment",
            notes = "Old notes"
        )

        val corrected = app.medicalRecordService.updateRecord(
            id = record.id,
            diagnosis = "Corrected diagnosis",
            treatment = "Updated treatment",
            notes = "Updated notes"
        )

        assertEquals(record.id, corrected.id)
        assertEquals("Corrected diagnosis", corrected.diagnosis)
        assertEquals("Updated notes", app.medicalRecordService.getRecord(record.id).notes)
    }

    @Test
    fun `unpaid invoice can be voided`() {
        val app = fixture()
        val pet = app.registerDefaultPet()
        val invoice = app.billingService.createInvoice(pet.id, listOf(ClinicService.CONSULTATION))

        val voided = app.billingService.voidInvoice(invoice.id)

        assertEquals(PaymentStatus.VOIDED, voided.paymentStatus)
    }

    @Test
    fun `paid invoice cannot be voided`() {
        val app = fixture()
        val pet = app.registerDefaultPet()
        val invoice = app.billingService.createInvoice(pet.id, listOf(ClinicService.CONSULTATION))
        app.billingService.markAsPaid(invoice.id)

        assertFailsWith<InvalidClinicOperationException> {
            app.billingService.voidInvoice(invoice.id)
        }
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
