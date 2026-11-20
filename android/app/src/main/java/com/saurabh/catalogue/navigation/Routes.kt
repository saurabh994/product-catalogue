package com.saurabh.catalogue.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations (Navigation Compose 2.8+).
 * Navigation stores each argument in the ViewModel's SavedStateHandle under its property name;
 * [RouteArgs] names those keys so ViewModels (and their JVM tests) can read them without Android APIs.
 */
@Serializable data object HomeRoute

@Serializable data class CategoryRoute(val slug: String, val name: String)

@Serializable data class SearchRoute(val query: String)

@Serializable data class ProductRoute(val id: String)

@Serializable data class EnquiryRoute(val productId: String, val productTitle: String)

@Serializable data class ThankYouRoute(val productTitle: String)

object RouteArgs {
    const val CATEGORY_SLUG = "slug"
    const val CATEGORY_NAME = "name"
    const val SEARCH_QUERY = "query"
    const val PRODUCT_ID = "id"
    const val ENQUIRY_PRODUCT_ID = "productId"
    const val ENQUIRY_PRODUCT_TITLE = "productTitle"
}
