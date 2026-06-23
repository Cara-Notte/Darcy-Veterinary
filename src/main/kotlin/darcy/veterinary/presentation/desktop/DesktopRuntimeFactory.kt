package darcy.veterinary.presentation.desktop

import darcy.veterinary.application.AppRuntime
import darcy.veterinary.application.AppRuntimeFactory
import darcy.veterinary.application.ClinicWorkspaceFacade
import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.presentation.desktop.viewmodel.PatientSearchViewModel

data class DesktopRuntime(
    val appRuntime: AppRuntime,
    val workspaceFacade: ClinicWorkspaceFacade,
    val patientSearchViewModel: PatientSearchViewModel
)

object DesktopRuntimeFactory {
    fun sqlite(config: DatabaseConfig = DatabaseConfig()): DesktopRuntime {
        val appRuntime = AppRuntimeFactory.sqlite(config)
        val workspaceFacade = appRuntime.clinicWorkspaceFacade

        return DesktopRuntime(
            appRuntime = appRuntime,
            workspaceFacade = workspaceFacade,
            patientSearchViewModel = PatientSearchViewModel(workspaceFacade)
        )
    }
}
