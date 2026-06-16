package darcy.veterinary.application

import darcy.veterinary.domain.exception.EntityNotFoundException
import darcy.veterinary.domain.exception.InvalidClinicOperationException
import darcy.veterinary.domain.model.Appointment
import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.repository.AppointmentRepository
import darcy.veterinary.repository.PetRepository
import java.time.LocalDateTime

class AppointmentService(
    private val appointmentRepository: AppointmentRepository,
    private val petRepository: PetRepository,
    private val idGenerator: IdGenerator = UuidIdGenerator
) {
    fun scheduleAppointment(petId: String, scheduledAt: LocalDateTime, reason: String): Appointment {
        require(reason.isNotBlank()) { "Appointment reason cannot be blank." }
        petRepository.findById(petId)
            ?: throw EntityNotFoundException("Pet with ID $petId was not found.")

        return appointmentRepository.save(
            Appointment(
                id = idGenerator.nextId("APT"),
                petId = petId,
                scheduledAt = scheduledAt,
                reason = reason.trim()
            )
        )
    }

    fun rescheduleAppointment(id: String, scheduledAt: LocalDateTime, reason: String): Appointment {
        require(reason.isNotBlank()) { "Appointment reason cannot be blank." }
        val appointment = getAppointment(id)
        if (appointment.status != AppointmentStatus.SCHEDULED) {
            throw InvalidClinicOperationException("Only scheduled appointments can be rescheduled.")
        }

        return appointmentRepository.save(
            appointment.copy(
                scheduledAt = scheduledAt,
                reason = reason.trim()
            )
        )
    }

    fun completeAppointment(id: String): Appointment = changeStatus(id, AppointmentStatus.COMPLETED)

    fun cancelAppointment(id: String): Appointment = changeStatus(id, AppointmentStatus.CANCELLED)

    fun getAppointment(id: String): Appointment = appointmentRepository.findById(id)
        ?: throw EntityNotFoundException("Appointment with ID $id was not found.")

    fun listAppointments(): List<Appointment> = appointmentRepository.findAll()

    fun listAppointmentsByPet(petId: String): List<Appointment> = appointmentRepository.findByPetId(petId)

    private fun changeStatus(id: String, status: AppointmentStatus): Appointment {
        val appointment = getAppointment(id)
        if (appointment.status != AppointmentStatus.SCHEDULED) {
            throw InvalidClinicOperationException("Only scheduled appointments can be updated.")
        }

        return appointmentRepository.save(appointment.copy(status = status))
    }
}
