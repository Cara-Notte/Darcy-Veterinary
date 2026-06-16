package darcy.veterinary.presentation.cli

import darcy.veterinary.application.MedicalRecordService

class MedicalRecordMenu(
    private val medicalRecordService: MedicalRecordService,
    private val input: InputReader
) {
    fun show() {
        println("\nMedical Records")
        println("1. Create record")
        println("2. List records")
        when (input.choice("Choose menu: ", 1..2)) {
            1 -> create()
            2 -> list()
        }
    }

    private fun create() {
        val record = medicalRecordService.createRecord(
            petId = input.text("Pet ID: "),
            appointmentId = input.optionalText("Appointment ID (optional): "),
            diagnosis = input.text("Diagnosis: "),
            treatment = input.text("Treatment: "),
            notes = input.optionalText("Notes: ").orEmpty()
        )
        println("Medical record created: ${record.id}")
    }

    private fun list() {
        medicalRecordService.listRecords().forEach { record ->
            println("${record.id} | Pet: ${record.petId} | ${record.diagnosis} | ${record.recordedAt}")
        }
    }
}
