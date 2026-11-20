package com.saurabh.catalogue.fakes

import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.common.Outcome
import com.saurabh.catalogue.domain.model.EnquiryConfirmation
import com.saurabh.catalogue.domain.model.EnquiryRequest
import com.saurabh.catalogue.domain.repository.EnquiryRepository

/** Records submissions. Set [error] to make the next submissions fail. */
class FakeEnquiryRepository : EnquiryRepository {
    var error: AppError? = null
    val submitted = mutableListOf<EnquiryRequest>()

    override suspend fun submit(request: EnquiryRequest): Outcome<EnquiryConfirmation> {
        submitted += request
        return error?.let { Outcome.Failure(it) } ?: Outcome.Success(EnquiryConfirmation("enquiry-${submitted.size}"))
    }
}
