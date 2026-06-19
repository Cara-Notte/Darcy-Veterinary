package darcy.veterinary.domain.model

import java.time.LocalDateTime

data class InvoiceStatusHistory(
    val id: String,
    val invoiceId: String,
    val fromStatus: PaymentStatus?,
    val toStatus: PaymentStatus,
    val changedAt: LocalDateTime,
    val reason: String
)
