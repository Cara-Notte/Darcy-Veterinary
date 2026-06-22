package darcy.veterinary.application

data class GuiRuntime(
    val ownerService: OwnerService,
    val patientService: PatientService,
    val appointmentService: AppointmentService
)
