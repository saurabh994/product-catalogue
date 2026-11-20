package com.saurabh.catalogue.ui.enquiry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.common.Outcome
import com.saurabh.catalogue.domain.model.EnquiryRequest
import com.saurabh.catalogue.domain.usecase.EnquiryValidator
import com.saurabh.catalogue.domain.usecase.SubmitEnquiryUseCase
import com.saurabh.catalogue.navigation.RouteArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EnquiryFormState(
    val productTitle: String,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val message: String = "",
    /** Field → message, from local validation or the backend's 400 response. */
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    /** Sending failed for a non-field reason (network, server, rate limit): show with a retry. */
    val submitError: AppError? = null,
    /** True only after the backend confirms the email was sent — then the screen moves to Thank you. */
    val sent: Boolean = false,
)

@HiltViewModel
class EnquiryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val submitEnquiry: SubmitEnquiryUseCase,
) : ViewModel() {

    private val productId: String = checkNotNull(savedStateHandle[RouteArgs.ENQUIRY_PRODUCT_ID]) { "Missing product id" }

    private val _state = MutableStateFlow(EnquiryFormState(productTitle = savedStateHandle[RouteArgs.ENQUIRY_PRODUCT_TITLE] ?: ""))
    val state: StateFlow<EnquiryFormState> = _state.asStateFlow()

    // Editing a field clears that field's error and any send error, so the form never shows stale messages.
    fun onNameChange(value: String) = edit(EnquiryValidator.NAME) { copy(name = value) }
    fun onEmailChange(value: String) = edit(EnquiryValidator.EMAIL) { copy(email = value) }
    fun onPhoneChange(value: String) = edit(EnquiryValidator.PHONE) { copy(phone = value) }
    fun onMessageChange(value: String) = edit(EnquiryValidator.MESSAGE) { copy(message = value) }

    private fun edit(field: String, change: EnquiryFormState.() -> EnquiryFormState) {
        _state.update { it.change().copy(fieldErrors = it.fieldErrors - field, submitError = null) }
    }

    /** Also used as "retry" after a failed send: the form keeps everything the user typed. */
    fun submit() {
        val form = _state.value
        if (form.isSubmitting || form.sent) return // no double submissions
        _state.update { it.copy(isSubmitting = true, submitError = null) }

        viewModelScope.launch {
            val request = EnquiryRequest(productId, form.name, form.email, form.phone, form.message)
            when (val result = submitEnquiry(request)) {
                is Outcome.Success -> _state.update { it.copy(isSubmitting = false, sent = true) }
                is Outcome.Failure -> _state.update { it.withFailure(result.error) }
            }
        }
    }

    private fun EnquiryFormState.withFailure(error: AppError): EnquiryFormState {
        val formErrors = (error as? AppError.Validation)?.fieldErrors?.filterKeys { it in EnquiryValidator.FORM_FIELDS }
        return if (!formErrors.isNullOrEmpty()) {
            copy(isSubmitting = false, fieldErrors = formErrors)
        } else {
            // e.g. productId rejected, network down, email provider failed, rate limited
            copy(isSubmitting = false, submitError = error)
        }
    }
}
