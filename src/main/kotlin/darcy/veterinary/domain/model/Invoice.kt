package darcy.veterinary.domain.model

import java.time.LocalDateTime

data class Invoice(
    val id: String,
    val petId: String,
    val items: List<InvoiceItem>,
    val issuedAt: LocalDateTime,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID
) {
    fun total(): Double = items.sumOf { it.cost }
}
