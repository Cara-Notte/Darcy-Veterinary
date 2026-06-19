package darcy.veterinary.repository

import darcy.veterinary.domain.model.MedicalRecordRevision

interface MedicalRecordRevisionRepository {
    fun save(revision: MedicalRecordRevision): MedicalRecordRevision
    fun findAll(): List<MedicalRecordRevision>
    fun findByRecordId(recordId: String): List<MedicalRecordRevision>
}
