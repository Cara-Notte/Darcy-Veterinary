package darcy.veterinary.application

import darcy.veterinary.domain.exception.EntityNotFoundException
import darcy.veterinary.domain.exception.InvalidClinicOperationException
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.Invoice
import darcy.veterinary.domain.model.InvoiceItem
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.repository.InvoiceRepository
import darcy.veterinary.repository.PetRepository
import java.time.LocalDateTime

class BillingService(
    private val invoiceRepository: InvoiceRepository,
    private val petRepository: PetRepository,
    private val idGenerator: IdGenerator = UuidIdGenerator
) {
    fun createInvoice(
        petId: String,
        services: List<ClinicService>,
        issuedAt: LocalDateTime = LocalDateTime.now()
    ): Invoice {
        if (services.isEmpty()) throw InvalidClinicOperationException("Invoice must contain at least one service.")
        petRepository.findById(petId)
            ?: throw EntityNotFoundException("Pet with ID $petId was not found.")

        return invoiceRepository.save(
            Invoice(
                id = idGenerator.nextId("INV"),
                petId = petId,
                items = services.map { InvoiceItem(it) },
                issuedAt = issuedAt
            )
        )
    }

    fun markAsPaid(invoiceId: String): Invoice {
        val invoice = getInvoice(invoiceId)
        if (invoice.paymentStatus == PaymentStatus.VOIDED) {
            throw InvalidClinicOperationException("Voided invoices cannot be marked as paid.")
        }

        return invoiceRepository.save(invoice.copy(paymentStatus = PaymentStatus.PAID))
    }

    fun voidInvoice(invoiceId: String): Invoice {
        val invoice = getInvoice(invoiceId)
        if (invoice.paymentStatus == PaymentStatus.PAID) {
            throw InvalidClinicOperationException("Paid invoices cannot be voided.")
        }

        return invoiceRepository.save(invoice.copy(paymentStatus = PaymentStatus.VOIDED))
    }

    fun getInvoice(invoiceId: String): Invoice = invoiceRepository.findById(invoiceId)
        ?: throw EntityNotFoundException("Invoice with ID $invoiceId was not found.")

    fun listInvoices(): List<Invoice> = invoiceRepository.findAll()

    fun listInvoicesByPet(petId: String): List<Invoice> = invoiceRepository.findByPetId(petId)
}
