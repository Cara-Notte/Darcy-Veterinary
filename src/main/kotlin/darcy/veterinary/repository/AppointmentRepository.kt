package darcy.veterinary.repository

import darcy.veterinary.domain.model.Appointment

interface AppointmentRepository {
    fun save(appointment: Appointment): Appointment
    fun findById(id: String): Appointment?
    fun findAll(): List<Appointment>
    fun findByPetId(petId: String): List<Appointment>
}
