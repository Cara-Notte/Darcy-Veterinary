package darcy.veterinary.infrastructure.memory

import darcy.veterinary.domain.model.Invoice
import darcy.veterinary.repository.InvoiceRepository

class InMemoryInvoiceRepository : InvoiceRepository {
    private val invoices = linkedMapOf<String, Invoice>()

    override fun save(invoice: Invoice): Invoice {
        invoices[invoice.id] = invoice
        return invoice
    }

    override fun findById(id: String): Invoice? = invoices[id]

    override fun findAll(): List<Invoice> = invoices.values.toList()

    override fun findByPetId(petId: String): List<Invoice> = invoices.values.filter { it.petId == petId }
}
