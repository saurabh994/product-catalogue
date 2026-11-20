package com.saurabh.catalogue.data.repository

import com.saurabh.catalogue.data.mapper.toDomain
import com.saurabh.catalogue.data.remote.ApiBaseUrl
import com.saurabh.catalogue.data.remote.CatalogueApi
import com.saurabh.catalogue.data.remote.apiCall
import com.saurabh.catalogue.domain.repository.ProductRepository
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import javax.inject.Inject

/** Remote implementation: calls our backend and maps DTOs to domain models. */
class ProductRepositoryImpl @Inject constructor(
    private val api: CatalogueApi,
    @param:ApiBaseUrl private val baseUrl: HttpUrl,
    private val json: Json,
) : ProductRepository {

    override suspend fun featured() = apiCall(json) {
        api.featured().products.map { it.toDomain(baseUrl) }
    }

    override suspend fun categories() = apiCall(json) {
        api.categories().categories.map { it.toDomain() }
    }

    override suspend fun categoryProducts(slug: String) = apiCall(json) {
        api.categoryProducts(slug).toDomain(baseUrl)
    }

    override suspend fun search(query: String) = apiCall(json) {
        api.search(query).products.map { it.toDomain(baseUrl) }
    }

    override suspend fun product(id: String) = apiCall(json) {
        api.product(id).product.toDomain(baseUrl)
    }
}
