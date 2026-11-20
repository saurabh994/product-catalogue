package com.saurabh.catalogue.domain.usecase

import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.common.Outcome
import com.saurabh.catalogue.domain.model.EnquiryConfirmation
import com.saurabh.catalogue.domain.model.EnquiryRequest
import com.saurabh.catalogue.domain.repository.EnquiryRepository
import javax.inject.Inject

/**
 * Client-side copy of the backend's rules (backend/src/enquiries/enquirySchema.ts) for instant feedback.
 * The backend stays the source of truth: its field errors are shown too.
 */
object EnquiryValidator {
    const val NAME = "name"
    const val EMAIL = "email"
    const val PHONE = "phone"
    const val MESSAGE = "message"
    val FORM_FIELDS = setOf(NAME, EMAIL, PHONE, MESSAGE)

    private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private val PHONE_PATTERN = Regex("^[+0-9 ()-]*$")

    /** Returns field → message for every invalid field; empty when the request is valid. */
    fun validate(request: EnquiryRequest): Map<String, String> = buildMap {
        when {
            request.name.isEmpty() -> put(NAME, "Name is required")
            request.name.length > 100 -> put(NAME, "Name must be 100 characters or fewer")
            request.name.contains('\n') || request.name.contains('\r') -> put(NAME, "Name must be a single line")
        }
        when {
            request.email.isEmpty() -> put(EMAIL, "Email is required")
            request.email.length > 254 || !EMAIL_PATTERN.matches(request.email) -> put(EMAIL, "Enter a valid email address")
        }
        request.phone?.let {
            when {
                it.length > 30 -> put(PHONE, "Phone must be 30 characters or fewer")
                !PHONE_PATTERN.matches(it) -> put(PHONE, "Phone can only contain digits, spaces and + ( ) -")
            }
        }
        request.message?.let {
            if (it.length > 1000) put(MESSAGE, "Message must be 1000 characters or fewer")
        }
    }
}

/** Trims input, validates locally, and only then sends. */
class SubmitEnquiryUseCase @Inject constructor(private val repository: EnquiryRepository) {
    suspend operator fun invoke(request: EnquiryRequest): Outcome<EnquiryConfirmation> {
        val clean = request.copy(
            name = request.name.trim(),
            email = request.email.trim(),
            phone = request.phone?.trim()?.ifEmpty { null },
            message = request.message?.trim()?.ifEmpty { null },
        )
        val errors = EnquiryValidator.validate(clean)
        return if (errors.isNotEmpty()) Outcome.Failure(AppError.Validation(errors)) else repository.submit(clean)
    }
}
