package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.BillingService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceStatusHistoryRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class BillingCheckoutViewModelTest {
    private fun fixture(): Fixture {
        val ids = SequenceIdGenerator()
        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val invoiceRepository = InMemoryInvoiceRepository()
        val historyRepository = InMemoryInvoiceStatusHistoryRepository()
        val ownerService = OwnerService(ownerRepository, ids)
        val patientService = PatientService(petRepository, ownerRepository, ids)
        val billingService = BillingService(invoiceRepository, petRepository, ids, historyRepository)
        return Fixture(
            ownerService = ownerService,
            patientService = patientService,
            billingService = billingService,
            viewModel = BillingCheckoutViewModel(billingService)
        )
    }

    @Test
    fun `blank invoice fields produce inline validation errors without saving`() {
        val app = fixture()

        app.viewModel.createInvoice()

        assertEquals("Patient is required.", app.viewModel.state.fieldErrors[BillingCheckoutField.PATIENT_ID])
        assertEquals("At least one service is required.", app.viewModel.state.fieldErrors[BillingCheckoutField.SERVICES])
        assertNull(app.viewModel.state.invoiceId)
        assertTrue(app.billingService.listInvoices().isEmpty())
    }

    @Test
    fun `invalid issued date time produces inline validation error`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        val patient = app.patientService.registerPet(owner.id, "Darcy", "Dog")
        app.viewModel.startInvoice(patient.id)
        app.viewModel.toggleService(ClinicService.CONSULTATION)
        app.viewModel.updateIssuedAt("2026/06/23 09:30")

        app.viewModel.createInvoice()

        assertEquals(
            "Issued date and time must use YYYY-MM-DDTHH:MM.",
            app.viewModel.state.fieldErrors[BillingCheckoutField.ISSUED_AT]
        )
        assertTrue(app.billingService.listInvoices().isEmpty())
    }

    @Test
    fun `create invoice saves selected services and shows total`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        val patient = app.patientService.registerPet(owner.id, "Darcy", "Dog")
        app.viewModel.startInvoice(patient.id)
        app.viewModel.updateIssuedAt("2026-06-23T09:30")
        app.viewModel.toggleService(ClinicService.CONSULTATION)
        app.viewModel.toggleService(ClinicService.VACCINATION)

        app.viewModel.createInvoice()

        val invoice = app.billingService.getInvoice("INV-0001")
        assertFalse(app.viewModel.state.isSaving)
        assertEquals("INV-0001", app.viewModel.state.invoiceId)
        assertEquals("Invoice created.", app.viewModel.state.successMessage)
        assertEquals(PaymentStatus.UNPAID, app.viewModel.state.paymentStatus)
        assertEquals(350_000.0, app.viewModel.state.total)
        assertEquals(LocalDateTime.of(2026, 6, 23, 9, 30), invoice.issuedAt)
        assertEquals(listOf(ClinicService.CONSULTATION, ClinicService.VACCINATION), invoice.items.map { it.service })
    }

    @Test
    fun `missing patient from service is shown as checkout error message`() {
        val app = fixture()
        app.viewModel.startInvoice("PET-404")
        app.viewModel.toggleService(ClinicService.CONSULTATION)

        app.viewModel.createInvoice()

        assertEquals("Pet with ID PET-404 was not found.", app.viewModel.state.errorMessage)
        assertNull(app.viewModel.state.invoiceId)
        assertTrue(app.billingService.listInvoices().isEmpty())
    }

    @Test
    fun `mark paid requires pending confirmation before mutation`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Nadia Prasetyo", "0811111111")
        val patient = app.patientService.registerPet(owner.id, "Miso", "Cat")
        app.viewModel.startInvoice(patient.id)
        app.viewModel.toggleService(ClinicService.GROOMING)
        app.viewModel.createInvoice()

        app.viewModel.requestMarkPaid()

        assertEquals(BillingCheckoutAction.MARK_PAID, app.viewModel.state.pendingAction?.action)
        assertEquals(PaymentStatus.UNPAID, app.billingService.getInvoice("INV-0001").paymentStatus)

        app.viewModel.confirmPendingAction()

        assertNull(app.viewModel.state.pendingAction)
        assertEquals("Invoice marked as paid.", app.viewModel.state.successMessage)
        assertEquals(PaymentStatus.PAID, app.viewModel.state.paymentStatus)
        assertEquals(PaymentStatus.PAID, app.billingService.getInvoice("INV-0001").paymentStatus)
        assertEquals(2, app.billingService.listStatusHistory("INV-0001").size)
    }

    @Test
    fun `void invoice requires pending confirmation before mutation`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Lia Santoso", "0822222222")
        val patient = app.patientService.registerPet(owner.id, "Bento", "Dog")
        app.viewModel.startInvoice(patient.id)
        app.viewModel.toggleService(ClinicService.BASIC_TREATMENT)
        app.viewModel.createInvoice()

        app.viewModel.requestVoidInvoice()
        app.viewModel.confirmPendingAction()

        assertEquals("Invoice voided.", app.viewModel.state.successMessage)
        assertEquals(PaymentStatus.VOIDED, app.viewModel.state.paymentStatus)
        assertEquals(PaymentStatus.VOIDED, app.billingService.getInvoice("INV-0001").paymentStatus)
    }

    @Test
    fun `paid invoice cannot be voided and reports service error`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        val patient = app.patientService.registerPet(owner.id, "Darcy", "Dog")
        val invoice = app.billingService.createInvoice(patient.id, listOf(ClinicService.CONSULTATION))
        app.billingService.markAsPaid(invoice.id)
        app.viewModel.loadInvoice(invoice.id)

        app.viewModel.requestVoidInvoice()
        app.viewModel.confirmPendingAction()

        assertEquals("Paid invoices cannot be voided.", app.viewModel.state.errorMessage)
        assertEquals(PaymentStatus.PAID, app.billingService.getInvoice(invoice.id).paymentStatus)
    }

    @Test
    fun `start invoice clears loaded invoice state and can prefill patient`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Nadia Prasetyo", "0811111111")
        val patient = app.patientService.registerPet(owner.id, "Miso", "Cat")
        val invoice = app.billingService.createInvoice(patient.id, listOf(ClinicService.CONSULTATION))
        app.viewModel.loadInvoice(invoice.id)

        app.viewModel.startInvoice(patient.id)

        assertNull(app.viewModel.state.invoiceId)
        assertEquals(patient.id, app.viewModel.state.patientId)
        assertEquals(emptyList(), app.viewModel.state.selectedServices)
        assertNull(app.viewModel.state.paymentStatus)
    }

    private data class Fixture(
        val ownerService: OwnerService,
        val patientService: PatientService,
        val billingService: BillingService,
        val viewModel: BillingCheckoutViewModel
    )
}
