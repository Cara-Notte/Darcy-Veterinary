package darcy.veterinary.presentation.cli

import darcy.veterinary.application.BillingService
import darcy.veterinary.domain.model.ClinicService

class BillingMenu(
    private val billingService: BillingService,
    private val input: InputReader
) {
    fun show() {
        println("\nBilling")
        println("1. Create invoice")
        println("2. Mark invoice as paid")
        println("3. List invoices")
        when (input.choice("Choose menu: ", 1..3)) {
            1 -> create()
            2 -> markPaid()
            3 -> list()
        }
    }

    private fun create() {
        val petId = input.text("Pet ID: ")
        println("Available services:")
        ClinicService.values().forEachIndexed { index, service ->
            println("${index + 1}. ${service.displayName} - Rp ${service.defaultCost.toInt()}")
        }
        val selections = input.text("Service numbers, separated by comma: ")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .mapNotNull { ClinicService.values().getOrNull(it - 1) }

        val invoice = billingService.createInvoice(petId, selections)
        println("Invoice created: ${invoice.id} | Total: Rp ${invoice.total().toInt()}")
    }

    private fun markPaid() {
        val invoice = billingService.markAsPaid(input.text("Invoice ID: "))
        println("Invoice marked as paid: ${invoice.id}")
    }

    private fun list() {
        billingService.listInvoices().forEach { invoice ->
            println("${invoice.id} | Pet: ${invoice.petId} | Rp ${invoice.total().toInt()} | ${invoice.paymentStatus}")
        }
    }
}
