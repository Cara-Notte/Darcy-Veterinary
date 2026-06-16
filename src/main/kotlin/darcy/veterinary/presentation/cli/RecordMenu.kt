package darcy.veterinary.presentation.cli

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.PatientService
import darcy.veterinary.domain.model.Appointment
import darcy.veterinary.domain.model.MedicalRecord
import darcy.veterinary.domain.model.Pet

class MedicalRecordMenu(
    private val medicalRecordService: MedicalRecordService,
    private val patientService: PatientService,
    private val appointmentService: AppointmentService,
    private val input: InputReader
) {
    private val selector = CliListSelector(input)

    fun show() {
        println("\nMedical Records")
        println("1. Create record")
        println("2. Edit record")
        println("3. List records")
        when (input.choice("Choose menu: ", 1..3)) {
            1 -> create()
            2 -> edit()
            3 -> list()
        }
    }

    private fun create() {
        val pet = selector.choose(
            title = "Available pets",
            items = patientService.listPets(),
            emptyMessage = "No pets registered yet. Register a pet before creating a medical record.",
            prompt = "Select pet: ",
            formatter = { it.summary() }
        ) ?: return

        val appointment = selector.chooseOptional(
            title = "Appointments for ${pet.name}",
            items = appointmentService.listAppointmentsByPet(pet.id),
            emptyMessage = "No appointments found for this pet. The record will not be linked to an appointment.",
            prompt = "Select appointment or 0: ",
            formatter = { it.summary() }
        )

        val record = medicalRecordService.createRecord(
            petId = pet.id,
            appointmentId = appointment?.id,
            diagnosis = input.text("Diagnosis: "),
            treatment = input.text("Treatment: "),
            notes = input.optionalText("Notes: ").orEmpty()
        )
        println("Medical record created: ${record.id}")
    }

    private fun edit() {
        val record = selector.choose(
            title = "Medical records",
            items = medicalRecordService.listRecords(),
            emptyMessage = "No medical records created yet.",
            prompt = "Select record to edit: ",
            formatter = { it.summary() }
        ) ?: return

        val updated = medicalRecordService.updateRecord(
            id = record.id,
            diagnosis = input.optionalText("Diagnosis [${record.diagnosis}]: ") ?: record.diagnosis,
            treatment = input.optionalText("Treatment [${record.treatment}]: ") ?: record.treatment,
            notes = input.optionalText("Notes [leave blank to keep current]: ") ?: record.notes
        )
        println("Medical record updated: ${updated.id}")
    }

    private fun list() {
        selector.show(
            title = "Medical records",
            items = medicalRecordService.listRecords(),
            emptyMessage = "No medical records created yet.",
            formatter = { it.summary() }
        )
    }

    private fun Pet.summary(): String = "$id | $name | $species | Owner: $ownerId"

    private fun Appointment.summary(): String = "$id | Pet: $petId | $scheduledAt | $status | $reason"

    private fun MedicalRecord.summary(): String = "$id | Pet: $petId | $diagnosis | $recordedAt"
}
