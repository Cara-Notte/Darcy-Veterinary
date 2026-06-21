package darcy.veterinary.infrastructure.storage

import darcy.veterinary.repository.AppointmentRepository
import darcy.veterinary.repository.InvoiceRepository
import darcy.veterinary.repository.InvoiceStatusHistoryRepository
import darcy.veterinary.repository.MedicalRecordRepository
import darcy.veterinary.repository.MedicalRecordRevisionRepository
import darcy.veterinary.repository.OwnerRepository
import darcy.veterinary.repository.PetRepository

object NoOpClinicStorage : ClinicStorage {
    override fun saveAll(
        ownerRepository: OwnerRepository,
        petRepository: PetRepository,
        appointmentRepository: AppointmentRepository,
        medicalRecordRepository: MedicalRecordRepository,
        invoiceRepository: InvoiceRepository,
        revisionRepository: MedicalRecordRevisionRepository?,
        invoiceHistoryRepository: InvoiceStatusHistoryRepository?
    ) = Unit

    override fun loadAll(
        ownerRepository: OwnerRepository,
        petRepository: PetRepository,
        appointmentRepository: AppointmentRepository,
        medicalRecordRepository: MedicalRecordRepository,
        invoiceRepository: InvoiceRepository,
        revisionRepository: MedicalRecordRevisionRepository?,
        invoiceHistoryRepository: InvoiceStatusHistoryRepository?
    ) = Unit
}
