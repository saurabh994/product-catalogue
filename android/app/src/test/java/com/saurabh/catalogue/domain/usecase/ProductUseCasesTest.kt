package com.saurabh.catalogue.domain.usecase

import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.common.Outcome
import com.saurabh.catalogue.fakes.FakeProductRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductUseCasesTest {
    private val repository = FakeProductRepository()

    @Test
    fun `featured returns the repository's featured products`() = runTest {
        val result = GetFeaturedProductsUseCase(repository)() as Outcome.Success
        assertEquals(5, result.value.size)
    }

    @Test
    fun `product returns NotFound for an unknown id`() = runTest {
        assertEquals(Outcome.Failure(AppError.NotFound), GetProductUseCase(repository)("999"))
    }

    @Test
    fun `category products are split into top rated and others`() = runTest {
        val page = (GetCategoryProductsUseCase(repository)("electronics") as Outcome.Success).value
        assertEquals(listOf("3"), page.topRated.map { it.id })
        assertEquals(listOf("4", "6"), page.others.map { it.id })
    }

    @Test
    fun `categories are distinct`() = runTest {
        val categories = (GetCategoriesUseCase(repository)() as Outcome.Success).value
        assertEquals(listOf("mens-clothing", "electronics"), categories.map { it.slug })
    }
}
