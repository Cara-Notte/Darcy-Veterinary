package darcy.veterinary.infrastructure.sqlite

import darcy.veterinary.domain.model.InvoiceStatusHistory
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.repository.InvoiceStatusHistoryRepository
import java.sql.ResultSet
import java.time.LocalDateTime

class SqliteInvoiceStatusHistoryRepository(
    private val connectionFactory: DatabaseConnectionFactory
) : InvoiceStatusHistoryRepository {
    override fun save(history: InvoiceStatusHistory): InvoiceStatusHistory {
        try {
            connectionFactory.openConnection().use { connection ->
                connection.prepareStatement(SAVE_SQL).use { statement ->
                    statement.setString(1, history.id)
                    statement.setString(2, history.invoiceId)
                    statement.setString(3, history.fromStatus?.name)
                    statement.setString(4, history.toStatus.name)
                    statement.setString(5, history.changedAt.toString())
                    statement.setString(6, history.reason)
                    statement.setString(7, history.invoiceId)
                    statement.setString(8, history.fromStatus?.name)
                    statement.setString(9, history.toStatus.name)
                    statement.setString(10, history.changedAt.toString())
                    statement.setString(11, history.reason)
                    statement.executeUpdate()
                }
            }
        } catch (error: Exception) {
            throw IllegalStateException("Failed to save invoice history ${history.id}.", error)
        }

        return history
    }

    override fun findAll(): List<InvoiceStatusHistory> = emptyList()

    override fun findByInvoiceId(invoiceId: String): List<InvoiceStatusHistory> = emptyList()

    private companion object {
        private const val SAVE_SQL = """
            INSERT INTO invoice_status_history (id, invoice_id, from_status, to_status, changed_at, reason)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                invoice_id = ?,
                from_status = ?,
                to_status = ?,
                changed_at = ?,
                reason = ?
        """
    }
}
