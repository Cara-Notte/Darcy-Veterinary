package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.MedicalRecordService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.model.VisitType
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRepository
import darcy.veterinary.infrastructure.memory.InMemoryMedicalRecordRevisionRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class MedicalRecordFormViewModelTest {
    private fun fixture(): Fixture {
        val ids = SequenceIdGenerator()
        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val appointmentRepository = InMemoryAppointmentRepository()
        val recordRepository = InMemoryMedicalRecordRepository()
        val revisionRepository = InMemoryMedicalRecordRevisionRepository()
        val ownerService = OwnerService(ownerRepository, ids)
        val patientService = PatientService(petRepository, ownerRepository, ids)
        val appointmentService = AppointmentService(appointmentRepository, petRepository, ids)
        val medicalRecordService = MedicalRecordService(
            recordRepository,
            petRepository,
            appointmentRepository,
            ids,
            revisionRepository
        )
        return Fixture(
            ownerService = ownerService,
            patientService = patientService,
            appointmentService = appointmentService,
            medicalRecordService = medicalRecordService,
            viewModel = MedicalRecordFormViewModel(medicalRecordService)
        )
    }

    @Test
    fun `blank required medical record fields produce inline validation errors without saving`() {
        val app = fixture()

        app.viewModel.save()

        assertEquals("Patient is required.", app.viewModel.state.fieldErrors[MedicalRecordFormField.PATIENT_ID])
        assertEquals("Diagnosis is required.", app.viewModel.state.fieldErrors[MedicalRecordFormField.DIAGNOSIS])
        assertEquals("Treatment is required.", app.viewModel.state.fieldErrors[MedicalRecordFormField.TREATMENT])
        assertNull(app.viewModel.state.savedRecordId)
        assertTrue(app.medicalRecordService.listRecords().isEmpty())
    }

    @Test
    fun `invalid recorded date time produces inline validation error`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        val patient = app.patientService.registerPet(owner.id, "Darcy", "Dog")
        app.viewModel.startCreate(patient.id)
        app.viewModel.updateDiagnosis("Otitis externa")
        app.viewModel.updateTreatment("Ear cleaning")
        app.viewModel.updateRecordedAt("2026/06/23 09:30")

        app.viewModel.save()

        assertEquals(
            "Recorded date and time must use YYYY-MM-DDTHH:MM.",
            app.viewModel.state.fieldErrors[MedicalRecordFormField.RECORDED_AT]
        )
        assertTrue(app.medicalRecordService.listRecords().isEmpty())
    }

    @Test
    fun `create mode saves medical record and switches to edit mode`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        val patient = app.patientService.registerPet(owner.id, "Darcy", "Dog")
        val appointment = app.appointmentService.scheduleAppointment(
            petId = patient.id,
            scheduledAt = LocalDateTime.of(2026, 6, 23, 9, 30),
            reason = "Ear check",
            visitType = VisitType.GENERAL
        )
        app.viewModel.startCreate(patient.id, appointment.id)
        app.viewModel.updateDiagnosis("  Otitis externa  ")
        app.viewModel.updateTreatment(" Ear cleaning and drops ")
        app.viewModel.updateNotes("Return if scratching continues")
        app.viewModel.updateRecordedAt("2026-06-23T10:15")
        app.viewModel.updateVeterinarianName(" Dr. Sari ")

        app.viewModel.save()

        val record = app.medicalRecordService.getRecord("REC-0001")
        assertFalse(app.viewModel.state.isSaving)
        assertEquals(MedicalRecordFormMode.EDIT, app.viewModel.state.mode)
        assertEquals("REC-0001", app.viewModel.state.savedRecordId)
        assertEquals("Medical record created.", app.viewModel.state.successMessage)
        assertEquals(patient.id, record.petId)
        assertEquals(appointment.id, record.appointmentId)
        assertEquals("Otitis externa", record.diagnosis)
        assertEquals("Ear cleaning and drops", record.treatment)
        assertEquals("Return if scratching continues", record.notes)
        assertEquals(LocalDateTime.of(2026, 6, 23, 10, 15), record.recordedAt)
        assertEquals("Dr. Sari", record.veterinarianName)
    }

    @Test
    fun `load and update existing medical record`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Nadia Prasetyo", "0811111111")
        val patient = app.patientService.registerPet(owner.id, "Miso", "Cat")
        val record = app.medicalRecordService.createRecord(
            petId = patient.id,
            diagnosis = "Dermatitis",
            treatment = "Topical medication",
            notes = "Initial note",
            recordedAt = LocalDateTime.of(2026, 6, 23, 11, 0),
            veterinarianName = "Dr. Bima"
        )

        app.viewModel.load(record.id)
        app.viewModel.updateDiagnosis("Dermatitis improving")
        app.viewModel.updateTreatment("Continue medication")
        app.viewModel.updateNotes("Recheck next week")
        app.viewModel.save()

        val updated = app.medicalRecordService.getRecord(record.id)
        assertEquals(MedicalRecordFormMode.EDIT, app.viewModel.state.mode)
        assertEquals("Medical record updated.", app.viewModel.state.successMessage)
        assertEquals("Dermatitis improving", updated.diagnosis)
        assertEquals("Continue medication", updated.treatment)
        assertEquals("Recheck next week", updated.notes)
        assertEquals("Dr. Bima", updated.veterinarianName)
        assertEquals(1, app.medicalRecordService.listRevisions(record.id).size)
    }

    @Test
    fun `appointment mismatch from service is shown as form error message`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Lia Santoso", "0822222222")
        val darcy = app.patientService.registerPet(owner.id, "Darcy", "Dog")
        val miso = app.patientService.registerPet(owner.id, "Miso", "Cat")
        val appointment = app.appointmentService.scheduleAppointment(
            petId = miso.id,
            scheduledAt = LocalDateTime.of(2026, 6, 23, 13, 0),
            reason = "Skin check"
        )
        app.viewModel.startCreate(darcy.id, appointment.id)
        app.viewModel.updateDiagnosis("Otitis externa")
        app.viewModel.updateTreatment("Ear drops")

        app.viewModel.save()

        assertEquals("Medical record appointment must belong to the same pet.", app.viewModel.state.errorMessage)
        assertNull(app.viewModel.state.successMessage)
        assertNull(app.viewModel.state.savedRecordId)
        assertTrue(app.medicalRecordService.listRecords().isEmpty())
    }

    @Test
    fun `start create can prefill patient and appointment and clears loaded record state`() {
        val app = fixture()
        val owner = app.ownerService.registerOwner("Maya Hartono", "0833333333")
        val patient = app.patientService.registerPet(owner.id, "Darcy", "Dog")
        val record = app.medicalRecordService.createRecord(
            petId = patient.id,
            diagnosis = "Otitis externa",
            treatment = "Ear drops",
            notes = "Initial note"
        )

        app.viewModel.load(record.id)
        app.viewModel.startCreate(patient.id, "APT-0001")

        assertEquals(MedicalRecordFormMode.CREATE, app.viewModel.state.mode)
        assertNull(app.viewModel.state.recordId)
        assertEquals(patient.id, app.viewModel.state.patientId)
        assertEquals("APT-0001", app.viewModel.state.appointmentId)
        assertEquals("", app.viewModel.state.diagnosis)
        assertEquals("", app.viewModel.state.treatment)
    }

    private data class Fixture(
        val ownerService: OwnerService,
        val patientService: PatientService,
        val appointmentService: AppointmentService,
        val medicalRecordService: MedicalRecordService,
        val viewModel: MedicalRecordFormViewModel
    )
}
