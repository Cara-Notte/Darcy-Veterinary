package darcy.veterinary.infrastructure.sqlite

import darcy.veterinary.domain.model.Appointment
import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.VisitType
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.repository.AppointmentRepository
import java.sql.ResultSet
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDateTime

class SqliteAppointmentRepository(
    private val connectionFactory: DatabaseConnectionFactory
) : AppointmentRepository {
    override fun save(appointment: Appointment): Appointment {
        val now = Instant.now().toString()

        try {
            connectionFactory.openConnection().use { connection ->
                connection.prepareStatement(SAVE_SQL).use { statement ->
                    statement.setString(1, appointment.id)
                    statement.setString(2, appointment.petId)
                    statement.setString(3, appointment.scheduledAt.toString())
                    statement.setString(4, appointment.reason)
                    statement.setString(5, appointment.status.name)
                    statement.setString(6, appointment.visitType.name)
                    statement.setString(7, appointment.veterinarianName)
                    statement.setString(8, now)
                    statement.setString(9, now)
                    statement.setString(10, appointment.petId)
                    statement.setString(11, appointment.scheduledAt.toString())
                    statement.setString(12, appointment.reason)
                    statement.setString(13, appointment.status.name)
                    statement.setString(14, appointment.visitType.name)
                    statement.setString(15, appointment.veterinarianName)
                    statement.setString(16, now)
                    statement.executeUpdate()
                }
            }
        } catch (error: SQLException) {
            throw IllegalStateException("Failed to save appointment ${appointment.id}.", error)
        }

        return appointment
    }

    override fun findById(id: String): Appointment? =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_BY_ID_SQL).use { statement ->
                statement.setString(1, id)
                statement.executeQuery().use { result ->
                    if (result.next()) result.toAppointment() else null
                }
            }
        }

    override fun findAll(): List<Appointment> =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_ALL_SQL).use { statement ->
                statement.executeQuery().use { result ->
                    result.toAppointmentList()
                }
            }
        }

    override fun findByPetId(petId: String): List<Appointment> =
        connectionFactory.openConnection().use { connection ->
            connection.prepareStatement(FIND_BY_PET_ID_SQL).use { statement ->
                statement.setString(1, petId)
                statement.executeQuery().use { result ->
                    result.toAppointmentList()
                }
            }
        }

    private fun ResultSet.toAppointment(): Appointment = Appointment(
        id = getString("id"),
        petId = getString("pet_id"),
        scheduledAt = LocalDateTime.parse(getString("scheduled_at")),
        reason = getString("reason"),
        status = AppointmentStatus.valueOf(getString("status")),
        visitType = VisitType.valueOf(getString("visit_type")),
        veterinarianName = getString("veterinarian_name")
    )

    private fun ResultSet.toAppointmentList(): List<Appointment> = buildList {
        while (next()) {
            add(toAppointment())
        }
    }

    private companion object {
        private const val SAVE_SQL = """
            INSERT INTO appointments (id, pet_id, scheduled_at, reason, status, visit_type, veterinarian_name, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                pet_id = ?,
                scheduled_at = ?,
                reason = ?,
                status = ?,
                visit_type = ?,
                veterinarian_name = ?,
                updated_at = ?
        """

        private const val FIND_BY_ID_SQL = """
            SELECT id, pet_id, scheduled_at, reason, status, visit_type, veterinarian_name
            FROM appointments
            WHERE id = ?
        """

        private const val FIND_ALL_SQL = """
            SELECT id, pet_id, scheduled_at, reason, status, visit_type, veterinarian_name
            FROM appointments
            ORDER BY rowid
        """

        private const val FIND_BY_PET_ID_SQL = """
            SELECT id, pet_id, scheduled_at, reason, status, visit_type, veterinarian_name
            FROM appointments
            WHERE pet_id = ?
            ORDER BY rowid
        """
    }
}
