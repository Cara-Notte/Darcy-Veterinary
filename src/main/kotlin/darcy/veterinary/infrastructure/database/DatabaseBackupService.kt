package darcy.veterinary.infrastructure.database

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DatabaseBackupService(
    private val config: DatabaseConfig = DatabaseConfig(),
    private val clock: Clock = Clock.systemDefaultZone()
) {
    fun createBackup(): Path {
        require(Files.exists(config.databasePath)) {
            "Database file does not exist: ${config.databasePath}"
        }

        Files.createDirectories(config.backupDirectory)
        val backupPath = config.backupDirectory.resolve("darcy-vet-${timestamp()}.db")
        Files.copy(config.databasePath, backupPath, StandardCopyOption.REPLACE_EXISTING)
        return backupPath
    }

    fun restoreFrom(backupPath: Path) {
        require(Files.exists(backupPath)) {
            "Backup file does not exist: $backupPath"
        }

        config.databasePath.parent?.let(Files::createDirectories)
        Files.copy(backupPath, config.databasePath, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun timestamp(): String = LocalDateTime
        .now(clock)
        .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
}
