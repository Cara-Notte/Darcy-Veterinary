package darcy.veterinary

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.model.PetSex
import darcy.veterinary.domain.model.VisitType
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import darcy.veterinary.infrastructure.storage.JsonClinicStorage
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JsonVeterinaryDomainStorageTest {
    @Test
    fun `json storage preserves richer veterinary domain fields`() {
        val directory = Files.createTempDirectory("darcy-json-domain-test")
        val ids = SequenceIdGenerator()

        val owners = InMemoryOwnerRepository()
        val pets = InMemoryPetRepository()
        val appointments = InMemoryAppointmentRepository()
        val records = InMemoryMedicalRecordRepository()
        val invoices = InMemoryInvoiceRepository()

        val ownerService = OwnerService(owners, ids)
        val patientService = PatientService(pets, owners, ids)
        val appointmentService = AppointmentService(appointments, pets, ids)
        val recordService = MedicalRecordService(records, pets, appointments, ids)

        val owner = ownerService.registerOwner("Storage Owner", "0822222222")
        val pet = patientService.registerPet(
            ownerId = owner.id,
            name = "Kopi",
            species = "Cat",
            sex = PetSex.MALE,
            dateOfBirth = LocalDate.of(2022, 2, 2),
            weightKg = 4.8,
            allergies = listOf("Fish"),
            medicalConditions = listOf("Asthma")
        )
        val appointment = appointmentService.scheduleAppointment(
            petId = pet.id,
            scheduledAt = LocalDateTime.of(2026, 6, 22, 8, 30),
            reason = "Vaccination booster",
            visitType = VisitType.VACCINATION,
            veterinarianName = "Dr. Citra"
        )
        recordService.createRecord(
            petId = pet.id,
            appointmentId = appointment.id,
            diagnosis = "Healthy",
            treatment = "Vaccine administered",
            notes = "Stable",
            veterinarianName = "Dr. Citra"
        )

        JsonClinicStorage(directory).saveAll(owners, pets, appointments, records, invoices, null, null)

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

        assertEquals(PetSex.MALE, loadedPets.findAll().first().sex)
        assertEquals(LocalDate.of(2022, 2, 2), loadedPets.findAll().first().dateOfBirth)
        assertEquals(4.8, loadedPets.findAll().first().weightKg)
        assertEquals(listOf("Fish"), loadedPets.findAll().first().allergies)
        assertEquals(listOf("Asthma"), loadedPets.findAll().first().medicalConditions)
        assertEquals(VisitType.VACCINATION, loadedAppointments.findAll().first().visitType)
        assertEquals("Dr. Citra", loadedAppointments.findAll().first().veterinarianName)
        assertEquals("Dr. Citra", loadedRecords.findAll().first().veterinarianName)
    }
}
