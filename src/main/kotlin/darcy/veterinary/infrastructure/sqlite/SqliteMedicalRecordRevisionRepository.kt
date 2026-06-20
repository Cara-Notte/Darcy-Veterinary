package darcy.veterinary.infrastructure.sqlite

import darcy.veterinary.domain.model.MedicalRecordRevision
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.repository.MedicalRecordRevisionRepository
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDateTime

class SqliteMedicalRecordRevisionRepository(
    private val connectionFactory: DatabaseConnectionFactory
) : MedicalRecordRevisionRepository {
    override fun save(revision: MedicalRecordRevision): MedicalRecordRevision {
        try {
            connectionFactory.openConnection().use { connection ->
                connection.prepareStatement(SAVE_SQL).use { statement ->
                    statement.setString(1, revision.id)
                    statement.setString(2, revision.recordId)
                    statement.setString(3, revision.diagnosis)
                    statement.setString(4, revision.treatment)
                    statement.setString(5, revision.notes)
                    statement.setString(6, revision.changedAt.toString())
                    statement.setString(7, revision.recordId)
                    statement.setString(8, revision.diagnosis)
                    statement.setString(9, revision.treatment)
                    statement.setString(10, revision.notes)
                    statement.setString(11, revision.changedAt.toString())
                    statement.executeUpdate()
                }
            }
        } catch (error: SQLException) {
            throw IllegalStateException("Failed to save medical record revision ${revision.id}.", error)
        }

        return revision
    }

    override fun findAll(): List<MedicalRecordRevision> =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_ALL_SQL).use { statement ->
                statement.executeQuery().use { result ->
                    result.toRevisionList()
                }
            }
        }

    override fun findByRecordId(recordId: String): List<MedicalRecordRevision> =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_BY_RECORD_ID_SQL).use { statement ->
                statement.setString(1, recordId)
                statement.executeQuery().use { result ->
                    result.toRevisionList()
                }
            }
        }

    private fun ResultSet.toRevision(): MedicalRecordRevision = MedicalRecordRevision(
        id = getString("id"),
        recordId = getString("record_id"),
        diagnosis = getString("diagnosis"),
        treatment = getString("treatment"),
        notes = getString("notes"),
        changedAt = LocalDateTime.parse(getString("changed_at"))
    )

    private fun ResultSet.toRevisionList(): List<MedicalRecordRevision> = buildList {
        while (next()) {
            add(toRevision())
        }
    }

    private companion object {
        private const val SAVE_SQL = """
            INSERT INTO medical_record_revisions (id, record_id, diagnosis, treatment, notes, changed_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                record_id = ?,
                diagnosis = ?,
                treatment = ?,
                notes = ?,
                changed_at = ?
        """

        private const val FIND_ALL_SQL = """
            SELECT id, record_id, diagnosis, treatment, notes, changed_at
            FROM medical_record_revisions
            ORDER BY rowid
        """

        private const val FIND_BY_RECORD_ID_SQL = """
            SELECT id, record_id, diagnosis, treatment, notes, changed_at
            FROM medical_record_revisions
            WHERE record_id = ?
            ORDER BY rowid
        """
    }
}
