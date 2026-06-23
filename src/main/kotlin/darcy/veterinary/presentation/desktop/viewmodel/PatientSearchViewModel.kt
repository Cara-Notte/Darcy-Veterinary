package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.ClinicWorkspaceFacade
import darcy.veterinary.application.ClinicWorkspaceSearchResult
import darcy.veterinary.application.PatientChartViewData

class PatientSearchViewModel(
    private val workspaceFacade: ClinicWorkspaceFacade
) {
    var state: PatientSearchState = PatientSearchState()
        private set

    fun updateQuery(query: String) {
        state = state.copy(query = query, validationMessage = null, errorMessage = null)
    }

    fun search() {
        val query = state.query.trim()
        if (query.isBlank()) {
            state = state.copy(
                query = query,
                searchResult = ClinicWorkspaceSearchResult.empty(query),
                selectedChart = null,
                validationMessage = "Enter a search term.",
                errorMessage = null,
                isLoading = false
            )
            return
        }

        state = state.copy(
            isLoading = false,
            query = query,
            searchResult = workspaceFacade.search(query),
            selectedChart = null,
            validationMessage = null,
            errorMessage = null
        )
    }

    fun openPatientChart(patientId: String) {
        state = state.copy(
            isLoading = false,
            selectedChart = workspaceFacade.patientChart(patientId),
            validationMessage = null,
            errorMessage = null
        )
    }

    fun clearSelectedChart() {
        state = state.copy(selectedChart = null)
    }
}

data class PatientSearchState(
    val query: String = "",
    val isLoading: Boolean = false,
    val searchResult: ClinicWorkspaceSearchResult = ClinicWorkspaceSearchResult.empty(),
    val selectedChart: PatientChartViewData? = null,
    val validationMessage: String? = null,
    val errorMessage: String? = null
) {
    val emptyStateMessage: String?
        get() = when {
            validationMessage != null || errorMessage != null || isLoading -> null
            query.isBlank() && !searchResult.hasResults -> "Search for an owner or patient to begin."
            query.isNotBlank() && !searchResult.hasResults -> "No owners or patients match \"$query\"."
            else -> null
        }
}
