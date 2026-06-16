package darcy.veterinary.infrastructure.storage

import darcy.veterinary.domain.model.Appointment
import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.domain.model.Invoice
import darcy.veterinary.domain.model.InvoiceItem
import darcy.veterinary.domain.model.MedicalRecord
import darcy.veterinary.domain.model.Owner
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.domain.model.Pet
import darcy.veterinary.repository.AppointmentRepository
import darcy.veterinary.repository.InvoiceRepository
import darcy.veterinary.repository.MedicalRecordRepository
import darcy.veterinary.repository.OwnerRepository
import darcy.veterinary.repository.PetRepository
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
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
        invoiceRepository: InvoiceRepository
    ) {
        Files.createDirectories(dataDirectory)
        val snapshot = ClinicSnapshot(
            owners = ownerRepository.findAll().map { it.toDto() },
            pets = petRepository.findAll().map { it.toDto() },
            appointments = appointmentRepository.findAll().map { it.toDto() },
            records = medicalRecordRepository.findAll().map { it.toDto() },
            invoices = invoiceRepository.findAll().map { it.toDto() }
        )
        Files.writeString(dataDirectory.resolve(fileName), json.encodeToString(snapshot))
    }

    override fun loadAll(
        ownerRepository: OwnerRepository,
        petRepository: PetRepository,
        appointmentRepository: AppointmentRepository,
        medicalRecordRepository: MedicalRecordRepository,
        invoiceRepository: InvoiceRepository
    ) {
        val file = dataDirectory.resolve(fileName)
        if (!Files.exists(file)) return

        val snapshot = json.decodeFromString<ClinicSnapshot>(Files.readString(file))
        snapshot.owners.map(OwnerDto::toDomain).forEach(ownerRepository::save)
        snapshot.pets.map(PetDto::toDomain).forEach(petRepository::save)
        snapshot.appointments.map(AppointmentDto::toDomain).forEach(appointmentRepository::save)
        snapshot.records.map(MedicalRecordDto::toDomain).forEach(medicalRecordRepository::save)
        snapshot.invoices.map(InvoiceDto::toDomain).forEach(invoiceRepository::save)
    }

    private fun Owner.toDto() = OwnerDto(id, fullName, phoneNumber, email)
    private fun Pet.toDto() = PetDto(id, ownerId, name, species, breed, age)
    private fun Appointment.toDto() = AppointmentDto(id, petId, scheduledAt.toString(), reason, status.name)
    private fun MedicalRecord.toDto() = MedicalRecordDto(id, petId, appointmentId, diagnosis, treatment, notes, recordedAt.toString())
    private fun Invoice.toDto() = InvoiceDto(id, petId, issuedAt.toString(), paymentStatus.name, items.map { it.service.name })

    @Serializable
    private data class ClinicSnapshot(
        val owners: List<OwnerDto> = emptyList(),
        val pets: List<PetDto> = emptyList(),
        val appointments: List<AppointmentDto> = emptyList(),
        val records: List<MedicalRecordDto> = emptyList(),
        val invoices: List<InvoiceDto> = emptyList()
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
}
