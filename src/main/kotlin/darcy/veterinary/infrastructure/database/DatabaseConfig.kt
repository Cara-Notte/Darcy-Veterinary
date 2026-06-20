package darcy.veterinary.infrastructure.database

import java.nio.file.Path

data class DatabaseConfig(
    val databasePath: Path,
    val backupDirectory: Path
)
