package darcy.veterinary.repository

import darcy.veterinary.domain.model.MedicalRecord

interface MedicalRecordRepository {
    fun save(record: MedicalRecord): MedicalRecord
    fun findById(id: String): MedicalRecord?
    fun findAll(): List<MedicalRecord>
    fun findByPetId(petId: String): List<MedicalRecord>
}
