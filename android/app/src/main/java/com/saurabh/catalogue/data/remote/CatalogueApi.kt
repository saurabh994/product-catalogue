package com.saurabh.catalogue.data.remote

import com.saurabh.catalogue.data.remote.dto.CategoriesResponse
import com.saurabh.catalogue.data.remote.dto.CategoryProductsResponse
import com.saurabh.catalogue.data.remote.dto.EnquiryRequestDto
import com.saurabh.catalogue.data.remote.dto.EnquiryResponseDto
import com.saurabh.catalogue.data.remote.dto.ProductResponse
import com.saurabh.catalogue.data.remote.dto.ProductsResponse
import com.saurabh.catalogue.data.remote.NetworkConstants.Endpoints
import com.saurabh.catalogue.data.remote.NetworkConstants.Params
import com.saurabh.catalogue.data.remote.dto.SearchResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Our backend only — the app never talks to Fake Store. Paths live in [NetworkConstants]. */
interface CatalogueApi {
    @GET(Endpoints.FEATURED_PRODUCTS)
    suspend fun featured(): ProductsResponse

    @GET(Endpoints.CATEGORIES)
    suspend fun categories(): CategoriesResponse

    @GET(Endpoints.CATEGORY_PRODUCTS)
    suspend fun categoryProducts(@Path(Params.SLUG) slug: String): CategoryProductsResponse

    @GET(Endpoints.SEARCH_PRODUCTS)
    suspend fun search(@Query(Params.QUERY) query: String): SearchResponse

    @GET(Endpoints.PRODUCT)
    suspend fun product(@Path(Params.ID) id: String): ProductResponse

    @POST(Endpoints.ENQUIRIES)
    suspend fun submitEnquiry(@Body body: EnquiryRequestDto): EnquiryResponseDto
}
