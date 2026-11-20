package com.saurabh.catalogue.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations (Navigation Compose 2.8+). */
@Serializable data object HomeRoute

@Serializable data class CategoryRoute(val slug: String, val name: String)

@Serializable data class SearchRoute(val query: String)

@Serializable data class ProductRoute(val id: String)

@Serializable data class EnquiryRoute(val productId: String, val productTitle: String)

@Serializable data object ThankYouRoute
