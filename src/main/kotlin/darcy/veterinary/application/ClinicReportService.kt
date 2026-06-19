package darcy.veterinary.application

import darcy.veterinary.domain.model.AppointmentStatus
import darcy.veterinary.domain.model.PaymentStatus
import darcy.veterinary.repository.AppointmentRepository
import darcy.veterinary.repository.InvoiceRepository
import darcy.veterinary.repository.OwnerRepository
import darcy.veterinary.repository.PetRepository
import java.time.LocalDate

data class ClinicOverviewReport(
    val reportDate: LocalDate,
    val totalOwners: Int,
    val totalPets: Int,
    val totalAppointments: Int,
    val todayAppointments: Int,
    val completedAppointments: Int,
    val cancelledAppointments: Int,
    val unpaidInvoices: Int,
    val paidInvoices: Int,
    val voidedInvoices: Int,
    val paidRevenue: Double
)

class ClinicReportService(
    private val ownerRepository: OwnerRepository,
    private val petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
    private val invoiceRepository: InvoiceRepository
) {
    fun generateOverview(reportDate: LocalDate = LocalDate.now()): ClinicOverviewReport {
        val appointments = appointmentRepository.findAll()
        val invoices = invoiceRepository.findAll()

        return ClinicOverviewReport(
            reportDate = reportDate,
            totalOwners = ownerRepository.findAll().size,
            totalPets = petRepository.findAll().size,
            totalAppointments = appointments.size,
            todayAppointments = appointments.count { it.scheduledAt.toLocalDate() == reportDate },
            completedAppointments = appointments.count { it.status == AppointmentStatus.COMPLETED },
            cancelledAppointments = appointments.count { it.status == AppointmentStatus.CANCELLED },
            unpaidInvoices = invoices.count { it.paymentStatus == PaymentStatus.UNPAID },
            paidInvoices = invoices.count { it.paymentStatus == PaymentStatus.PAID },
            voidedInvoices = invoices.count { it.paymentStatus == PaymentStatus.VOIDED },
            paidRevenue = invoices.filter { it.paymentStatus == PaymentStatus.PAID }.sumOf { it.total() }
        )
    }
}
