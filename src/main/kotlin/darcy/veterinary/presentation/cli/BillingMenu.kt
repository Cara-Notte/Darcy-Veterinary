package darcy.veterinary.presentation.cli

import darcy.veterinary.application.BillingService
import darcy.veterinary.application.PatientService
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.Invoice
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.domain.model.Pet

class BillingMenu(
    private val billingService: BillingService,
    private val patientService: PatientService,
    private val input: InputReader
) {
    private val selector = CliListSelector(input)

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
        val pet = selector.choose(
            title = "Available pets",
            items = patientService.listPets(),
            emptyMessage = "No pets registered yet. Register a pet before creating an invoice.",
            prompt = "Select pet: ",
            formatter = Pet::summary
        ) ?: return

        println("Available services:")
        ClinicService.values().forEachIndexed { index, service ->
            println("${index + 1}. ${service.displayName} - Rp ${service.defaultCost.toInt()}")
        }
        val selections = input.text("Service numbers, separated by comma: ")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .mapNotNull { ClinicService.values().getOrNull(it - 1) }

        val invoice = billingService.createInvoice(pet.id, selections)
        println("Invoice created: ${invoice.id} | Total: Rp ${invoice.total().toInt()}")
    }

    private fun markPaid() {
        val invoice = selector.choose(
            title = "Unpaid invoices",
            items = billingService.listInvoices().filter { it.paymentStatus == PaymentStatus.UNPAID },
            emptyMessage = "No unpaid invoices available.",
            prompt = "Select invoice: ",
            formatter = Invoice::summary
        ) ?: return

        val paid = billingService.markAsPaid(invoice.id)
        println("Invoice marked as paid: ${paid.id}")
    }

    private fun list() {
        selector.show(
            title = "Invoices",
            items = billingService.listInvoices(),
            emptyMessage = "No invoices created yet.",
            formatter = Invoice::summary
        )
    }

    private fun Pet.summary(): String = "$id | $name | $species | Owner: $ownerId"

    private fun Invoice.summary(): String = "$id | Pet: $petId | Rp ${total().toInt()} | $paymentStatus"
}
