package darcy.veterinary.application

import darcy.veterinary.domain.exception.EntityNotFoundException
import darcy.veterinary.domain.exception.InvalidClinicOperationException
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.Invoice
import darcy.veterinary.domain.model.InvoiceItem
import darcy.veterinary.domain.model.InvoiceStatusHistory
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.repository.InvoiceRepository
import darcy.veterinary.repository.InvoiceStatusHistoryRepository
import darcy.veterinary.repository.PetRepository
import java.time.LocalDateTime

class BillingService(
    private val invoiceRepository: InvoiceRepository,
    private val petRepository: PetRepository,
    private val idGenerator: IdGenerator = UuidIdGenerator,
    private val statusHistoryRepository: InvoiceStatusHistoryRepository? = null
) {
    fun createInvoice(
        petId: String,
        services: List<ClinicService>,
        issuedAt: LocalDateTime = LocalDateTime.now()
    ): Invoice {
        if (services.isEmpty()) throw InvalidClinicOperationException("Invoice must contain at least one service.")
        petRepository.findById(petId)
            ?: throw EntityNotFoundException("Pet with ID $petId was not found.")

        val invoice = invoiceRepository.save(
            Invoice(
                id = idGenerator.nextId("INV"),
                petId = petId,
                items = services.map { InvoiceItem(it) },
                issuedAt = issuedAt
            )
        )
        recordStatusChange(invoice.id, null, invoice.paymentStatus, "Invoice created")
        return invoice
    }

    fun markAsPaid(invoiceId: String): Invoice {
        val invoice = getInvoice(invoiceId)
        if (invoice.paymentStatus == PaymentStatus.VOIDED) {
            throw InvalidClinicOperationException("Voided invoices cannot be marked as paid.")
        }

        val paid = invoiceRepository.save(invoice.copy(paymentStatus = PaymentStatus.PAID))
        recordStatusChange(invoice.id, invoice.paymentStatus, PaymentStatus.PAID, "Invoice paid")
        return paid
    }

    fun voidInvoice(invoiceId: String): Invoice {
        val invoice = getInvoice(invoiceId)
        if (invoice.paymentStatus == PaymentStatus.PAID) {
            throw InvalidClinicOperationException("Paid invoices cannot be voided.")
        }

        val voided = invoiceRepository.save(invoice.copy(paymentStatus = PaymentStatus.VOIDED))
        recordStatusChange(invoice.id, invoice.paymentStatus, PaymentStatus.VOIDED, "Invoice voided")
        return voided
    }

    fun getInvoice(invoiceId: String): Invoice = invoiceRepository.findById(invoiceId)
        ?: throw EntityNotFoundException("Invoice with ID $invoiceId was not found.")

    fun listInvoices(): List<Invoice> = invoiceRepository.findAll()

    fun listInvoicesByPet(petId: String): List<Invoice> = invoiceRepository.findByPetId(petId)

    fun listStatusHistory(invoiceId: String): List<InvoiceStatusHistory> =
        statusHistoryRepository?.findByInvoiceId(invoiceId) ?: emptyList()

    private fun recordStatusChange(
        invoiceId: String,
        fromStatus: PaymentStatus?,
        toStatus: PaymentStatus,
        reason: String
    ) {
        statusHistoryRepository?.save(
            InvoiceStatusHistory(
                id = idGenerator.nextId("HIS"),
                invoiceId = invoiceId,
                fromStatus = fromStatus,
                toStatus = toStatus,
                changedAt = LocalDateTime.now(),
                reason = reason
            )
        )
    }
}
