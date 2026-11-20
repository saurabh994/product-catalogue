package com.saurabh.catalogue.domain.repository

import com.saurabh.catalogue.domain.common.Outcome
import com.saurabh.catalogue.domain.model.EnquiryConfirmation
import com.saurabh.catalogue.domain.model.EnquiryRequest

interface EnquiryRepository {
    suspend fun submit(request: EnquiryRequest): Outcome<EnquiryConfirmation>
}
