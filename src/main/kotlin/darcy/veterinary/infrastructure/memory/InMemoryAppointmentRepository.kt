package darcy.veterinary.infrastructure.memory

import darcy.veterinary.domain.model.Appointment
import darcy.veterinary.repository.AppointmentRepository

class InMemoryAppointmentRepository : AppointmentRepository {
    private val appointments = linkedMapOf<String, Appointment>()

    override fun save(appointment: Appointment): Appointment {
        appointments[appointment.id] = appointment
        return appointment
    }

    override fun findById(id: String): Appointment? = appointments[id]

    override fun findAll(): List<Appointment> = appointments.values.toList()

    override fun findByPetId(petId: String): List<Appointment> = appointments.values.filter { it.petId == petId }
}
