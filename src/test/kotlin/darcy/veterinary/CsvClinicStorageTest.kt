package darcy.veterinary

import darcy.veterinary.application.BillingService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import darcy.veterinary.infrastructure.storage.CsvClinicStorage
import java.nio.file.Files
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CsvClinicStorageTest {
    @Test
    fun `storage saves and reloads owners pets and invoices`() {
        val directory = Files.createTempDirectory("darcy-storage-test")
        val ids = SequenceIdGenerator()

        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val appointmentRepository = InMemoryAppointmentRepository()
        val medicalRecordRepository = InMemoryMedicalRecordRepository()
        val invoiceRepository = InMemoryInvoiceRepository()

        val ownerService = OwnerService(ownerRepository, ids)
        val patientService = PatientService(petRepository, ownerRepository, ids)
        val billingService = BillingService(invoiceRepository, petRepository, ids)

        val owner = ownerService.registerOwner("Raka Wijaya", "0844444444")
        val pet = patientService.registerPet(owner.id, "Luna", "Cat")
        billingService.createInvoice(pet.id, listOf(ClinicService.GROOMING, ClinicService.BASIC_TREATMENT))

        CsvClinicStorage(directory).saveAll(
            ownerRepository,
            petRepository,
            appointmentRepository,
            medicalRecordRepository,
            invoiceRepository
        )

        val loadedOwners = InMemoryOwnerRepository()
        val loadedPets = InMemoryPetRepository()
        val loadedAppointments = InMemoryAppointmentRepository()
        val loadedRecords = InMemoryMedicalRecordRepository()
        val loadedInvoices = InMemoryInvoiceRepository()

        CsvClinicStorage(directory).loadAll(
            loadedOwners,
            loadedPets,
            loadedAppointments,
            loadedRecords,
            loadedInvoices
        )

        assertEquals(1, loadedOwners.findAll().size)
        assertEquals("Raka Wijaya", loadedOwners.findAll().first().fullName)
        assertEquals(1, loadedPets.findAll().size)
        assertEquals("Luna", loadedPets.findAll().first().name)
        assertEquals(225_000.0, loadedInvoices.findAll().first().total())
    }
}
