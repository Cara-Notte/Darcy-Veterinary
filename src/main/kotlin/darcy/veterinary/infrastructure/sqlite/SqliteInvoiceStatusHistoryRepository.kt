package darcy.veterinary.infrastructure.sqlite

import darcy.veterinary.domain.model.InvoiceStatusHistory
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.repository.InvoiceStatusHistoryRepository
import java.sql.ResultSet

class SqliteInvoiceStatusHistoryRepository(
    private val connectionFactory: DatabaseConnectionFactory
) : InvoiceStatusHistoryRepository {
    override fun save(history: InvoiceStatusHistory): InvoiceStatusHistory = history

    override fun findAll(): List<InvoiceStatusHistory> = emptyList()

    override fun findByInvoiceId(invoiceId: String): List<InvoiceStatusHistory> = emptyList()
}
