package darcy.veterinary.infrastructure.database

import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager

class DatabaseConnectionFactory(
    private val config: DatabaseConfig
) {
    fun openConnection(): Connection {
        config.databasePath.parent?.let { parent ->
            Files.createDirectories(parent)
        }

        return DriverManager.getConnection("jdbc:sqlite:" + config.databasePath.toString()).also { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA foreign_keys = ON")
            }
        }
    }
}
