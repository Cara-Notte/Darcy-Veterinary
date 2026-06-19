package darcy.veterinary.application

import darcy.veterinary.domain.exception.EntityNotFoundException
import darcy.veterinary.domain.model.MedicalRecord
import darcy.veterinary.domain.model.MedicalRecordRevision
import darcy.veterinary.repository.AppointmentRepository
import darcy.veterinary.repository.MedicalRecordRepository
import darcy.veterinary.repository.MedicalRecordRevisionRepository
import darcy.veterinary.repository.PetRepository
import java.time.LocalDateTime

class MedicalRecordService(
    private val medicalRecordRepository: MedicalRecordRepository,
    private val petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
    private val idGenerator: IdGenerator = UuidIdGenerator,
    private val revisionRepository: MedicalRecordRevisionRepository? = null
) {
    fun createRecord(
        petId: String,
        diagnosis: String,
        treatment: String,
        notes: String,
        appointmentId: String? = null,
        recordedAt: LocalDateTime = LocalDateTime.now()
    ): MedicalRecord {
        validateRecord(diagnosis, treatment)
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

    fun updateRecord(id: String, diagnosis: String, treatment: String, notes: String): MedicalRecord {
        validateRecord(diagnosis, treatment)
        val existing = getRecord(id)

        revisionRepository?.save(
            MedicalRecordRevision(
                id = idGenerator.nextId("REV"),
                recordId = existing.id,
                diagnosis = existing.diagnosis,
                treatment = existing.treatment,
                notes = existing.notes,
                changedAt = LocalDateTime.now()
            )
        )

        return medicalRecordRepository.save(
            existing.copy(
                diagnosis = diagnosis.trim(),
                treatment = treatment.trim(),
                notes = notes.trim()
            )
        )
    }

    fun getRecord(id: String): MedicalRecord = medicalRecordRepository.findById(id)
        ?: throw EntityNotFoundException("Medical record with ID $id was not found.")

    fun listRecords(): List<MedicalRecord> = medicalRecordRepository.findAll()

    fun listRecordsByPet(petId: String): List<MedicalRecord> = medicalRecordRepository.findByPetId(petId)

    fun listRevisions(recordId: String): List<MedicalRecordRevision> =
        revisionRepository?.findByRecordId(recordId) ?: emptyList()

    private fun validateRecord(diagnosis: String, treatment: String) {
        require(diagnosis.isNotBlank()) { "Diagnosis cannot be blank." }
        require(treatment.isNotBlank()) { "Treatment cannot be blank." }
    }
}
