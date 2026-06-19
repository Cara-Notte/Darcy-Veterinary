package darcy.veterinary

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.model.PetSex
import darcy.veterinary.domain.model.VisitType
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BetterDomainModelTest {
    @Test
    fun `pet profile stores veterinary-specific fields`() {
        val fixture = fixture()
        val owner = fixture.ownerService.registerOwner("Domain Owner", "0812345678")

        val pet = fixture.patientService.registerPet(
            ownerId = owner.id,
            name = "Coco",
            species = "Dog",
            breed = "Poodle",
            age = 5,
            sex = PetSex.FEMALE,
            dateOfBirth = LocalDate.of(2021, 4, 12),
            weightKg = 7.4,
            allergies = listOf("Chicken", "Dust"),
            medicalConditions = listOf("Sensitive skin")
        )

        assertEquals(PetSex.FEMALE, pet.sex)
        assertEquals(LocalDate.of(2021, 4, 12), pet.dateOfBirth)
        assertEquals(7.4, pet.weightKg)
        assertEquals(listOf("Chicken", "Dust"), pet.allergies)
        assertEquals(listOf("Sensitive skin"), pet.medicalConditions)
    }

    @Test
    fun `pet weight cannot be negative`() {
        val fixture = fixture()
        val owner = fixture.ownerService.registerOwner("Weight Owner", "0812345679")

        assertFailsWith<IllegalArgumentException> {
            fixture.patientService.registerPet(
                ownerId = owner.id,
                name = "Bad Weight",
                species = "Cat",
                weightKg = -1.0
            )
        }
    }

    @Test
    fun `appointment stores visit type and veterinarian`() {
        val fixture = fixture()
        val pet = fixture.registerPet()

        val appointment = fixture.appointmentService.scheduleAppointment(
            petId = pet.id,
            scheduledAt = LocalDateTime.of(2026, 6, 21, 10, 0),
            reason = "Annual vaccination",
            visitType = VisitType.VACCINATION,
            veterinarianName = "Dr. Maya"
        )

        assertEquals(VisitType.VACCINATION, appointment.visitType)
        assertEquals("Dr. Maya", appointment.veterinarianName)
    }

    @Test
    fun `medical record stores veterinarian name`() {
        val fixture = fixture()
        val pet = fixture.registerPet()

        val record = fixture.medicalRecordService.createRecord(
            petId = pet.id,
            diagnosis = "Healthy",
            treatment = "Routine check",
            notes = "No issues",
            veterinarianName = "Dr. Bima"
        )

        assertEquals("Dr. Bima", record.veterinarianName)
    }

    private fun fixture(): Fixture {
        val ids = SequenceIdGenerator()
        val owners = InMemoryOwnerRepository()
        val pets = InMemoryPetRepository()
        val appointments = InMemoryAppointmentRepository()
        val records = InMemoryMedicalRecordRepository()

        return Fixture(
            ownerService = OwnerService(owners, ids),
            patientService = PatientService(pets, owners, ids),
            appointmentService = AppointmentService(appointments, pets, ids),
            medicalRecordService = MedicalRecordService(records, pets, appointments, ids)
        )
    }

    private data class Fixture(
        val ownerService: OwnerService,
        val patientService: PatientService,
        val appointmentService: AppointmentService,
        val medicalRecordService: MedicalRecordService
    ) {
        fun registerPet() = patientService.registerPet(
            ownerId = ownerService.registerOwner("Default Owner", "0811111111").id,
            name = "Default Pet",
            species = "Dog"
        )
    }
}
