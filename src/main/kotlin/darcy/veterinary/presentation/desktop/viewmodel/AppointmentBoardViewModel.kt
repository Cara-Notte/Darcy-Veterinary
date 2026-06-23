package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.AppointmentBoardFacade
import darcy.veterinary.application.AppointmentBoardViewData
import darcy.veterinary.domain.model.AppointmentStatus
import java.time.LocalDate

class AppointmentBoardViewModel(
    private val appointmentBoardFacade: AppointmentBoardFacade,
    initialDate: LocalDate
) {
    var state: AppointmentBoardState = AppointmentBoardState(selectedDate = initialDate)
        private set

    fun load() {
        loadBoard(state.selectedDate, state.statusFilter)
    }

    fun selectDate(date: LocalDate) {
        loadBoard(date, state.statusFilter)
    }

    fun applyStatusFilter(status: AppointmentStatus?) {
        loadBoard(state.selectedDate, status)
    }

    fun clearStatusFilter() {
        applyStatusFilter(null)
    }

    private fun loadBoard(date: LocalDate, statusFilter: AppointmentStatus?) {
        state = state.copy(
            selectedDate = date,
            statusFilter = statusFilter,
            isLoading = true,
            errorMessage = null
        )

        state = try {
            state.copy(
                isLoading = false,
                board = appointmentBoardFacade.dayBoard(date, statusFilter),
                errorMessage = null
            )
        } catch (error: Exception) {
            state.copy(
                isLoading = false,
                board = null,
                errorMessage = error.message ?: "Appointment board could not be loaded."
            )
        }
    }
}

data class AppointmentBoardState(
    val selectedDate: LocalDate,
    val statusFilter: AppointmentStatus? = null,
    val isLoading: Boolean = false,
    val board: AppointmentBoardViewData? = null,
    val errorMessage: String? = null
) {
    val emptyStateMessage: String?
        get() = when {
            isLoading || errorMessage != null -> null
            board == null -> "Load the appointment board to view the daily schedule."
            !board.hasAppointments && statusFilter == null -> "No appointments scheduled for this date."
            !board.hasAppointments && statusFilter != null -> "No appointments match the selected status for this date."
            else -> null
        }
}
