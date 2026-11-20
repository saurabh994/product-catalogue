package com.saurabh.catalogue.data.remote

import com.saurabh.catalogue.BuildConfig

/**
 * Every network value the app uses, in one place.
 * The base URL itself is per build type, so it lives in gradle.properties → BuildConfig and is exposed here.
 */
object NetworkConstants {

    /** e.g. http://10.0.2.2:3000/ for debug on the emulator. Must end with "/". */
    val BASE_URL: String = BuildConfig.API_BASE_URL

    const val CONTENT_TYPE_JSON = "application/json"
    const val CONNECT_TIMEOUT_SECONDS = 10L
    const val READ_TIMEOUT_SECONDS = 15L

    /** Relative paths (no leading "/") so a base URL with a path prefix is preserved. */
    object Endpoints {
        const val FEATURED_PRODUCTS = "v1/products/featured"
        const val CATEGORIES = "v1/categories"
        const val CATEGORY_PRODUCTS = "v1/categories/{${Params.SLUG}}/products"
        const val SEARCH_PRODUCTS = "v1/products"
        const val PRODUCT = "v1/products/{${Params.ID}}"
        const val ENQUIRIES = "v1/enquiries"
    }

    object Params {
        const val SLUG = "slug"
        const val ID = "id"
        const val QUERY = "q"
    }
}
