package darcy.veterinary.presentation.desktop

import darcy.veterinary.application.AppRuntime
import darcy.veterinary.application.AppRuntimeFactory
import darcy.veterinary.application.AppointmentBoardFacade
import darcy.veterinary.application.ClinicWorkspaceFacade
import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.presentation.desktop.viewmodel.AppointmentBoardViewModel
import darcy.veterinary.presentation.desktop.viewmodel.PatientSearchViewModel
import java.time.LocalDate

data class DesktopRuntime(
    val appRuntime: AppRuntime,
    val workspaceFacade: ClinicWorkspaceFacade,
    val appointmentBoardFacade: AppointmentBoardFacade,
    val patientSearchViewModel: PatientSearchViewModel,
    val appointmentBoardViewModel: AppointmentBoardViewModel
)

object DesktopRuntimeFactory {
    fun sqlite(
        config: DatabaseConfig = DatabaseConfig(),
        initialBoardDate: LocalDate = LocalDate.now()
    ): DesktopRuntime {
        val appRuntime = AppRuntimeFactory.sqlite(config)
        val workspaceFacade = appRuntime.clinicWorkspaceFacade
        val appointmentBoardFacade = appRuntime.appointmentBoardFacade

        return DesktopRuntime(
            appRuntime = appRuntime,
            workspaceFacade = workspaceFacade,
            appointmentBoardFacade = appointmentBoardFacade,
            patientSearchViewModel = PatientSearchViewModel(workspaceFacade),
            appointmentBoardViewModel = AppointmentBoardViewModel(appointmentBoardFacade, initialBoardDate)
        )
    }
}
