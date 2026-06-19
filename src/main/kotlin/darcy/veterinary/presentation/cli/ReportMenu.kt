package darcy.veterinary.presentation.cli

import darcy.veterinary.application.ClinicOverviewReport
import darcy.veterinary.application.ClinicReportService
import java.time.LocalDate

class ReportMenu(
    private val reportService: ClinicReportService,
    private val input: InputReader
) {
    fun show() {
        println("\nReports")
        println("1. Show today's overview")
        println("2. Show overview for another date")
        when (input.choice("Choose menu: ", 1..2)) {
            1 -> printReport(reportService.generateOverview())
            2 -> printReport(reportService.generateOverview(readDate()))
        }
    }

    private fun readDate(): LocalDate {
        while (true) {
            val value = input.text("Report date (YYYY-MM-DD): ")
            val date = runCatching { LocalDate.parse(value) }.getOrNull()
            if (date != null) return date
            println("Use ISO date format, for example 2026-06-19.")
        }
    }

    private fun printReport(report: ClinicOverviewReport) {
        println("\nClinic overview for ${report.reportDate}")
        println("Owners: ${report.totalOwners}")
        println("Pets: ${report.totalPets}")
        println("Appointments: ${report.totalAppointments}")
        println("Appointments today: ${report.todayAppointments}")
        println("Completed appointments: ${report.completedAppointments}")
        println("Cancelled appointments: ${report.cancelledAppointments}")
        println("Unpaid invoices: ${report.unpaidInvoices}")
        println("Paid invoices: ${report.paidInvoices}")
        println("Voided invoices: ${report.voidedInvoices}")
        println("Paid revenue: Rp ${report.paidRevenue.toInt()}")
    }
}
