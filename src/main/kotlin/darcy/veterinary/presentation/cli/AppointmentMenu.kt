package darcy.veterinary.presentation.cli

import darcy.veterinary.application.AppointmentService

class AppointmentMenu(
    private val appointmentService: AppointmentService,
    private val input: InputReader
) {
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
        val appointment = appointmentService.scheduleAppointment(
            petId = input.text("Pet ID: "),
            scheduledAt = input.dateTime("Scheduled at (YYYY-MM-DDTHH:MM): "),
            reason = input.text("Reason: ")
        )
        println("Appointment scheduled: ${appointment.id}")
    }

    private fun complete() {
        val appointment = appointmentService.completeAppointment(input.text("Appointment ID: "))
        println("Appointment completed: ${appointment.id}")
    }

    private fun cancel() {
        val appointment = appointmentService.cancelAppointment(input.text("Appointment ID: "))
        println("Appointment cancelled: ${appointment.id}")
    }

    private fun list() {
        appointmentService.listAppointments().forEach { appointment ->
            println("${appointment.id} | ${appointment.petId} | ${appointment.scheduledAt} | ${appointment.status} | ${appointment.reason}")
        }
    }
}
