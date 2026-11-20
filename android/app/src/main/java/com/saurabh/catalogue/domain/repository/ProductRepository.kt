package com.saurabh.catalogue.domain.repository

import com.saurabh.catalogue.domain.common.Outcome
import com.saurabh.catalogue.domain.model.Category
import com.saurabh.catalogue.domain.model.CategoryProducts
import com.saurabh.catalogue.domain.model.Product

/**
 * Owned by the domain layer; implemented in data. The domain never knows whether products
 * come from our API, a cache or a database (dependency rule: data → domain, never the reverse).
 */
interface ProductRepository {
    suspend fun featured(): Outcome<List<Product>>
    suspend fun categories(): Outcome<List<Category>>
    suspend fun categoryProducts(slug: String): Outcome<CategoryProducts>
    suspend fun search(query: String): Outcome<List<Product>>
    suspend fun product(id: String): Outcome<Product>
}
