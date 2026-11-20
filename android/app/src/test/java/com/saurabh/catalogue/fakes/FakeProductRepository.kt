package com.saurabh.catalogue.fakes

import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.common.Outcome
import com.saurabh.catalogue.domain.model.Category
import com.saurabh.catalogue.domain.model.CategoryProducts
import com.saurabh.catalogue.domain.model.Price
import com.saurabh.catalogue.domain.model.Product
import com.saurabh.catalogue.domain.model.Rating
import com.saurabh.catalogue.domain.repository.ProductRepository

/** In-memory ProductRepository for use case and ViewModel tests. Set [error] to make every call fail. */
class FakeProductRepository(var products: List<Product> = sampleProducts) : ProductRepository {
    var error: AppError? = null
    val searchCalls = mutableListOf<String>()

    private fun <T> result(value: () -> T): Outcome<T> = error?.let { Outcome.Failure(it) } ?: Outcome.Success(value())

    override suspend fun featured() = result { products.take(5) }

    override suspend fun categories() = result { products.map { it.category }.distinct() }

    override suspend fun categoryProducts(slug: String): Outcome<CategoryProducts> {
        val inCategory = products.filter { it.category.slug == slug }
        if (error == null && inCategory.isEmpty()) return Outcome.Failure(AppError.NotFound)
        return result { CategoryProducts(inCategory.first().category, inCategory.take(1), inCategory.drop(1)) }
    }

    override suspend fun search(query: String): Outcome<List<Product>> {
        searchCalls += query
        return result { products.filter { it.title.contains(query, ignoreCase = true) } }
    }

    override suspend fun product(id: String): Outcome<Product> {
        if (error == null && products.none { it.id == id }) return Outcome.Failure(AppError.NotFound)
        return result { products.first { it.id == id } }
    }

    companion object {
        private val mens = Category("mens-clothing", "Men's clothing")
        private val electronics = Category("electronics", "Electronics")

        fun product(id: String, title: String, category: Category = mens) = Product(
            id = id,
            title = title,
            description = "Description of $title",
            price = Price(1999, "USD"),
            imageUrl = "http://test/v1/products/$id/image",
            category = category,
            rating = Rating(4.0, 10),
        )

        val sampleProducts = listOf(
            product("1", "Mens Cotton Jacket"),
            product("2", "Slim Fit T-Shirt"),
            product("3", "Portable SSD", electronics),
            product("4", "Gaming Drive", electronics),
            product("5", "Casual Shirt"),
            product("6", "Curved Monitor", electronics),
        )
    }
}
