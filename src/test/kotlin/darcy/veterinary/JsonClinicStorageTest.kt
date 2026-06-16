package darcy.veterinary

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
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

class JsonClinicStorageTest {
    @Test
    fun `json storage preserves separators and multiline text`() {
        val directory = Files.createTempDirectory("darcy-json-storage-test")
        val ids = SequenceIdGenerator()

        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val appointmentRepository = InMemoryAppointmentRepository()
        val medicalRecordRepository = InMemoryMedicalRecordRepository()
        val invoiceRepository = InMemoryInvoiceRepository()

        val ownerService = OwnerService(ownerRepository, ids)
        val patientService = PatientService(petRepository, ownerRepository, ids)
        val appointmentService = AppointmentService(appointmentRepository, petRepository, ids)
        val medicalRecordService = MedicalRecordService(medicalRecordRepository, petRepository, appointmentRepository, ids)

        val owner = ownerService.registerOwner("Rani; Santoso", "0855555555", "rani@example.com")
        val pet = patientService.registerPet(owner.id, "Mochi", "Cat", "Domestic; short hair", 4)
        val appointment = appointmentService.scheduleAppointment(
            pet.id,
            LocalDateTime.of(2026, 6, 18, 9, 30),
            "Skin check; recurring itch"
        )
        val notes = "First line; still part of notes\nSecond line with comma, semicolon; and pipe | symbols"
        medicalRecordService.createRecord(
            petId = pet.id,
            appointmentId = appointment.id,
            diagnosis = "Mild allergy; seasonal",
            treatment = "Antihistamine, topical care",
            notes = notes,
            recordedAt = LocalDateTime.of(2026, 6, 18, 10, 15)
        )

        JsonClinicStorage(directory).saveAll(
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

        JsonClinicStorage(directory).loadAll(
            loadedOwners,
            loadedPets,
            loadedAppointments,
            loadedRecords,
            loadedInvoices
        )

        assertEquals("Rani; Santoso", loadedOwners.findAll().first().fullName)
        assertEquals("Domestic; short hair", loadedPets.findAll().first().breed)
        assertEquals("Skin check; recurring itch", loadedAppointments.findAll().first().reason)
        assertEquals(notes, loadedRecords.findAll().first().notes)
    }
}
