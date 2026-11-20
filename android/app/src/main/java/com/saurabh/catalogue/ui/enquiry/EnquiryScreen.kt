package com.saurabh.catalogue.ui.enquiry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurabh.catalogue.R
import com.saurabh.catalogue.domain.usecase.EnquiryValidator
import com.saurabh.catalogue.ui.common.AppTopBar
import com.saurabh.catalogue.ui.common.messageRes
import com.saurabh.catalogue.ui.theme.CatalogueTheme

@Composable
fun EnquiryScreen(
    onBack: () -> Unit,
    onSent: (productTitle: String) -> Unit,
    viewModel: EnquiryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // One-off navigation once the backend confirms the send.
    LaunchedEffect(state.sent) {
        if (state.sent) onSent(state.productTitle)
    }

    EnquiryContent(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPhoneChange = viewModel::onPhoneChange,
        onMessageChange = viewModel::onMessageChange,
        onSubmit = viewModel::submit,
    )
}

@Composable
fun EnquiryContent(
    state: EnquiryFormState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val spacing = CatalogueTheme.spacing
    Scaffold(topBar = { AppTopBar(stringResource(R.string.enquiry_title), onBack) }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(stringResource(R.string.enquiry_heading, state.productTitle), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.enquiry_intro), style = MaterialTheme.typography.bodyMedium)

            FormField(
                value = state.name, onValueChange = onNameChange, label = stringResource(R.string.enquiry_name),
                error = state.fieldErrors[EnquiryValidator.NAME], tag = "enquiry_name", enabled = !state.isSubmitting,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            FormField(
                value = state.email, onValueChange = onEmailChange, label = stringResource(R.string.enquiry_email),
                error = state.fieldErrors[EnquiryValidator.EMAIL], tag = "enquiry_email", enabled = !state.isSubmitting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            FormField(
                value = state.phone, onValueChange = onPhoneChange, label = stringResource(R.string.enquiry_phone),
                error = state.fieldErrors[EnquiryValidator.PHONE], tag = "enquiry_phone", enabled = !state.isSubmitting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            )
            FormField(
                value = state.message, onValueChange = onMessageChange, label = stringResource(R.string.enquiry_message),
                error = state.fieldErrors[EnquiryValidator.MESSAGE], tag = "enquiry_message", enabled = !state.isSubmitting,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = false,
            )

            state.submitError?.let { error ->
                Text(
                    text = stringResource(R.string.enquiry_send_failed, stringResource(error.messageRes())),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .testTag("enquiry_submit_error")
                        .semantics { liveRegion = LiveRegionMode.Polite }, // announced by TalkBack
                )
            }

            Button(
                onClick = onSubmit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().testTag("enquiry_submit"),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    // After a failed send the same button retries, keeping what the user typed.
                    Text(stringResource(if (state.submitError != null) R.string.action_retry else R.string.enquiry_submit))
                }
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    tag: String,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        // if/else (not error?.let) so the lambda gets the parameter's @Composable type.
        supportingText = if (error != null) {
            { Text(error, modifier = Modifier.testTag("${tag}_error")) }
        } else {
            null
        },
        enabled = enabled,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 4,
        keyboardOptions = keyboardOptions,
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}
