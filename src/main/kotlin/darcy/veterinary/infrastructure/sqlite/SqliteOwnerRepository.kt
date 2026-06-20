package darcy.veterinary.infrastructure.sqlite

import darcy.veterinary.domain.model.Owner
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.repository.OwnerRepository
import java.sql.ResultSet
import java.sql.SQLException
import java.time.Instant

class SqliteOwnerRepository(
    private val connectionFactory: DatabaseConnectionFactory
) : OwnerRepository {
    override fun save(owner: Owner): Owner {
        val now = Instant.now().toString()

        try {
            connectionFactory.openConnection().use { connection ->
                connection.prepareStatement(SAVE_SQL).use { statement ->
                    statement.setString(1, owner.id)
                    statement.setString(2, owner.fullName)
                    statement.setString(3, owner.phoneNumber)
                    statement.setString(4, owner.email)
                    statement.setString(5, now)
                    statement.setString(6, now)
                    statement.setString(7, owner.fullName)
                    statement.setString(8, owner.phoneNumber)
                    statement.setString(9, owner.email)
                    statement.setString(10, now)
                    statement.executeUpdate()
                }
            }
        } catch (error: SQLException) {
            throw IllegalStateException("Failed to save owner ${owner.id}.", error)
        }

        return owner
    }

    override fun findById(id: String): Owner? =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_BY_ID_SQL).use { statement ->
                statement.setString(1, id)
                statement.executeQuery().use { result ->
                    if (result.next()) result.toOwner() else null
                }
            }
        }

    override fun findAll(): List<Owner> =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_ALL_SQL).use { statement ->
                statement.executeQuery().use { result ->
                    result.toOwnerList()
                }
            }
        }

    override fun search(keyword: String): List<Owner> {
        val query = keyword.trim()
        if (query.isBlank()) return emptyList()

        val pattern = "%$query%"

        return connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(SEARCH_SQL).use { statement ->
                statement.setString(1, pattern)
                statement.setString(2, pattern)
                statement.setString(3, pattern)
                statement.executeQuery().use { result ->
                    result.toOwnerList()
                }
            }
        }
    }

    private fun ResultSet.toOwner(): Owner = Owner(
        id = getString("id"),
        fullName = getString("full_name"),
        phoneNumber = getString("phone_number"),
        email = getString("email")
    )

    private fun ResultSet.toOwnerList(): List<Owner> = buildList {
        while (next()) {
            add(toOwner())
        }
    }

    private companion object {
        private const val SAVE_SQL = """
            INSERT INTO owners (id, full_name, phone_number, email, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                full_name = ?,
                phone_number = ?,
                email = ?,
                updated_at = ?
        """

        private const val FIND_BY_ID_SQL = """
            SELECT id, full_name, phone_number, email
            FROM owners
            WHERE id = ?
        """

        private const val FIND_ALL_SQL = """
            SELECT id, full_name, phone_number, email
            FROM owners
            ORDER BY rowid
        """

        private const val SEARCH_SQL = """
            SELECT id, full_name, phone_number, email
            FROM owners
            WHERE LOWER(full_name) LIKE LOWER(?)
                OR phone_number LIKE ?
                OR LOWER(COALESCE(email, '')) LIKE LOWER(?)
            ORDER BY rowid
        """
    }
}
