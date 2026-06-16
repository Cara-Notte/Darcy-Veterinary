package darcy.veterinary.infrastructure.memory

import darcy.veterinary.domain.model.MedicalRecord
import darcy.veterinary.repository.MedicalRecordRepository

class InMemoryMedicalRecordRepository : MedicalRecordRepository {
    private val records = linkedMapOf<String, MedicalRecord>()

    override fun save(record: MedicalRecord): MedicalRecord {
        records[record.id] = record
        return record
    }

    override fun findById(id: String): MedicalRecord? = records[id]

    override fun findAll(): List<MedicalRecord> = records.values.toList()

    override fun findByPetId(petId: String): List<MedicalRecord> = records.values.filter { it.petId == petId }
}
