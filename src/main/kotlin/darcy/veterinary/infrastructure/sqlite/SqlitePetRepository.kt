package darcy.veterinary.infrastructure.sqlite

import darcy.veterinary.domain.model.Pet
import darcy.veterinary.domain.model.PetSex
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.repository.PetRepository
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.time.Instant
import java.time.LocalDate

class SqlitePetRepository(
    private val connectionFactory: DatabaseConnectionFactory
) : PetRepository {
    override fun save(pet: Pet): Pet {
        val now = Instant.now().toString()

        connectionFactory.openConnection().use { connection ->
            val originalAutoCommit = connection.autoCommit
            connection.autoCommit = false

            try {
                upsertPet(connection, pet, now)
                replacePetValues(connection, pet.id, "DELETE FROM pet_allergies WHERE pet_id = ?", pet.allergies, INSERT_ALLERGY_SQL)
                replacePetValues(connection, pet.id, "DELETE FROM pet_medical_conditions WHERE pet_id = ?", pet.medicalConditions, INSERT_MEDICAL_CONDITION_SQL)
                connection.commit()
            } catch (error: SQLException) {
                connection.rollback()
                throw IllegalStateException("Failed to save pet ${pet.id}.", error)
            } finally {
                connection.autoCommit = originalAutoCommit
            }
        }

        return pet
    }

    override fun findById(id: String): Pet? =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_BY_ID_SQL).use { statement ->
                statement.setString(1, id)
                statement.executeQuery().use { result ->
                    if (result.next()) result.toPet(connection) else null
                }
            }
        }

    override fun findAll(): List<Pet> =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_ALL_SQL).use { statement ->
                statement.executeQuery().use { result ->
                    result.toPetList(connection)
                }
            }
        }

    override fun findByOwnerId(ownerId: String): List<Pet> =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_BY_OWNER_ID_SQL).use { statement ->
                statement.setString(1, ownerId)
                statement.executeQuery().use { result ->
                    result.toPetList(connection)
                }
            }
        }

    override fun search(keyword: String): List<Pet> {
        val query = keyword.trim()
        if (query.isBlank()) return emptyList()

        val pattern = "%$query%"

        return connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(SEARCH_SQL).use { statement ->
                statement.setString(1, pattern)
                statement.setString(2, pattern)
                statement.setString(3, pattern)
                statement.executeQuery().use { result ->
                    result.toPetList(connection)
                }
            }
        }
    }

    private fun upsertPet(connection: Connection, pet: Pet, now: String) {
        connection.prepareStatement(SAVE_SQL).use { statement ->
            statement.setString(1, pet.id)
            statement.setString(2, pet.ownerId)
            statement.setString(3, pet.name)
            statement.setString(4, pet.species)
            statement.setString(5, pet.breed)
            if (pet.age == null) statement.setNull(6, Types.INTEGER) else statement.setInt(6, pet.age)
            statement.setString(7, pet.sex?.name)
            statement.setString(8, pet.dateOfBirth?.toString())
            if (pet.weightKg == null) statement.setNull(9, Types.REAL) else statement.setDouble(9, pet.weightKg)
            statement.setString(10, now)
            statement.setString(11, now)
            statement.setString(12, pet.ownerId)
            statement.setString(13, pet.name)
            statement.setString(14, pet.species)
            statement.setString(15, pet.breed)
            if (pet.age == null) statement.setNull(16, Types.INTEGER) else statement.setInt(16, pet.age)
            statement.setString(17, pet.sex?.name)
            statement.setString(18, pet.dateOfBirth?.toString())
            if (pet.weightKg == null) statement.setNull(19, Types.REAL) else statement.setDouble(19, pet.weightKg)
            statement.setString(20, now)
            statement.executeUpdate()
        }
    }

    private fun replacePetValues(
        connection: Connection,
        petId: String,
        deleteSql: String,
        values: List<String>,
        insertSql: String
    ) {
        connection.prepareStatement(deleteSql).use { statement ->
            statement.setString(1, petId)
            statement.executeUpdate()
        }

        connection.prepareStatement(insertSql).use { statement ->
            values.forEach { value ->
                statement.setString(1, petId)
                statement.setString(2, value)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun ResultSet.toPet(connection: Connection): Pet = Pet(
        id = getString("id"),
        ownerId = getString("owner_id"),
        name = getString("name"),
        species = getString("species"),
        breed = getString("breed"),
        age = getNullableInt("age"),
        sex = getString("sex")?.let(PetSex::valueOf),
        dateOfBirth = getString("date_of_birth")?.let(LocalDate::parse),
        weightKg = getNullableDouble("weight_kg"),
        allergies = connection.findPetValues("SELECT allergy FROM pet_allergies WHERE pet_id = ? ORDER BY id", getString("id")),
        medicalConditions = connection.findPetValues("SELECT condition_name FROM pet_medical_conditions WHERE pet_id = ? ORDER BY id", getString("id"))
    )

    private fun ResultSet.getNullableInt(column: String): Int? {
        val value = getInt(column)
        return if (wasNull()) null else value
    }

    private fun ResultSet.getNullableDouble(column: String): Double? {
        val value = getDouble(column)
        return if (wasNull()) null else value
    }

    private fun ResultSet.toPetList(connection: Connection): List<Pet> = buildList {
        while (next()) {
            add(toPet(connection))
        }
    }

    private fun Connection.findPetValues(sql: String, petId: String): List<String> =
        prepareStatement(sql).use { statement ->
            statement.setString(1, petId)
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(result.getString(1))
                    }
                }
            }
        }

    private companion object {
        private const val SAVE_SQL = """
            INSERT INTO pets (id, owner_id, name, species, breed, age, sex, date_of_birth, weight_kg, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                owner_id = ?,
                name = ?,
                species = ?,
                breed = ?,
                age = ?,
                sex = ?,
                date_of_birth = ?,
                weight_kg = ?,
                updated_at = ?
        """

        private const val INSERT_ALLERGY_SQL = """
            INSERT INTO pet_allergies (pet_id, allergy)
            VALUES (?, ?)
        """

        private const val INSERT_MEDICAL_CONDITION_SQL = """
            INSERT INTO pet_medical_conditions (pet_id, condition_name)
            VALUES (?, ?)
        """

        private const val FIND_BY_ID_SQL = """
            SELECT id, owner_id, name, species, breed, age, sex, date_of_birth, weight_kg
            FROM pets
            WHERE id = ?
        """

        private const val FIND_ALL_SQL = """
            SELECT id, owner_id, name, species, breed, age, sex, date_of_birth, weight_kg
            FROM pets
            ORDER BY rowid
        """

        private const val FIND_BY_OWNER_ID_SQL = """
            SELECT id, owner_id, name, species, breed, age, sex, date_of_birth, weight_kg
            FROM pets
            WHERE owner_id = ?
            ORDER BY rowid
        """

        private const val SEARCH_SQL = """
            SELECT id, owner_id, name, species, breed, age, sex, date_of_birth, weight_kg
            FROM pets
            WHERE LOWER(name) LIKE LOWER(?)
                OR LOWER(species) LIKE LOWER(?)
                OR LOWER(COALESCE(breed, '')) LIKE LOWER(?)
            ORDER BY rowid
        """
    }
}
