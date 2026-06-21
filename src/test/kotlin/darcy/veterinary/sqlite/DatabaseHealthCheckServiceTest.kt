package darcy.veterinary.sqlite

import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.infrastructure.database.DatabaseHealthCheckService
import darcy.veterinary.infrastructure.database.DatabaseMigrator
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DatabaseHealthCheckServiceTest
