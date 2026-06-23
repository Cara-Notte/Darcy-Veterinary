package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.ClinicOverviewReport
import darcy.veterinary.application.ClinicReportService
import java.time.LocalDate

class DashboardSummaryViewModel(
    private val reportService: ClinicReportService,
    initialDate: LocalDate = LocalDate.now()
) {
    var state: DashboardSummaryState = DashboardSummaryState(reportDate = initialDate)
        private set

    fun load() {
        loadReport(state.reportDate)
    }

    fun selectDate(date: LocalDate) {
        loadReport(date)
    }

    fun refresh() {
        loadReport(state.reportDate)
    }

    private fun loadReport(date: LocalDate) {
        state = state.copy(
            reportDate = date,
            isLoading = true,
            errorMessage = null
        )

        state = try {
            state.copy(
                isLoading = false,
                report = reportService.generateOverview(date),
                errorMessage = null
            )
        } catch (error: Exception) {
            state.copy(
                isLoading = false,
                report = null,
                errorMessage = error.message ?: "Dashboard summary could not be loaded."
            )
        }
    }
}

data class DashboardSummaryState(
    val reportDate: LocalDate,
    val isLoading: Boolean = false,
    val report: ClinicOverviewReport? = null,
    val errorMessage: String? = null
) {
    val emptyStateMessage: String?
        get() = when {
            isLoading || errorMessage != null -> null
            report == null -> "Load the dashboard summary to view clinic activity."
            report.totalOwners == 0 && report.totalPets == 0 && report.totalAppointments == 0 -> "No clinic activity has been recorded yet."
            else -> null
        }

    val hasOpenBillingWork: Boolean = (report?.unpaidInvoices ?: 0) > 0
    val hasTodaySchedule: Boolean = (report?.todayAppointments ?: 0) > 0
    val collectedRevenue: Double = report?.paidRevenue ?: 0.0
}
