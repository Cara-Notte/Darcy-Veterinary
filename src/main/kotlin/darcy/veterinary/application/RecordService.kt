package darcy.veterinary.application

import darcy.veterinary.domain.exception.EntityNotFoundException
import darcy.veterinary.domain.model.MedicalRecord
import darcy.veterinary.repository.AppointmentRepository
import darcy.veterinary.repository.MedicalRecordRepository
import darcy.veterinary.repository.PetRepository
import java.time.LocalDateTime

class MedicalRecordService(
    private val medicalRecordRepository: MedicalRecordRepository,
    private val petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
    private val idGenerator: IdGenerator = UuidIdGenerator
) {
    fun createRecord(
        petId: String,
        diagnosis: String,
        treatment: String,
        notes: String,
        appointmentId: String? = null,
        recordedAt: LocalDateTime = LocalDateTime.now()
    ): MedicalRecord {
        require(diagnosis.isNotBlank()) { "Diagnosis cannot be blank." }
        require(treatment.isNotBlank()) { "Treatment cannot be blank." }

        petRepository.findById(petId)
            ?: throw EntityNotFoundException("Pet with ID $petId was not found.")

        if (appointmentId != null && appointmentRepository.findById(appointmentId) == null) {
            throw EntityNotFoundException("Appointment with ID $appointmentId was not found.")
        }

        return medicalRecordRepository.save(
            MedicalRecord(
                id = idGenerator.nextId("REC"),
                petId = petId,
                appointmentId = appointmentId,
                diagnosis = diagnosis.trim(),
                treatment = treatment.trim(),
                notes = notes.trim(),
                recordedAt = recordedAt
            )
        )
    }

    fun listRecords(): List<MedicalRecord> = medicalRecordRepository.findAll()

    fun listRecordsByPet(petId: String): List<MedicalRecord> = medicalRecordRepository.findByPetId(petId)
}
