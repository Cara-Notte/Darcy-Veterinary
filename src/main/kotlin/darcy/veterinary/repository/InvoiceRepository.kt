package darcy.veterinary.repository

import darcy.veterinary.domain.model.Invoice

interface InvoiceRepository {
    fun save(invoice: Invoice): Invoice
    fun findById(id: String): Invoice?
    fun findAll(): List<Invoice>
    fun findByPetId(petId: String): List<Invoice>
}
