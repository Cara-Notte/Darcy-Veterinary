package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.AdminMaintenanceService
import darcy.veterinary.infrastructure.database.DatabaseHealthCheck
import java.nio.file.Path

class AdminMaintenanceViewModel(
    private val adminMaintenanceService: AdminMaintenanceService
) {
    var state: AdminMaintenanceState = AdminMaintenanceState()
        private set

    fun checkHealth() {
        state = state.copy(isLoading = true, errorMessage = null, successMessage = null)
        state = try {
            val report = adminMaintenanceService.checkDatabaseHealth()
            state.copy(
                healthChecks = report.checks.map { it.toViewData() },
                isHealthy = report.healthy,
                isLoading = false,
                successMessage = if (report.healthy) "Database health check passed." else null,
                errorMessage = if (report.healthy) null else "Database health check found issues."
            )
        } catch (error: Exception) {
            state.copy(
                isLoading = false,
                errorMessage = error.message ?: "Database health check failed.",
                successMessage = null
            )
        }
    }

    fun createBackup() {
        state = state.copy(isLoading = true, errorMessage = null, successMessage = null)
        state = try {
            val backupPath = adminMaintenanceService.createBackup()
            state.copy(
                isLoading = false,
                lastBackupPath = backupPath,
                successMessage = "Backup created.",
                errorMessage = null
            )
        } catch (error: Exception) {
            state.copy(
                isLoading = false,
                errorMessage = error.message ?: "Backup could not be created.",
                successMessage = null
            )
        }
    }

    private fun DatabaseHealthCheck.toViewData(): DatabaseHealthCheckRow = DatabaseHealthCheckRow(
        name = name,
        passed = passed,
        message = message
    )
}

data class AdminMaintenanceState(
    val healthChecks: List<DatabaseHealthCheckRow> = emptyList(),
    val isHealthy: Boolean? = null,
    val isLoading: Boolean = false,
    val lastBackupPath: Path? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val canRunActions: Boolean = !isLoading
}

data class DatabaseHealthCheckRow(
    val name: String,
    val passed: Boolean,
    val message: String
)
