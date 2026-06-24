package darcy.veterinary.presentation.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import darcy.veterinary.domain.model.PetSex
import darcy.veterinary.presentation.desktop.theme.DarcyColor
import darcy.veterinary.presentation.desktop.viewmodel.OwnerFormField
import darcy.veterinary.presentation.desktop.viewmodel.OwnerFormMode
import darcy.veterinary.presentation.desktop.viewmodel.OwnerFormState
import darcy.veterinary.presentation.desktop.viewmodel.PatientFormField
import darcy.veterinary.presentation.desktop.viewmodel.PatientFormMode
import darcy.veterinary.presentation.desktop.viewmodel.PatientFormState

@Composable
internal fun OwnerProfileFormPanel(
    state: OwnerFormState,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSave: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = when (state.mode) {
                    OwnerFormMode.CREATE -> "New owner profile"
                    OwnerFormMode.EDIT -> "Edit owner profile"
                },
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = DarcyColor.TextPrimary
            )
            LabeledTextField(
                value = state.fullName,
                onValueChange = onFullNameChange,
                label = "Owner name",
                error = state.fieldErrors[OwnerFormField.FULL_NAME]
            )
            LabeledTextField(
                value = state.phoneNumber,
                onValueChange = onPhoneChange,
                label = "Phone number",
                error = state.fieldErrors[OwnerFormField.PHONE_NUMBER]
            )
            LabeledTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "Email"
            )
            state.errorMessage?.let { ErrorState(it) }
            state.successMessage?.let { SuccessState(it) }
            Button(onClick = onSave, enabled = state.canAttemptSave) {
                Text(if (state.mode == OwnerFormMode.CREATE) "Create owner" else "Save owner")
            }
        }
    }
}

@Composable
internal fun PatientProfileFormPanel(
    state: PatientFormState,
    onOwnerIdChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onSpeciesChange: (String) -> Unit,
    onBreedChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onSexChange: (PetSex?) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onAllergiesChange: (String) -> Unit,
    onConditionsChange: (String) -> Unit,
    onSave: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = when (state.mode) {
                    PatientFormMode.CREATE -> "New patient profile"
                    PatientFormMode.EDIT -> "Edit patient profile"
                },
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = DarcyColor.TextPrimary
            )
            LabeledTextField(
                value = state.ownerId,
                onValueChange = onOwnerIdChange,
                label = "Owner ID",
                error = state.fieldErrors[PatientFormField.OWNER_ID]
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                LabeledTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = "Patient name",
                    error = state.fieldErrors[PatientFormField.NAME],
                    modifier = Modifier.weight(1f)
                )
                LabeledTextField(
                    value = state.species,
                    onValueChange = onSpeciesChange,
                    label = "Species",
                    error = state.fieldErrors[PatientFormField.SPECIES],
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                LabeledTextField(
                    value = state.breed,
                    onValueChange = onBreedChange,
                    label = "Breed",
                    modifier = Modifier.weight(1f)
                )
                LabeledTextField(
                    value = state.age,
                    onValueChange = onAgeChange,
                    label = "Age",
                    error = state.fieldErrors[PatientFormField.AGE],
                    modifier = Modifier.weight(1f)
                )
                LabeledTextField(
                    value = state.weightKg,
                    onValueChange = onWeightChange,
                    label = "Weight kg",
                    error = state.fieldErrors[PatientFormField.WEIGHT_KG],
                    modifier = Modifier.weight(1f)
                )
            }
            SexSelector(state.sex, onSexChange)
            LabeledTextField(
                value = state.dateOfBirth,
                onValueChange = onDateOfBirthChange,
                label = "Date of birth, YYYY-MM-DD",
                error = state.fieldErrors[PatientFormField.DATE_OF_BIRTH]
            )
            LabeledTextField(
                value = state.allergies,
                onValueChange = onAllergiesChange,
                label = "Allergies, separated by comma or new line",
                singleLine = false
            )
            LabeledTextField(
                value = state.medicalConditions,
                onValueChange = onConditionsChange,
                label = "Medical conditions, separated by comma or new line",
                singleLine = false
            )
            state.errorMessage?.let { ErrorState(it) }
            state.successMessage?.let { SuccessState(it) }
            Button(onClick = onSave, enabled = state.canAttemptSave) {
                Text(if (state.mode == PatientFormMode.CREATE) "Create patient" else "Save patient")
            }
        }
    }
}

@Composable
private fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null,
    singleLine: Boolean = true
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            isError = error != null,
            singleLine = singleLine
        )
        error?.let { Text(it, color = DarcyColor.SemanticRed, style = MaterialTheme.typography.caption) }
    }
}

@Composable
private fun SexSelector(selected: PetSex?, onSexChange: (PetSex?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Sex", style = MaterialTheme.typography.caption, color = DarcyColor.TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SexOption("Unknown", selected == null || selected == PetSex.UNKNOWN) { onSexChange(PetSex.UNKNOWN) }
            SexOption("Male", selected == PetSex.MALE) { onSexChange(PetSex.MALE) }
            SexOption("Female", selected == PetSex.FEMALE) { onSexChange(PetSex.FEMALE) }
            TextButton(onClick = { onSexChange(null) }) { Text("Clear") }
        }
    }
}

@Composable
private fun SexOption(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = if (selected) "• $label" else label,
            color = if (selected) DarcyColor.ClinicalAmber else DarcyColor.TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SuccessState(message: String) {
    Text(message, color = DarcyColor.ClinicalAmber, fontWeight = FontWeight.Bold)
}
