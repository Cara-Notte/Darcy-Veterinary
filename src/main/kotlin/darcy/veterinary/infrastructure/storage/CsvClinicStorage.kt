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
import darcy.veterinary.repository.InvoiceStatusHistoryRepository
import darcy.veterinary.repository.MedicalRecordRepository
import darcy.veterinary.repository.MedicalRecordRevisionRepository
import darcy.veterinary.repository.OwnerRepository
import darcy.veterinary.repository.PetRepository
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime

class CsvClinicStorage(private val dataDirectory: Path = Path.of("data")) : ClinicStorage {
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
        writeLines("owners.csv", ownerRepository.findAll().map { owner ->
            row(owner.id, owner.fullName, owner.phoneNumber, owner.email.orEmpty())
        })
        writeLines("pets.csv", petRepository.findAll().map { pet ->
            row(pet.id, pet.ownerId, pet.name, pet.species, pet.breed.orEmpty(), pet.age?.toString().orEmpty())
        })
        writeLines("appointments.csv", appointmentRepository.findAll().map { appointment ->
            row(appointment.id, appointment.petId, appointment.scheduledAt.toString(), appointment.reason, appointment.status.name)
        })
        writeLines("records.csv", medicalRecordRepository.findAll().map { record ->
            row(record.id, record.petId, record.appointmentId.orEmpty(), record.diagnosis, record.treatment, record.notes, record.recordedAt.toString())
        })
        writeLines("invoices.csv", invoiceRepository.findAll().map { invoice ->
            row(invoice.id, invoice.petId, invoice.issuedAt.toString(), invoice.paymentStatus.name, invoice.items.joinToString("|") { it.service.name })
        })
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
        readRows("owners.csv").forEach { columns ->
            if (columns.size >= 3) ownerRepository.save(Owner(columns[0], columns[1], columns[2], columns.getOrNull(3)?.ifBlank { null }))
        }
        readRows("pets.csv").forEach { columns ->
            if (columns.size >= 4) petRepository.save(Pet(columns[0], columns[1], columns[2], columns[3], columns.getOrNull(4)?.ifBlank { null }, columns.getOrNull(5)?.toIntOrNull()))
        }
        readRows("appointments.csv").forEach { columns ->
            if (columns.size >= 5) appointmentRepository.save(Appointment(columns[0], columns[1], LocalDateTime.parse(columns[2]), columns[3], AppointmentStatus.valueOf(columns[4])))
        }
        readRows("records.csv").forEach { columns ->
            if (columns.size >= 7) medicalRecordRepository.save(MedicalRecord(columns[0], columns[1], columns[2].ifBlank { null }, columns[3], columns[4], columns[5], LocalDateTime.parse(columns[6])))
        }
        readRows("invoices.csv").forEach { columns ->
            if (columns.size >= 5) {
                val items = columns[4].split("|").filter { it.isNotBlank() }.mapNotNull(ClinicService::fromCode).map { InvoiceItem(it) }
                invoiceRepository.save(Invoice(columns[0], columns[1], items, LocalDateTime.parse(columns[2]), PaymentStatus.valueOf(columns[3])))
            }
        }
    }

    private fun writeLines(fileName: String, records: List<String>) {
        Files.write(dataDirectory.resolve(fileName), records)
    }

    private fun readRows(fileName: String): List<List<String>> {
        val file = dataDirectory.resolve(fileName)
        if (!Files.exists(file)) return emptyList()
        return Files.readAllLines(file).filter { it.isNotBlank() }.map { it.split(";").map(String::trim) }
    }

    private fun row(vararg values: String): String = values.joinToString(";") { sanitize(it) }

    private fun sanitize(value: String): String = value.replace(";", " ").replace(System.lineSeparator(), " ").trim()
}
