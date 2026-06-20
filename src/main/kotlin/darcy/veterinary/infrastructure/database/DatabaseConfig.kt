package darcy.veterinary.infrastructure.database

import java.nio.file.Path

data class DatabaseConfig(
    val databasePath: Path = Path.of("data", "darcy-vet.db"),
    val backupDirectory: Path = Path.of("data", "backups")
)
