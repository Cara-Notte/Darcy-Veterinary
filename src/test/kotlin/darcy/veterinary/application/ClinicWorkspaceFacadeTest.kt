package darcy.veterinary.application

import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.PetSex
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceStatusHistoryRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRevisionRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class ClinicWorkspaceFacadeTest {
    private fun fixture(): Fixture {
        val ids = SequenceIdGenerator()
        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val appointmentRepository = InMemoryAppointmentRepository()
        val recordRepository = InMemoryMedicalRecordRepository()
        val invoiceRepository = InMemoryInvoiceRepository()
        val revisionRepository = InMemoryMedicalRecordRevisionRepository()
        val invoiceHistoryRepository = InMemoryInvoiceStatusHistoryRepository()

        val ownerService = OwnerService(ownerRepository, ids)
        val patientService = PatientService(petRepository, ownerRepository, ids)
        val appointmentService = AppointmentService(appointmentRepository, petRepository, ids)
        val recordService = MedicalRecordService(
            recordRepository,
            petRepository,
            appointmentRepository,
            ids,
            revisionRepository
        )
        val billingService = BillingService(invoiceRepository, petRepository, ids, invoiceHistoryRepository)

        return Fixture(
            ownerService = ownerService,
            patientService = patientService,
            appointmentService = appointmentService,
            recordService = recordService,
            billingService = billingService,
            facade = ClinicWorkspaceFacade(
                ownerService = ownerService,
                patientService = patientService,
                appointmentService = appointmentService,
                recordService = recordService,
                billingService = billingService
            )
        )
    }

    @Test
    fun `search returns owner and patient lookup rows for desktop UI`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333", "maya@example.com")
        app.patientService.registerPet(
            ownerId = owner.id,
            name = "Darcy",
            species = "Dog",
            breed = "Golden Retriever",
            age = 3,
            allergies = listOf("Chicken")
        )

        val ownerSearch = app.facade.search("maya")
        val patientSearch = app.facade.search("darcy")

        assertTrue(ownerSearch.hasResults)
        assertEquals("Maya Hartono", ownerSearch.owners.single().fullName)
        assertEquals(1, ownerSearch.owners.single().patientCount)
        assertEquals("Maya Hartono", patientSearch.patients.single().ownerName)
        assertTrue(patientSearch.patients.single().hasAlerts)
    }

    @Test
    fun `patient chart aggregates chart header timeline records and invoices`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Nadia Prasetyo", "0811111111", "nadia@example.com")
        val pet = app.patientService.registerPet(
            ownerId = owner.id,
            name = "Miso",
            species = "Cat",
            breed = "Domestic Short Hair",
            age = 2,
            sex = PetSex.FEMALE,
            dateOfBirth = LocalDate.of(2024, 2, 1),
            weightKg = 4.2,
            medicalConditions = listOf("Sensitive stomach")
        )
        val appointment = app.appointmentService.scheduleAppointment(
            pet.id,
            LocalDateTime.of(2026, 6, 22, 9, 30),
            "Vomiting"
        )
        app.recordService.createRecord(
            petId = pet.id,
            appointmentId = appointment.id,
            diagnosis = "Gastritis",
            treatment = "Diet adjustment",
            notes = "Recheck if symptoms continue",
            recordedAt = LocalDateTime.of(2026, 6, 22, 10, 0),
            veterinarianName = "Dr. Sari"
        )
        app.billingService.createInvoice(
            petId = pet.id,
            services = listOf(ClinicService.CONSULTATION),
            issuedAt = LocalDateTime.of(2026, 6, 22, 10, 30)
        )

        val chart = app.facade.patientChart(pet.id)

        assertEquals("Miso", chart.patient.name)
        assertEquals("Nadia Prasetyo", chart.owner.fullName)
        assertEquals(listOf("Sensitive stomach"), chart.patient.medicalConditions)
        assertEquals("Vomiting", chart.appointments.single().reason)
        assertEquals("Gastritis", chart.records.single().diagnosis)
        assertEquals(100_000.0, chart.invoices.single().total)
    }

    @Test
    fun `blank search returns an empty result instead of querying broad tables`() {
        val app = fixture()

        val result = app.facade.search("   ")

        assertFalse(result.hasResults)
        assertEquals("", result.query)
    }

    private data class Fixture(
        val ownerService: OwnerService,
        val patientService: PatientService,
        val appointmentService: AppointmentService,
        val recordService: MedicalRecordService,
        val billingService: BillingService,
        val facade: ClinicWorkspaceFacade
    )
}
