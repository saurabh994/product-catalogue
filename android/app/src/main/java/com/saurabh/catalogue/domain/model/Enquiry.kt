package com.saurabh.catalogue.domain.model

/** What the customer submits. Product details are looked up by the backend, never sent from the app. */
data class EnquiryRequest(
    val productId: String,
    val name: String,
    val email: String,
    val phone: String?,
    val message: String?,
)

data class EnquiryConfirmation(val enquiryId: String)
