package com.saurabh.catalogue.data.repository

import com.saurabh.catalogue.data.remote.CatalogueApi
import com.saurabh.catalogue.data.remote.apiCall
import com.saurabh.catalogue.data.remote.dto.EnquiryRequestDto
import com.saurabh.catalogue.domain.model.EnquiryConfirmation
import com.saurabh.catalogue.domain.model.EnquiryRequest
import com.saurabh.catalogue.domain.repository.EnquiryRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

class EnquiryRepositoryImpl @Inject constructor(
    private val api: CatalogueApi,
    private val json: Json,
) : EnquiryRepository {
    override suspend fun submit(request: EnquiryRequest) = apiCall(json) {
        val response = api.submitEnquiry(
            EnquiryRequestDto(
                productId = request.productId,
                name = request.name,
                email = request.email,
                phone = request.phone,
                message = request.message,
            ),
        )
        EnquiryConfirmation(enquiryId = response.enquiryId)
    }
}
