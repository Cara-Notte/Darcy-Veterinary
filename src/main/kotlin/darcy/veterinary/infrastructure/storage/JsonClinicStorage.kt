package darcy.veterinary.infrastructure.storage

import darcy.veterinary.domain.model.Appointment
import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.Invoice
import darcy.veterinary.domain.model.InvoiceItem
import darcy.veterinary.domain.model.InvoiceStatusHistory
import darcy.veterinary.domain.model.MedicalRecord
import darcy.veterinary.domain.model.MedicalRecordRevision
import darcy.veterinary.domain.model.Owner
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.domain.model.Pet
import darcy.veterinary.repository.AppointmentRepository
import darcy.veterinary.repository.InvoiceRepository
import darcy.veterinary.repository.InvoiceStatusHistoryRepository
import darcy.veterinary.repository.MedicalRecordRepository
import darcy.veterinary.repository.MedicalRecordRevisionRepository
import darcy.veterinary.repository.OwnerRepository
import darcy.veterinary.repository.PetRepository
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JsonClinicStorage(
    private val dataDirectory: Path = Path.of("data"),
    private val fileName: String = "clinic-data.json"
) : ClinicStorage {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override fun saveAll(
        ownerRepository: OwnerRepository,
        petRepository: PetRepository,
        appointmentRepository: AppointmentRepository,
        medicalRecordRepository: MedicalRecordRepository,
        invoiceRepository: InvoiceRepository,
        revisionRepository: MedicalRecordRevisionRepository?,
        invoiceHistoryRepository: InvoiceStatusHistoryRepository?
    ) {
        Files.createDirectories(dataDirectory)
        val snapshot = ClinicSnapshot(
            owners = ownerRepository.findAll().map { it.toDto() },
            pets = petRepository.findAll().map { it.toDto() },
            appointments = appointmentRepository.findAll().map { it.toDto() },
            records = medicalRecordRepository.findAll().map { it.toDto() },
            invoices = invoiceRepository.findAll().map { it.toDto() },
            recordRevisions = revisionRepository?.findAll()?.map { it.toDto() }.orEmpty(),
            invoiceStatusHistory = invoiceHistoryRepository?.findAll()?.map { it.toDto() }.orEmpty()
        )
        Files.writeString(dataDirectory.resolve(fileName), json.encodeToString(snapshot))
    }

    override fun loadAll(
        ownerRepository: OwnerRepository,
        petRepository: PetRepository,
        appointmentRepository: AppointmentRepository,
        medicalRecordRepository: MedicalRecordRepository,
        invoiceRepository: InvoiceRepository,
        revisionRepository: MedicalRecordRevisionRepository?,
        invoiceHistoryRepository: InvoiceStatusHistoryRepository?
    ) {
        val file = dataDirectory.resolve(fileName)
        if (!Files.exists(file)) return

        val snapshot = json.decodeFromString<ClinicSnapshot>(Files.readString(file))
        snapshot.owners.map(OwnerDto::toDomain).forEach(ownerRepository::save)
        snapshot.pets.map(PetDto::toDomain).forEach(petRepository::save)
        snapshot.appointments.map(AppointmentDto::toDomain).forEach(appointmentRepository::save)
        snapshot.records.map(MedicalRecordDto::toDomain).forEach(medicalRecordRepository::save)
        snapshot.invoices.map(InvoiceDto::toDomain).forEach(invoiceRepository::save)
        snapshot.recordRevisions.map(MedicalRecordRevisionDto::toDomain).forEach { revisionRepository?.save(it) }
        snapshot.invoiceStatusHistory.map(InvoiceStatusHistoryDto::toDomain).forEach { invoiceHistoryRepository?.save(it) }
    }

    private fun Owner.toDto() = OwnerDto(id, fullName, phoneNumber, email)
    private fun Pet.toDto() = PetDto(id, ownerId, name, species, breed, age)
    private fun Appointment.toDto() = AppointmentDto(id, petId, scheduledAt.toString(), reason, status.name)
    private fun MedicalRecord.toDto() = MedicalRecordDto(id, petId, appointmentId, diagnosis, treatment, notes, recordedAt.toString())
    private fun Invoice.toDto() = InvoiceDto(id, petId, issuedAt.toString(), paymentStatus.name, items.map { it.service.name })
    private fun MedicalRecordRevision.toDto() = MedicalRecordRevisionDto(id, recordId, diagnosis, treatment, notes, changedAt.toString())
    private fun InvoiceStatusHistory.toDto() = InvoiceStatusHistoryDto(id, invoiceId, fromStatus?.name, toStatus.name, changedAt.toString(), reason)

    @Serializable
    private data class ClinicSnapshot(
        val owners: List<OwnerDto> = emptyList(),
        val pets: List<PetDto> = emptyList(),
        val appointments: List<AppointmentDto> = emptyList(),
        val records: List<MedicalRecordDto> = emptyList(),
        val invoices: List<InvoiceDto> = emptyList(),
        val recordRevisions: List<MedicalRecordRevisionDto> = emptyList(),
        val invoiceStatusHistory: List<InvoiceStatusHistoryDto> = emptyList()
    )

    @Serializable
    private data class OwnerDto(
        val id: String,
        val fullName: String,
        val phoneNumber: String,
        val email: String? = null
    ) {
        fun toDomain() = Owner(id, fullName, phoneNumber, email)
    }

    @Serializable
    private data class PetDto(
        val id: String,
        val ownerId: String,
        val name: String,
        val species: String,
        val breed: String? = null,
        val age: Int? = null
    ) {
        fun toDomain() = Pet(id, ownerId, name, species, breed, age)
    }

    @Serializable
    private data class AppointmentDto(
        val id: String,
        val petId: String,
        val scheduledAt: String,
        val reason: String,
        val status: String
    ) {
        fun toDomain() = Appointment(id, petId, LocalDateTime.parse(scheduledAt), reason, AppointmentStatus.valueOf(status))
    }

    @Serializable
    private data class MedicalRecordDto(
        val id: String,
        val petId: String,
        val appointmentId: String? = null,
        val diagnosis: String,
        val treatment: String,
        val notes: String,
        val recordedAt: String
    ) {
        fun toDomain() = MedicalRecord(id, petId, appointmentId, diagnosis, treatment, notes, LocalDateTime.parse(recordedAt))
    }

    @Serializable
    private data class InvoiceDto(
        val id: String,
        val petId: String,
        val issuedAt: String,
        val paymentStatus: String,
        val services: List<String>
    ) {
        fun toDomain() = Invoice(
            id = id,
            petId = petId,
            items = services.mapNotNull(ClinicService::fromCode).map { InvoiceItem(it) },
            issuedAt = LocalDateTime.parse(issuedAt),
            paymentStatus = PaymentStatus.valueOf(paymentStatus)
        )
    }

    @Serializable
    private data class MedicalRecordRevisionDto(
        val id: String,
        val recordId: String,
        val diagnosis: String,
        val treatment: String,
        val notes: String,
        val changedAt: String
    ) {
        fun toDomain() = MedicalRecordRevision(id, recordId, diagnosis, treatment, notes, LocalDateTime.parse(changedAt))
    }

    @Serializable
    private data class InvoiceStatusHistoryDto(
        val id: String,
        val invoiceId: String,
        val fromStatus: String? = null,
        val toStatus: String,
        val changedAt: String,
        val reason: String
    ) {
        fun toDomain() = InvoiceStatusHistory(
            id = id,
            invoiceId = invoiceId,
            fromStatus = fromStatus?.let(PaymentStatus::valueOf),
            toStatus = PaymentStatus.valueOf(toStatus),
            changedAt = LocalDateTime.parse(changedAt),
            reason = reason
        )
    }
}
