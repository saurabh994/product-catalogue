package com.saurabh.catalogue.domain.usecase

import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.common.Outcome
import com.saurabh.catalogue.domain.model.Category
import com.saurabh.catalogue.domain.model.CategoryProducts
import com.saurabh.catalogue.domain.model.Product
import com.saurabh.catalogue.domain.repository.ProductRepository
import javax.inject.Inject

// One use case per user action. Most are thin today; they are the place business rules go as the
// product grows (e.g. personalised featured products), without touching ViewModels or the data layer.

/** Home: the business-chosen top-rated products. */
class GetFeaturedProductsUseCase @Inject constructor(private val repository: ProductRepository) {
    suspend operator fun invoke(): Outcome<List<Product>> = repository.featured()
}

/** Category links. */
class GetCategoriesUseCase @Inject constructor(private val repository: ProductRepository) {
    suspend operator fun invoke(): Outcome<List<Category>> = repository.categories()
}

/** Category page: top rated + other products. */
class GetCategoryProductsUseCase @Inject constructor(private val repository: ProductRepository) {
    suspend operator fun invoke(slug: String): Outcome<CategoryProducts> = repository.categoryProducts(slug)
}

/** Product page. */
class GetProductUseCase @Inject constructor(private val repository: ProductRepository) {
    suspend operator fun invoke(id: String): Outcome<Product> = repository.product(id)
}

/** Search: trims the term and rejects blank/over-long input before any network call. */
class SearchProductsUseCase @Inject constructor(private val repository: ProductRepository) {
    suspend operator fun invoke(rawQuery: String): Outcome<List<Product>> {
        val query = rawQuery.trim()
        return when {
            query.isEmpty() -> Outcome.Failure(AppError.Validation(mapOf(FIELD to "Enter a search term")))
            query.length > MAX_LENGTH -> Outcome.Failure(AppError.Validation(mapOf(FIELD to "Search term is too long")))
            else -> repository.search(query)
        }
    }

    companion object {
        const val FIELD = "query"
        /** Matches the backend's limit (DECISIONS.md §2.3). */
        const val MAX_LENGTH = 100
    }
}
