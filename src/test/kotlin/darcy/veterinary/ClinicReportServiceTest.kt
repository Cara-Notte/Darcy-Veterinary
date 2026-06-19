package darcy.veterinary

import darcy.veterinary.application.AppointmentService
import darcy.veterinary.application.BillingService
import darcy.veterinary.application.ClinicReportService
import darcy.veterinary.application.OwnerService
import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.model.ClinicService
import darcy.veterinary.infrastructure.memory.InMemoryAppointmentRepository
import darcy.veterinary.infrastructure.memory.InMemoryInvoiceRepository
import darcy.veterinary.infrastructure.memory.InMemoryOwnerRepository
import darcy.veterinary.infrastructure.memory.InMemoryPetRepository
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ClinicReportServiceTest {
    @Test
    fun `overview report summarizes clinic operations`() {
        val ids = SequenceIdGenerator()
        val ownerRepository = InMemoryOwnerRepository()
        val petRepository = InMemoryPetRepository()
        val appointmentRepository = InMemoryAppointmentRepository()
        val invoiceRepository = InMemoryInvoiceRepository()

        val ownerService = OwnerService(ownerRepository, ids)
        val patientService = PatientService(petRepository, ownerRepository, ids)
        val appointmentService = AppointmentService(appointmentRepository, petRepository, ids)
        val billingService = BillingService(invoiceRepository, petRepository, ids)
        val reportService = ClinicReportService(ownerRepository, petRepository, appointmentRepository, invoiceRepository)

        val owner = ownerService.registerOwner("Report Owner", "0800000000")
        val pet = patientService.registerPet(owner.id, "Report Pet", "Dog")
        val todayAppointment = appointmentService.scheduleAppointment(
            pet.id,
            LocalDateTime.of(2026, 6, 19, 9, 0),
            "Today appointment"
        )
        appointmentService.completeAppointment(todayAppointment.id)
        appointmentService.scheduleAppointment(
            pet.id,
            LocalDateTime.of(2026, 6, 20, 9, 0),
            "Tomorrow appointment"
        )
        val paidInvoice = billingService.createInvoice(pet.id, listOf(ClinicService.CONSULTATION, ClinicService.GROOMING))
        billingService.markAsPaid(paidInvoice.id)
        billingService.createInvoice(pet.id, listOf(ClinicService.VACCINATION))

        val report = reportService.generateOverview(LocalDate.of(2026, 6, 19))

        assertEquals(1, report.totalOwners)
        assertEquals(1, report.totalPets)
        assertEquals(2, report.totalAppointments)
        assertEquals(1, report.todayAppointments)
        assertEquals(1, report.completedAppointments)
        assertEquals(1, report.unpaidInvoices)
        assertEquals(250_000.0, report.paidRevenue)
    }
}
