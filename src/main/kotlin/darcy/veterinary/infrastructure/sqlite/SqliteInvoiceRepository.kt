package darcy.veterinary.infrastructure.sqlite

import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.Invoice
import darcy.veterinary.domain.model.InvoiceItem
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.repository.InvoiceRepository
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDateTime

class SqliteInvoiceRepository(
    private val connectionFactory: DatabaseConnectionFactory
) : InvoiceRepository {
    override fun save(invoice: Invoice): Invoice {
        val now = Instant.now().toString()

        connectionFactory.openConnection().use { connection ->
            val originalAutoCommit = connection.autoCommit
            connection.autoCommit = false

            try {
                upsertInvoice(connection, invoice, now)
                replaceInvoiceItems(connection, invoice)
                connection.commit()
            } catch (error: SQLException) {
                connection.rollback()
                throw IllegalStateException("Failed to save invoice ${invoice.id}.", error)
            } finally {
                connection.autoCommit = originalAutoCommit
            }
        }

        return invoice
    }

    override fun findById(id: String): Invoice? =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_BY_ID_SQL).use { statement ->
                statement.setString(1, id)
                statement.executeQuery().use { result ->
                    if (result.next()) result.toInvoice(connection) else null
                }
            }
        }

    override fun findAll(): List<Invoice> =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_ALL_SQL).use { statement ->
                statement.executeQuery().use { result ->
                    result.toInvoiceList(connection)
                }
            }
        }

    override fun findByPetId(petId: String): List<Invoice> =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_BY_PET_ID_SQL).use { statement ->
                statement.setString(1, petId)
                statement.executeQuery().use { result ->
                    result.toInvoiceList(connection)
                }
            }
        }

    private fun upsertInvoice(connection: Connection, invoice: Invoice, now: String) {
        connection.prepareStatement(SAVE_SQL).use { statement ->
            statement.setString(1, invoice.id)
            statement.setString(2, invoice.petId)
            statement.setString(3, invoice.issuedAt.toString())
            statement.setString(4, invoice.paymentStatus.name)
            statement.setString(5, now)
            statement.setString(6, now)
            statement.setString(7, invoice.petId)
            statement.setString(8, invoice.issuedAt.toString())
            statement.setString(9, invoice.paymentStatus.name)
            statement.setString(10, now)
            statement.executeUpdate()
        }
    }

    private fun replaceInvoiceItems(connection: Connection, invoice: Invoice) {
        connection.prepareStatement("DELETE FROM invoice_items WHERE invoice_id = ?").use { statement ->
            statement.setString(1, invoice.id)
            statement.executeUpdate()
        }

        connection.prepareStatement(INSERT_ITEM_SQL).use { statement ->
            invoice.items.forEach { item ->
                statement.setString(1, invoice.id)
                statement.setString(2, item.service.name)
                statement.setString(3, item.description)
                statement.setDouble(4, item.cost)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun ResultSet.toInvoice(connection: Connection): Invoice {
        val invoiceId = getString("id")
        return Invoice(
            id = invoiceId,
            petId = getString("pet_id"),
            items = connection.findInvoiceItems(invoiceId),
            issuedAt = LocalDateTime.parse(getString("issued_at")),
            paymentStatus = PaymentStatus.valueOf(getString("payment_status"))
        )
    }

    private fun ResultSet.toInvoiceList(connection: Connection): List<Invoice> = buildList {
        while (next()) {
            add(toInvoice(connection))
        }
    }

    private fun Connection.findInvoiceItems(invoiceId: String): List<InvoiceItem> =
        prepareStatement(FIND_ITEMS_SQL).use { statement ->
            statement.setString(1, invoiceId)
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            InvoiceItem(
                                service = ClinicService.valueOf(result.getString("service_code")),
                                description = result.getString("service_display_name"),
                                cost = result.getDouble("price")
                            )
                        )
                    }
                }
            }
        }

    private companion object {
        private const val SAVE_SQL = """
            INSERT INTO invoices (id, pet_id, issued_at, payment_status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                pet_id = ?,
                issued_at = ?,
                payment_status = ?,
                updated_at = ?
        """

        private const val INSERT_ITEM_SQL = """
            INSERT INTO invoice_items (invoice_id, service_code, service_display_name, price)
            VALUES (?, ?, ?, ?)
        """

        private const val FIND_BY_ID_SQL = """
            SELECT id, pet_id, issued_at, payment_status
            FROM invoices
            WHERE id = ?
        """

        private const val FIND_ALL_SQL = """
            SELECT id, pet_id, issued_at, payment_status
            FROM invoices
            ORDER BY rowid
        """

        private const val FIND_BY_PET_ID_SQL = """
            SELECT id, pet_id, issued_at, payment_status
            FROM invoices
            WHERE pet_id = ?
            ORDER BY rowid
        """

        private const val FIND_ITEMS_SQL = """
            SELECT service_code, service_display_name, price
            FROM invoice_items
            WHERE invoice_id = ?
            ORDER BY id
        """
    }
}
