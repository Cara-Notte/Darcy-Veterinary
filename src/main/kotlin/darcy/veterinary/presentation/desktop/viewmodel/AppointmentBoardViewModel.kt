package darcy.veterinary.presentation.desktop.viewmodel

import darcy.veterinary.application.AppointmentBoardFacade
import darcy.veterinary.application.AppointmentBoardViewData
import darcy.veterinary.application.AppointmentBoardRow
import darcy.veterinary.domain.model.AppointmentStatus
import java.time.LocalDate
import java.time.LocalDateTime

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

    fun requestCompleteAppointment(appointmentId: String) {
        requestAction(AppointmentBoardAction.COMPLETE, appointmentId)
    }

    fun requestCancelAppointment(appointmentId: String) {
        requestAction(AppointmentBoardAction.CANCEL, appointmentId)
    }

    fun dismissPendingAction() {
        state = state.copy(pendingAction = null)
    }

    fun confirmPendingAction() {
        val pendingAction = state.pendingAction
        if (pendingAction == null) {
            state = state.copy(
                errorMessage = "No appointment action is awaiting confirmation.",
                successMessage = null
            )
            return
        }

        try {
            when (pendingAction.action) {
                AppointmentBoardAction.COMPLETE -> appointmentBoardFacade.completeAppointment(pendingAction.appointmentId)
                AppointmentBoardAction.CANCEL -> appointmentBoardFacade.cancelAppointment(pendingAction.appointmentId)
            }
            loadBoard(
                date = state.selectedDate,
                statusFilter = state.statusFilter,
                successMessage = when (pendingAction.action) {
                    AppointmentBoardAction.COMPLETE -> "Appointment marked as completed."
                    AppointmentBoardAction.CANCEL -> "Appointment cancelled."
                }
            )
        } catch (error: Exception) {
            state = state.copy(
                isLoading = false,
                pendingAction = null,
                errorMessage = error.message ?: "Appointment action failed.",
                successMessage = null
            )
        }
    }

    private fun requestAction(action: AppointmentBoardAction, appointmentId: String) {
        val appointment = state.board?.rows?.firstOrNull { it.id == appointmentId }
        if (appointment == null) {
            state = state.copy(
                pendingAction = null,
                errorMessage = "Load the appointment board and select an appointment first.",
                successMessage = null
            )
            return
        }

        if (appointment.status != AppointmentStatus.SCHEDULED) {
            state = state.copy(
                pendingAction = null,
                errorMessage = "Only scheduled appointments can be updated.",
                successMessage = null
            )
            return
        }

        state = state.copy(
            pendingAction = PendingAppointmentAction.from(action, appointment),
            errorMessage = null,
            successMessage = null
        )
    }

    private fun loadBoard(
        date: LocalDate,
        statusFilter: AppointmentStatus?,
        successMessage: String? = null
    ) {
        state = state.copy(
            selectedDate = date,
            statusFilter = statusFilter,
            isLoading = true,
            pendingAction = null,
            errorMessage = null,
            successMessage = successMessage
        )

        state = try {
            state.copy(
                isLoading = false,
                board = appointmentBoardFacade.dayBoard(date, statusFilter),
                pendingAction = null,
                errorMessage = null,
                successMessage = successMessage
            )
        } catch (error: Exception) {
            state.copy(
                isLoading = false,
                board = null,
                pendingAction = null,
                errorMessage = error.message ?: "Appointment board could not be loaded.",
                successMessage = null
            )
        }
    }
}

enum class AppointmentBoardAction {
    COMPLETE,
    CANCEL
}

data class PendingAppointmentAction(
    val action: AppointmentBoardAction,
    val appointmentId: String,
    val patientName: String,
    val scheduledAt: LocalDateTime
) {
    companion object {
        fun from(action: AppointmentBoardAction, row: AppointmentBoardRow): PendingAppointmentAction =
            PendingAppointmentAction(
                action = action,
                appointmentId = row.id,
                patientName = row.patientName,
                scheduledAt = row.scheduledAt
            )
    }
}

data class AppointmentBoardState(
    val selectedDate: LocalDate,
    val statusFilter: AppointmentStatus? = null,
    val isLoading: Boolean = false,
    val board: AppointmentBoardViewData? = null,
    val pendingAction: PendingAppointmentAction? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
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
