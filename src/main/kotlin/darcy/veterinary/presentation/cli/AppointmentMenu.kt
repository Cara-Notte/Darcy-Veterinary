package darcy.veterinary.presentation.cli

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.PatientService
import darcy.veterinary.domain.model.Appointment
import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.Pet
import darcy.veterinary.domain.model.VisitType

class AppointmentMenu(
    private val appointmentService: AppointmentService,
    private val patientService: PatientService,
    private val input: InputReader
) {
    private val selector = CliListSelector(input)

    fun show() {
        println("\nAppointment Management")
        println("0. Back")
        println("1. Schedule appointment")
        println("2. Reschedule appointment")
        println("3. Complete appointment")
        println("4. Cancel appointment")
        println("5. List appointments")
        when (input.choice("Choose menu: ", 0..5)) {
            0 -> return
            1 -> schedule()
            2 -> reschedule()
            3 -> complete()
            4 -> cancel()
            5 -> list()
        }
    }

    private fun schedule() {
        val pet = selector.choose(
            title = "Available pets",
            items = patientService.listPets().sortedBy { it.name.lowercase() },
            emptyMessage = "No pets registered yet. Register a pet before scheduling an appointment.",
            prompt = "Select pet: ",
            formatter = { it.summary() }
        ) ?: return

        val appointment = appointmentService.scheduleAppointment(
            petId = pet.id,
            scheduledAt = input.dateTime("Scheduled at (YYYY-MM-DDTHH:MM): "),
            reason = input.text("Reason: "),
            visitType = readVisitType("Visit type ${VisitType.entries.joinToString()} (optional): ") ?: VisitType.GENERAL,
            veterinarianName = input.optionalText("Veterinarian name (optional): ")
        )
        println("Appointment scheduled: ${appointment.id}")
    }

    private fun reschedule() {
        val appointment = selector.choose(
            title = "Scheduled appointments",
            items = scheduledAppointments(),
            emptyMessage = "No scheduled appointments available to reschedule.",
            prompt = "Select appointment: ",
            formatter = { it.summary() }
        ) ?: return

        val updated = appointmentService.rescheduleAppointment(
            id = appointment.id,
            scheduledAt = input.dateTime("New scheduled time (YYYY-MM-DDTHH:MM): "),
            reason = input.optionalText("Reason [${appointment.reason}]: ") ?: appointment.reason,
            visitType = readVisitType("Visit type [${appointment.visitType}]: ") ?: appointment.visitType,
            veterinarianName = input.optionalText("Veterinarian [${appointment.veterinarianName.orEmpty()}]: ") ?: appointment.veterinarianName
        )
        println("Appointment rescheduled: ${updated.id}")
    }

    private fun complete() {
        val appointment = selector.choose(
            title = "Scheduled appointments",
            items = scheduledAppointments(),
            emptyMessage = "No scheduled appointments available to complete.",
            prompt = "Select appointment: ",
            formatter = { it.summary() }
        ) ?: return

        if (!input.confirm("Mark appointment ${appointment.id} as completed")) {
            println("Completion cancelled.")
            return
        }

        val completed = appointmentService.completeAppointment(appointment.id)
        println("Appointment completed: ${completed.id}")
    }

    private fun cancel() {
        val appointment = selector.choose(
            title = "Scheduled appointments",
            items = scheduledAppointments(),
            emptyMessage = "No scheduled appointments available to cancel.",
            prompt = "Select appointment: ",
            formatter = { it.summary() }
        ) ?: return

        if (!input.confirm("Cancel appointment ${appointment.id}")) {
            println("Cancellation aborted.")
            return
        }

        val cancelled = appointmentService.cancelAppointment(appointment.id)
        println("Appointment cancelled: ${cancelled.id}")
    }

    private fun list() {
        selector.show(
            title = "Appointments",
            items = appointmentService.listAppointments().sortedBy { it.scheduledAt },
            emptyMessage = "No appointments scheduled yet.",
            formatter = { it.summary() }
        )
    }

    private fun scheduledAppointments(): List<Appointment> =
        appointmentService.listAppointments()
            .filter { it.status == AppointmentStatus.SCHEDULED }
            .sortedBy { it.scheduledAt }

    private fun readVisitType(prompt: String): VisitType? {
        val value = input.optionalText(prompt) ?: return null
        return runCatching { VisitType.valueOf(value.trim().uppercase()) }.getOrElse {
            println("Unknown visit type. Using current/default value.")
            null
        }
    }

    private fun Pet.summary(): String = "$id | $name | $species | Owner: $ownerId"

    private fun Appointment.summary(): String = "$id | Pet: $petId | $scheduledAt | $status | $visitType | Vet: ${veterinarianName.orEmpty()} | $reason"
}
