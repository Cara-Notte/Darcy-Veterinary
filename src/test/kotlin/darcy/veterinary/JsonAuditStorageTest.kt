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
import darcy.veterinary.infrastructure.storage.JsonClinicStorage
import java.nio.file.Files
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JsonAuditStorageTest {
    @Test
    fun `json storage saves and reloads audit history`() {
        val directory = Files.createTempDirectory("darcy-json-audit-test")
        val ids = SequenceIdGenerator()

        val owners = InMemoryOwnerRepository()
        val pets = InMemoryPetRepository()
        val appointments = InMemoryAppointmentRepository()
        val records = InMemoryMedicalRecordRepository()
        val invoices = InMemoryInvoiceRepository()
        val revisions = InMemoryMedicalRecordRevisionRepository()
        val invoiceHistory = InMemoryInvoiceStatusHistoryRepository()

        val ownerService = OwnerService(owners, ids)
        val patientService = PatientService(pets, owners, ids)
        val recordService = MedicalRecordService(records, pets, appointments, ids, revisions)
        val billingService = BillingService(invoices, pets, ids, invoiceHistory)

        val owner = ownerService.registerOwner("Audit Storage Owner", "0899999999")
        val pet = patientService.registerPet(owner.id, "Pixel", "Cat")
        val record = recordService.createRecord(pet.id, "Old", "Care", "Notes")
        recordService.updateRecord(record.id, "New", "Care", "Updated")
        val invoice = billingService.createInvoice(pet.id, listOf(ClinicService.CONSULTATION))
        billingService.markAsPaid(invoice.id)

        JsonClinicStorage(directory).saveAll(owners, pets, appointments, records, invoices, revisions, invoiceHistory)

        val loadedOwners = InMemoryOwnerRepository()
        val loadedPets = InMemoryPetRepository()
        val loadedAppointments = InMemoryAppointmentRepository()
        val loadedRecords = InMemoryMedicalRecordRepository()
        val loadedInvoices = InMemoryInvoiceRepository()
        val loadedRevisions = InMemoryMedicalRecordRevisionRepository()
        val loadedInvoiceHistory = InMemoryInvoiceStatusHistoryRepository()

        JsonClinicStorage(directory).loadAll(
            loadedOwners,
            loadedPets,
            loadedAppointments,
            loadedRecords,
            loadedInvoices,
            loadedRevisions,
            loadedInvoiceHistory
        )

        assertEquals("Old", loadedRevisions.findByRecordId(record.id).first().diagnosis)
        assertEquals(PaymentStatus.PAID, loadedInvoiceHistory.findByInvoiceId(invoice.id).last().toStatus)
    }
}
