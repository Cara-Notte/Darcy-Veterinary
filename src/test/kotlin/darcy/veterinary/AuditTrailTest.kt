package darcy.veterinary

import darcy.veterinary.application.BillingService
import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceStatusHistoryRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRevisionRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AuditTrailTest {
    @Test
    fun `medical record correction stores the previous record state`() {
        val fixture = fixture()
        val pet = fixture.patientService.registerPet(
            ownerId = fixture.ownerService.registerOwner("Audit Owner", "0877777777").id,
            name = "Nori",
            species = "Cat"
        )
        val record = fixture.medicalRecordService.createRecord(
            petId = pet.id,
            diagnosis = "Old diagnosis",
            treatment = "Old treatment",
            notes = "Old notes"
        )

        fixture.medicalRecordService.updateRecord(
            id = record.id,
            diagnosis = "New diagnosis",
            treatment = "New treatment",
            notes = "New notes"
        )

        val revisions = fixture.medicalRecordService.listRevisions(record.id)
        assertEquals(1, revisions.size)
        assertEquals("Old diagnosis", revisions.first().diagnosis)
        assertEquals("Old treatment", revisions.first().treatment)
        assertEquals("Old notes", revisions.first().notes)
    }

    @Test
    fun `invoice payment and void actions store status history`() {
        val fixture = fixture()
        val pet = fixture.patientService.registerPet(
            ownerId = fixture.ownerService.registerOwner("Billing Owner", "0888888888").id,
            name = "Bima",
            species = "Dog"
        )
        val invoice = fixture.billingService.createInvoice(pet.id, listOf(ClinicService.CONSULTATION))

        fixture.billingService.markAsPaid(invoice.id)

        val history = fixture.billingService.listStatusHistory(invoice.id)
        assertEquals(2, history.size)
        assertEquals(null, history[0].fromStatus)
        assertEquals(PaymentStatus.UNPAID, history[0].toStatus)
        assertEquals(PaymentStatus.UNPAID, history[1].fromStatus)
        assertEquals(PaymentStatus.PAID, history[1].toStatus)
    }

    private fun fixture(): Fixture {
        val ids = SequenceIdGenerator()
        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val appointmentRepository = InMemoryAppointmentRepository()
        val medicalRecordRepository = InMemoryMedicalRecordRepository()
        val invoiceRepository = InMemoryInvoiceRepository()
        val revisionRepository = InMemoryMedicalRecordRevisionRepository()
        val statusHistoryRepository = InMemoryInvoiceStatusHistoryRepository()

        return Fixture(
            ownerService = OwnerService(ownerRepository, ids),
            patientService = PatientService(petRepository, ownerRepository, ids),
            medicalRecordService = MedicalRecordService(
                medicalRecordRepository,
                petRepository,
                appointmentRepository,
                ids,
                revisionRepository
            ),
            billingService = BillingService(invoiceRepository, petRepository, ids, statusHistoryRepository)
        )
    }

    private data class Fixture(
        val ownerService: OwnerService,
        val patientService: PatientService,
        val medicalRecordService: MedicalRecordService,
        val billingService: BillingService
    )
}
