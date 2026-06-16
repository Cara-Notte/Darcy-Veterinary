package darcy.veterinary.presentation.cli

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.PatientService
import darcy.veterinary.domain.model.Appointment
import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.Pet

class AppointmentMenu(
    private val appointmentService: AppointmentService,
    private val patientService: PatientService,
    private val input: InputReader
) {
    private val selector = CliListSelector(input)

    fun show() {
        println("\nAppointment Management")
        println("1. Schedule appointment")
        println("2. Complete appointment")
        println("3. Cancel appointment")
        println("4. List appointments")
        when (input.choice("Choose menu: ", 1..4)) {
            1 -> schedule()
            2 -> complete()
            3 -> cancel()
            4 -> list()
        }
    }

    private fun schedule() {
        val pet = selector.choose(
            title = "Available pets",
            items = patientService.listPets(),
            emptyMessage = "No pets registered yet. Register a pet before scheduling an appointment.",
            prompt = "Select pet: ",
            formatter = Pet::summary
        ) ?: return

        val appointment = appointmentService.scheduleAppointment(
            petId = pet.id,
            scheduledAt = input.dateTime("Scheduled at (YYYY-MM-DDTHH:MM): "),
            reason = input.text("Reason: ")
        )
        println("Appointment scheduled: ${appointment.id}")
    }

    private fun complete() {
        val appointment = selector.choose(
            title = "Scheduled appointments",
            items = appointmentService.listAppointments().filter { it.status == AppointmentStatus.SCHEDULED },
            emptyMessage = "No scheduled appointments available to complete.",
            prompt = "Select appointment: ",
            formatter = Appointment::summary
        ) ?: return

        val completed = appointmentService.completeAppointment(appointment.id)
        println("Appointment completed: ${completed.id}")
    }

    private fun cancel() {
        val appointment = selector.choose(
            title = "Scheduled appointments",
            items = appointmentService.listAppointments().filter { it.status == AppointmentStatus.SCHEDULED },
            emptyMessage = "No scheduled appointments available to cancel.",
            prompt = "Select appointment: ",
            formatter = Appointment::summary
        ) ?: return

        val cancelled = appointmentService.cancelAppointment(appointment.id)
        println("Appointment cancelled: ${cancelled.id}")
    }

    private fun list() {
        selector.show(
            title = "Appointments",
            items = appointmentService.listAppointments(),
            emptyMessage = "No appointments scheduled yet.",
            formatter = Appointment::summary
        )
    }

    private fun Pet.summary(): String = "$id | $name | $species | Owner: $ownerId"

    private fun Appointment.summary(): String = "$id | Pet: $petId | $scheduledAt | $status | $reason"
}
