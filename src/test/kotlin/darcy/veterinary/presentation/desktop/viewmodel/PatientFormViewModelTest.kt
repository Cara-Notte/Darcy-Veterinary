package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.model.PetSex
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class PatientFormViewModelTest {
    private fun fixture(): Fixture {
        val ids = SequenceIdGenerator()
        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val ownerService = OwnerService(ownerRepository, ids)
        val patientService = PatientService(petRepository, ownerRepository, ids)
        return Fixture(
            ownerService = ownerService,
            patientService = patientService,
            viewModel = PatientFormViewModel(patientService)
        )
    }

    @Test
    fun `blank required patient fields produce inline validation errors without saving`() {
        val app = fixture()

        app.viewModel.save()

        assertEquals("Owner is required.", app.viewModel.state.fieldErrors[PatientFormField.OWNER_ID])
        assertEquals("Patient name is required.", app.viewModel.state.fieldErrors[PatientFormField.NAME])
        assertEquals("Species is required.", app.viewModel.state.fieldErrors[PatientFormField.SPECIES])
        assertNull(app.viewModel.state.savedPatientId)
        assertTrue(app.patientService.listPets().isEmpty())
    }

    @Test
    fun `invalid number and date fields produce inline validation errors`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        app.viewModel.updateOwnerId(owner.id)
        app.viewModel.updateName("Darcy")
        app.viewModel.updateSpecies("Dog")
        app.viewModel.updateAge("four")
        app.viewModel.updateWeightKg("0")
        app.viewModel.updateDateOfBirth("23-06-2026")

        app.viewModel.save()

        assertEquals("Age must be a whole number.", app.viewModel.state.fieldErrors[PatientFormField.AGE])
        assertEquals("Weight must be greater than zero.", app.viewModel.state.fieldErrors[PatientFormField.WEIGHT_KG])
        assertEquals("Date of birth must use YYYY-MM-DD.", app.viewModel.state.fieldErrors[PatientFormField.DATE_OF_BIRTH])
        assertTrue(app.patientService.listPets().isEmpty())
    }

    @Test
    fun `create mode saves patient and normalizes list fields`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        app.viewModel.startCreate(owner.id)
        app.viewModel.updateName("  Darcy  ")
        app.viewModel.updateSpecies(" Dog ")
        app.viewModel.updateBreed(" Corgi ")
        app.viewModel.updateAge("4")
        app.viewModel.updateSex(PetSex.FEMALE)
        app.viewModel.updateDateOfBirth("2022-06-23")
        app.viewModel.updateWeightKg("9.8")
        app.viewModel.updateAllergies("Chicken, Dust")
        app.viewModel.updateMedicalConditions("Sensitive digestion; Nervous temperament")

        app.viewModel.save()

        val patient = app.patientService.getPet("PET-0001")
        assertFalse(app.viewModel.state.isSaving)
        assertEquals(PatientFormMode.EDIT, app.viewModel.state.mode)
        assertEquals("PET-0001", app.viewModel.state.savedPatientId)
        assertEquals("Patient profile created.", app.viewModel.state.successMessage)
        assertEquals("Darcy", patient.name)
        assertEquals("Dog", patient.species)
        assertEquals("Corgi", patient.breed)
        assertEquals(4, patient.age)
        assertEquals(PetSex.FEMALE, patient.sex)
        assertEquals(LocalDate.of(2022, 6, 23), patient.dateOfBirth)
        assertEquals(9.8, patient.weightKg)
        assertEquals(listOf("Chicken", "Dust"), patient.allergies)
        assertEquals(listOf("Sensitive digestion", "Nervous temperament"), patient.medicalConditions)
    }

    @Test
    fun `load and update existing patient`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Nadia Prasetyo", "0811111111")
        val patient = app.patientService.registerPet(
            ownerId = owner.id,
            name = "Miso",
            species = "Cat",
            age = 2,
            sex = PetSex.UNKNOWN,
            allergies = listOf("Fish")
        )

        app.viewModel.load(patient.id)
        app.viewModel.updateName("Miso Jr")
        app.viewModel.updateAge("")
        app.viewModel.updateAllergies("Fish\nPollen")
        app.viewModel.save()

        val updated = app.patientService.getPet(patient.id)
        assertEquals(PatientFormMode.EDIT, app.viewModel.state.mode)
        assertEquals(patient.id, app.viewModel.state.patientId)
        assertEquals("Patient profile updated.", app.viewModel.state.successMessage)
        assertEquals("Miso Jr", updated.name)
        assertNull(updated.age)
        assertEquals(listOf("Fish", "Pollen"), updated.allergies)
    }

    @Test
    fun `missing owner from service is shown as form error message`() {
        val app = fixture()
        app.viewModel.updateOwnerId("OWN-404")
        app.viewModel.updateName("Bento")
        app.viewModel.updateSpecies("Dog")

        app.viewModel.save()

        assertEquals("Owner with ID OWN-404 was not found.", app.viewModel.state.errorMessage)
        assertNull(app.viewModel.state.successMessage)
        assertNull(app.viewModel.state.savedPatientId)
        assertTrue(app.patientService.listPets().isEmpty())
    }

    @Test
    fun `start create can prefill owner and clears loaded patient state`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Lia Santoso", "0822222222")
        val patient = app.patientService.registerPet(owner.id, "Bento", "Dog")
        app.viewModel.load(patient.id)

        app.viewModel.startCreate(owner.id)

        assertEquals(PatientFormMode.CREATE, app.viewModel.state.mode)
        assertNull(app.viewModel.state.patientId)
        assertEquals(owner.id, app.viewModel.state.ownerId)
        assertEquals("", app.viewModel.state.name)
        assertEquals("", app.viewModel.state.species)
    }

    private data class Fixture(
        val ownerService: OwnerService,
        val patientService: PatientService,
        val viewModel: PatientFormViewModel
    )
}
