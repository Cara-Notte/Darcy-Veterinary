package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class OwnerFormViewModelTest {
    private fun fixture(): Fixture {
        val ownerRepository = InMemoryOwnerRepository()
        val ownerService = OwnerService(ownerRepository, SequenceIdGenerator())
        return Fixture(ownerService, OwnerFormViewModel(ownerService))
    }

    @Test
    fun `blank owner fields produce inline validation errors without saving`() {
        val app = fixture()

        app.viewModel.save()

        assertEquals("Owner name is required.", app.viewModel.state.fieldErrors[OwnerFormField.FULL_NAME])
        assertEquals("Phone number is required.", app.viewModel.state.fieldErrors[OwnerFormField.PHONE_NUMBER])
        assertNull(app.viewModel.state.savedOwnerId)
        assertTrue(app.ownerService.listOwners().isEmpty())
    }

    @Test
    fun `create mode saves owner and switches to edit mode`() {
        val app = fixture()
        app.viewModel.updateFullName("  Maya Hartono  ")
        app.viewModel.updatePhoneNumber(" 0833333333 ")
        app.viewModel.updateEmail(" maya@example.com ")

        app.viewModel.save()

        assertFalse(app.viewModel.state.isSaving)
        assertEquals(OwnerFormMode.EDIT, app.viewModel.state.mode)
        assertEquals("OWN-0001", app.viewModel.state.savedOwnerId)
        assertEquals("Owner profile created.", app.viewModel.state.successMessage)
        assertEquals("Maya Hartono", app.viewModel.state.fullName)
        assertEquals("0833333333", app.viewModel.state.phoneNumber)
        assertEquals("maya@example.com", app.viewModel.state.email)
        assertEquals("Maya Hartono", app.ownerService.getOwner("OWN-0001").fullName)
    }

    @Test
    fun `load and update existing owner`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Nadia Prasetyo", "0811111111", "nadia@example.com")

        app.viewModel.load(owner.id)
        app.viewModel.updateFullName("Nadia P.")
        app.viewModel.updatePhoneNumber("0811999999")
        app.viewModel.updateEmail("")
        app.viewModel.save()

        assertEquals(OwnerFormMode.EDIT, app.viewModel.state.mode)
        assertEquals(owner.id, app.viewModel.state.ownerId)
        assertEquals("Owner profile updated.", app.viewModel.state.successMessage)
        assertEquals("Nadia P.", app.ownerService.getOwner(owner.id).fullName)
        assertEquals("0811999999", app.ownerService.getOwner(owner.id).phoneNumber)
        assertNull(app.ownerService.getOwner(owner.id).email)
    }

    @Test
    fun `duplicate phone number from service is shown as form error message`() {
        val app = fixture()
        app.ownerService.registerOwner("Existing Owner", "0822222222")
        app.viewModel.updateFullName("New Owner")
        app.viewModel.updatePhoneNumber("0822222222")

        app.viewModel.save()

        assertEquals("Owner phone number already exists.", app.viewModel.state.errorMessage)
        assertNull(app.viewModel.state.successMessage)
        assertNull(app.viewModel.state.savedOwnerId)
        assertEquals(1, app.ownerService.listOwners().size)
    }

    @Test
    fun `start create clears loaded owner state`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Nadia Prasetyo", "0811111111")
        app.viewModel.load(owner.id)

        app.viewModel.startCreate()

        assertEquals(OwnerFormMode.CREATE, app.viewModel.state.mode)
        assertNull(app.viewModel.state.ownerId)
        assertEquals("", app.viewModel.state.fullName)
        assertEquals("", app.viewModel.state.phoneNumber)
        assertEquals("", app.viewModel.state.email)
    }

    private data class Fixture(
        val ownerService: OwnerService,
        val viewModel: OwnerFormViewModel
    )
}
