package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.BillingService
import darcy.veterinary.application.ClinicWorkspaceFacade
import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class PatientSearchViewModelTest {
    private fun fixture(): Fixture {
        val ids = SequenceIdGenerator()
        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val appointmentRepository = InMemoryAppointmentRepository()
        val recordRepository = InMemoryMedicalRecordRepository()
        val invoiceRepository = InMemoryInvoiceRepository()

        val ownerService = OwnerService(ownerRepository, ids)
        val patientService = PatientService(petRepository, ownerRepository, ids)
        val appointmentService = AppointmentService(appointmentRepository, petRepository, ids)
        val recordService = MedicalRecordService(recordRepository, petRepository, appointmentRepository, ids)
        val billingService = BillingService(invoiceRepository, petRepository, ids)
        val facade = ClinicWorkspaceFacade(
            ownerService = ownerService,
            patientService = patientService,
            appointmentService = appointmentService,
            recordService = recordService,
            billingService = billingService
        )

        return Fixture(ownerService, patientService, PatientSearchViewModel(facade))
    }

    @Test
    fun `blank search gives validation without loading broad results`() {
        val app = fixture()
        app.viewModel.updateQuery("   ")

        app.viewModel.search()

        assertEquals("", app.viewModel.state.query)
        assertEquals("Enter a search term.", app.viewModel.state.validationMessage)
        assertFalse(app.viewModel.state.searchResult.hasResults)
        assertNull(app.viewModel.state.emptyStateMessage)
    }

    @Test
    fun `search exposes lookup rows and empty state for desktop screens`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        app.patientService.registerPet(owner.id, "Darcy", "Dog")

        app.viewModel.updateQuery("Darcy")
        app.viewModel.search()

        assertNull(app.viewModel.state.validationMessage)
        assertTrue(app.viewModel.state.searchResult.hasResults)
        assertEquals("Darcy", app.viewModel.state.searchResult.patients.single().name)
        assertNull(app.viewModel.state.emptyStateMessage)

        app.viewModel.updateQuery("No Match")
        app.viewModel.search()

        assertFalse(app.viewModel.state.searchResult.hasResults)
        assertEquals("No owners or patients match \"No Match\".", app.viewModel.state.emptyStateMessage)
    }

    @Test
    fun `patient chart can be opened from a search result`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Nadia Prasetyo", "0811111111")
        val pet = app.patientService.registerPet(owner.id, "Miso", "Cat")

        app.viewModel.openPatientChart(pet.id)

        assertNotNull(app.viewModel.state.selectedChart)
        assertEquals("Miso", app.viewModel.state.selectedChart?.patient?.name)
        assertEquals("Nadia Prasetyo", app.viewModel.state.selectedChart?.owner?.fullName)
    }

    private data class Fixture(
        val ownerService: OwnerService,
        val patientService: PatientService,
        val viewModel: PatientSearchViewModel
    )
}
