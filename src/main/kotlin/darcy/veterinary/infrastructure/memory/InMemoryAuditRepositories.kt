package darcy.veterinary.infrastructure.memory

import darcy.veterinary.domain.model.InvoiceStatusHistory
import darcy.veterinary.domain.model.MedicalRecordRevision
import darcy.veterinary.repository.InvoiceStatusHistoryRepository
import darcy.veterinary.repository.MedicalRecordRevisionRepository

class InMemoryMedicalRecordRevisionRepository : MedicalRecordRevisionRepository {
    private val revisions = linkedMapOf<String, MedicalRecordRevision>()

    override fun save(revision: MedicalRecordRevision): MedicalRecordRevision {
        revisions[revision.id] = revision
        return revision
    }

    override fun findAll(): List<MedicalRecordRevision> = revisions.values.toList()

    override fun findByRecordId(recordId: String): List<MedicalRecordRevision> =
        revisions.values.filter { it.recordId == recordId }
}

class InMemoryInvoiceStatusHistoryRepository : InvoiceStatusHistoryRepository {
    private val entries = linkedMapOf<String, InvoiceStatusHistory>()

    override fun save(history: InvoiceStatusHistory): InvoiceStatusHistory {
        entries[history.id] = history
        return history
    }

    override fun findAll(): List<InvoiceStatusHistory> = entries.values.toList()

    override fun findByInvoiceId(invoiceId: String): List<InvoiceStatusHistory> =
        entries.values.filter { it.invoiceId == invoiceId }
}
