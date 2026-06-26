package darcy.veterinary.application

import darcy.veterinary.infrastructure.database.DatabaseBackupService
import darcy.veterinary.infrastructure.database.DatabaseHealthReport
import darcy.veterinary.infrastructure.database.DatabaseHealthCheckService
import java.nio.file.Path

class AdminMaintenanceService(
    private val healthCheckService: DatabaseHealthCheckService,
    private val backupService: DatabaseBackupService
) {
    fun checkDatabaseHealth(): DatabaseHealthReport = healthCheckService.check()

    fun createBackup(): Path = backupService.createBackup()
}
