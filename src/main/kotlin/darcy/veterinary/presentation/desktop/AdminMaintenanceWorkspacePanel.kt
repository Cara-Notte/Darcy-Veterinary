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
import darcy.veterinary.presentation.desktop.theme.DarcyColor
import darcy.veterinary.presentation.desktop.viewmodel.AdminMaintenanceState
import darcy.veterinary.presentation.desktop.viewmodel.DatabaseHealthCheckRow

@Composable
internal fun AdminMaintenanceWorkspacePanel(
    state: AdminMaintenanceState,
    onCheckHealth: () -> Unit,
    onCreateBackup: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onCheckHealth, enabled = state.canRunActions) { Text("Run health check") }
            Button(onClick = onCreateBackup, enabled = state.canRunActions) { Text("Create backup") }
        }
        MaintenanceStatusPanel(state)
        HealthCheckPanel(state.healthChecks)
        BackupPanel(state)
    }
}

@Composable
private fun MaintenanceStatusPanel(state: AdminMaintenanceState) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Maintenance status",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = DarcyColor.TextPrimary
            )
            when {
                state.isLoading -> MutedText("Running maintenance action...")
                state.successMessage != null -> Text(state.successMessage, color = DarcyColor.ClinicalAmber, fontWeight = FontWeight.Bold)
                state.errorMessage != null -> ErrorState(state.errorMessage)
                else -> MutedText("Run a database health check or create a manual backup.")
            }
            state.isHealthy?.let { healthy ->
                MutedText(if (healthy) "Database health: healthy" else "Database health: issues found")
            }
        }
    }
}

@Composable
private fun HealthCheckPanel(checks: List<DatabaseHealthCheckRow>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Database health checks",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = DarcyColor.TextPrimary
            )
            if (checks.isEmpty()) {
                MutedText("No health check has been run in this session.")
            } else {
                checks.forEach { check -> HealthCheckRow(check) }
            }
        }
    }
}

@Composable
private fun HealthCheckRow(check: DatabaseHealthCheckRow) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (check.passed) "OK" else "FAIL",
            color = if (check.passed) DarcyColor.ClinicalAmber else DarcyColor.SemanticRed,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.18f)
        )
        Text(check.name, color = DarcyColor.TextPrimary, modifier = Modifier.weight(0.38f))
        Text(check.message, color = DarcyColor.TextSecondary, modifier = Modifier.weight(0.44f))
    }
}

@Composable
private fun BackupPanel(state: AdminMaintenanceState) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Backup",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = DarcyColor.TextPrimary
            )
            state.lastBackupPath?.let { path ->
                MutedText("Last backup path: $path")
            } ?: MutedText("No backup has been created in this session.")
            MutedText("Restore, import, and export should remain guarded actions in later Admin slices.")
        }
    }
}
