package darcy.veterinary.repository

import darcy.veterinary.domain.model.InvoiceStatusHistory

interface InvoiceStatusHistoryRepository {
    fun save(history: InvoiceStatusHistory): InvoiceStatusHistory
    fun findAll(): List<InvoiceStatusHistory>
    fun findByInvoiceId(invoiceId: String): List<InvoiceStatusHistory>
}
