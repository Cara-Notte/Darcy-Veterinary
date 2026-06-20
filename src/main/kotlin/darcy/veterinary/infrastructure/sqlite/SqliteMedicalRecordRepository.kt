package darcy.veterinary.infrastructure.sqlite

import darcy.veterinary.domain.model.MedicalRecord
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.repository.MedicalRecordRepository
import java.sql.ResultSet
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDateTime

class SqliteMedicalRecordRepository(
    private val connectionFactory: DatabaseConnectionFactory
) : MedicalRecordRepository {
    override fun save(record: MedicalRecord): MedicalRecord {
        val now = Instant.now().toString()

        try {
            connectionFactory.openConnection().use { connection ->
                connection.prepareStatement(SAVE_SQL).use { statement ->
                    statement.setString(1, record.id)
                    statement.setString(2, record.petId)
                    statement.setString(3, record.appointmentId)
                    statement.setString(4, record.diagnosis)
                    statement.setString(5, record.treatment)
                    statement.setString(6, record.notes)
                    statement.setString(7, record.recordedAt.toString())
                    statement.setString(8, record.veterinarianName)
                    statement.setString(9, now)
                    statement.setString(10, now)
                    statement.setString(11, record.petId)
                    statement.setString(12, record.appointmentId)
                    statement.setString(13, record.diagnosis)
                    statement.setString(14, record.treatment)
                    statement.setString(15, record.notes)
                    statement.setString(16, record.recordedAt.toString())
                    statement.setString(17, record.veterinarianName)
                    statement.setString(18, now)
                    statement.executeUpdate()
                }
            }
        } catch (error: SQLException) {
            throw IllegalStateException("Failed to save medical record ${record.id}.", error)
        }

        return record
    }

    override fun findById(id: String): MedicalRecord? =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_BY_ID_SQL).use { statement ->
                statement.setString(1, id)
                statement.executeQuery().use { result ->
                    if (result.next()) result.toMedicalRecord() else null
                }
            }
        }

    override fun findAll(): List<MedicalRecord> =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_ALL_SQL).use { statement ->
                statement.executeQuery().use { result ->
                    result.toMedicalRecordList()
                }
            }
        }

    override fun findByPetId(petId: String): List<MedicalRecord> =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_BY_PET_ID_SQL).use { statement ->
                statement.setString(1, petId)
                statement.executeQuery().use { result ->
                    result.toMedicalRecordList()
                }
            }
        }

    private fun ResultSet.toMedicalRecord(): MedicalRecord = MedicalRecord(
        id = getString("id"),
        petId = getString("pet_id"),
        appointmentId = getString("appointment_id"),
        diagnosis = getString("diagnosis"),
        treatment = getString("treatment"),
        notes = getString("notes"),
        recordedAt = LocalDateTime.parse(getString("recorded_at")),
        veterinarianName = getString("veterinarian_name")
    )

    private fun ResultSet.toMedicalRecordList(): List<MedicalRecord> = buildList {
        while (next()) {
            add(toMedicalRecord())
        }
    }

    private companion object {
        private const val SAVE_SQL = """
            INSERT INTO medical_records (id, pet_id, appointment_id, diagnosis, treatment, notes, recorded_at, veterinarian_name, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                pet_id = ?,
                appointment_id = ?,
                diagnosis = ?,
                treatment = ?,
                notes = ?,
                recorded_at = ?,
                veterinarian_name = ?,
                updated_at = ?
        """

        private const val FIND_BY_ID_SQL = """
            SELECT id, pet_id, appointment_id, diagnosis, treatment, notes, recorded_at, veterinarian_name
            FROM medical_records
            WHERE id = ?
        """

        private const val FIND_ALL_SQL = """
            SELECT id, pet_id, appointment_id, diagnosis, treatment, notes, recorded_at, veterinarian_name
            FROM medical_records
            ORDER BY rowid
        """

        private const val FIND_BY_PET_ID_SQL = """
            SELECT id, pet_id, appointment_id, diagnosis, treatment, notes, recorded_at, veterinarian_name
            FROM medical_records
            WHERE pet_id = ?
            ORDER BY rowid
        """
    }
}
