package darcy.veterinary.application

import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.domain.model.PetSex
import darcy.veterinary.domain.model.VisitType
import java.time.LocalDate
import java.time.LocalDateTime

class ClinicWorkspaceFacade(
    private val ownerService: OwnerService,
    private val patientService: PatientService,
    private val appointmentService: AppointmentService,
    private val recordService: MedicalRecordService,
    private val billingService: BillingService
) {
    fun search(keyword: String): ClinicWorkspaceSearchResult {
        val query = keyword.trim()
        if (query.isBlank()) return ClinicWorkspaceSearchResult.empty(query)

        val owners = ownerService.searchOwners(query)
            .sortedBy { it.fullName.lowercase() }
            .map { owner ->
                OwnerLookupRow(
                    id = owner.id,
                    fullName = owner.fullName,
                    phoneNumber = owner.phoneNumber,
                    email = owner.email,
                    patientCount = patientService.listPetsByOwner(owner.id).size
                )
            }

        val patients = patientService.searchPets(query)
            .sortedWith(compareBy({ it.name.lowercase() }, { it.species.lowercase() }))
            .map { pet ->
                val owner = ownerService.getOwner(pet.ownerId)
                PatientLookupRow(
                    id = pet.id,
                    name = pet.name,
                    species = pet.species,
                    breed = pet.breed,
                    ownerId = owner.id,
                    ownerName = owner.fullName,
                    hasAlerts = pet.allergies.isNotEmpty() || pet.medicalConditions.isNotEmpty()
                )
            }

        return ClinicWorkspaceSearchResult(query, owners, patients)
    }

    fun patientChart(petId: String): PatientChartViewData {
        val pet = patientService.getPet(petId)
        val owner = ownerService.getOwner(pet.ownerId)

        return PatientChartViewData(
            patient = PatientChartHeader(
                id = pet.id,
                name = pet.name,
                species = pet.species,
                breed = pet.breed,
                age = pet.age,
                sex = pet.sex,
                dateOfBirth = pet.dateOfBirth,
                weightKg = pet.weightKg,
                allergies = pet.allergies,
                medicalConditions = pet.medicalConditions
            ),
            owner = OwnerChartHeader(
                id = owner.id,
                fullName = owner.fullName,
                phoneNumber = owner.phoneNumber,
                email = owner.email
            ),
            appointments = appointmentService.listAppointmentsByPet(pet.id)
                .sortedByDescending { it.scheduledAt }
                .map {
                    AppointmentTimelineRow(
                        id = it.id,
                        scheduledAt = it.scheduledAt,
                        reason = it.reason,
                        status = it.status,
                        visitType = it.visitType,
                        veterinarianName = it.veterinarianName
                    )
                },
            records = recordService.listRecordsByPet(pet.id)
                .sortedByDescending { it.recordedAt }
                .map {
                    RecordTimelineRow(
                        id = it.id,
                        appointmentId = it.appointmentId,
                        diagnosis = it.diagnosis,
                        treatment = it.treatment,
                        notes = it.notes,
                        recordedAt = it.recordedAt,
                        veterinarianName = it.veterinarianName
                    )
                },
            invoices = billingService.listInvoicesByPet(pet.id)
                .sortedByDescending { it.issuedAt }
                .map {
                    InvoiceTimelineRow(
                        id = it.id,
                        issuedAt = it.issuedAt,
                        paymentStatus = it.paymentStatus,
                        total = it.total()
                    )
                }
        )
    }
}

data class ClinicWorkspaceSearchResult(
    val query: String,
    val owners: List<OwnerLookupRow>,
    val patients: List<PatientLookupRow>
) {
    val hasResults: Boolean = owners.isNotEmpty() || patients.isNotEmpty()

    companion object {
        fun empty(query: String = "") = ClinicWorkspaceSearchResult(query, emptyList(), emptyList())
    }
}

data class OwnerLookupRow(
    val id: String,
    val fullName: String,
    val phoneNumber: String,
    val email: String?,
    val patientCount: Int
)

data class PatientLookupRow(
    val id: String,
    val name: String,
    val species: String,
    val breed: String?,
    val ownerId: String,
    val ownerName: String,
    val hasAlerts: Boolean
)

data class PatientChartViewData(
    val patient: PatientChartHeader,
    val owner: OwnerChartHeader,
    val appointments: List<AppointmentTimelineRow>,
    val records: List<RecordTimelineRow>,
    val invoices: List<InvoiceTimelineRow>
)

data class PatientChartHeader(
    val id: String,
    val name: String,
    val species: String,
    val breed: String?,
    val age: Int?,
    val sex: PetSex?,
    val dateOfBirth: LocalDate?,
    val weightKg: Double?,
    val allergies: List<String>,
    val medicalConditions: List<String>
)

data class OwnerChartHeader(
    val id: String,
    val fullName: String,
    val phoneNumber: String,
    val email: String?
)

data class AppointmentTimelineRow(
    val id: String,
    val scheduledAt: LocalDateTime,
    val reason: String,
    val status: AppointmentStatus,
    val visitType: VisitType,
    val veterinarianName: String?
)

data class RecordTimelineRow(
    val id: String,
    val appointmentId: String?,
    val diagnosis: String,
    val treatment: String,
    val notes: String,
    val recordedAt: LocalDateTime,
    val veterinarianName: String?
)

data class InvoiceTimelineRow(
    val id: String,
    val issuedAt: LocalDateTime,
    val paymentStatus: PaymentStatus,
    val total: Double
)
