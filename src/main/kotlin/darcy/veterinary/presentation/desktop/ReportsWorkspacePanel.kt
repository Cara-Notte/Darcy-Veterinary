package darcy.veterinary.presentation.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import darcy.veterinary.application.ClinicOverviewReport
import darcy.veterinary.presentation.desktop.theme.DarcyColor
import darcy.veterinary.presentation.desktop.viewmodel.DashboardSummaryState

@Composable
internal fun ReportsWorkspacePanel(
    state: DashboardSummaryState,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRefresh) { Text("Refresh reports") }
        }
        state.report?.let { report ->
            OperationalSummaryPanel(report)
            RevenueAndBillingPanel(report)
            AppointmentStatusPanel(report)
        } ?: EmptyState(state.emptyStateMessage ?: "No report data loaded yet.")
        state.errorMessage?.let { ErrorState(it) }
    }
}

@Composable
private fun OperationalSummaryPanel(report: ClinicOverviewReport) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Clinic overview",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = DarcyColor.TextPrimary
            )
            MutedText("Report date: ${report.reportDate}")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Owners", report.totalOwners.toString(), Modifier.weight(1f))
                MetricCard("Patients", report.totalPets.toString(), Modifier.weight(1f))
                MetricCard("Appointments", report.totalAppointments.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RevenueAndBillingPanel(report: ClinicOverviewReport) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Billing snapshot",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = DarcyColor.TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Paid revenue", formatReportMoney(report.paidRevenue), Modifier.weight(1f))
                MetricCard("Paid invoices", report.paidInvoices.toString(), Modifier.weight(1f))
                MetricCard("Unpaid invoices", report.unpaidInvoices.toString(), Modifier.weight(1f))
                MetricCard("Voided", report.voidedInvoices.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AppointmentStatusPanel(report: ClinicOverviewReport) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Appointment status",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = DarcyColor.TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Today", report.todayAppointments.toString(), Modifier.weight(1f))
                MetricCard("Completed", report.completedAppointments.toString(), Modifier.weight(1f))
                MetricCard("Cancelled", report.cancelledAppointments.toString(), Modifier.weight(1f))
            }
        }
    }
}

private fun formatReportMoney(value: Double): String = "Rp %,.0f".format(value)
