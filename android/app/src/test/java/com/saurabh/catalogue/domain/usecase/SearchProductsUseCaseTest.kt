package com.saurabh.catalogue.domain.usecase

import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.common.Outcome
import com.saurabh.catalogue.fakes.FakeProductRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchProductsUseCaseTest {
    private val repository = FakeProductRepository()
    private val search = SearchProductsUseCase(repository)

    @Test
    fun `trims the term before searching`() = runTest {
        search("  jacket  ")
        assertEquals(listOf("jacket"), repository.searchCalls)
    }

    @Test
    fun `returns matching products`() = runTest {
        val result = search("jacket") as Outcome.Success
        assertEquals(listOf("1"), result.value.map { it.id })
    }

    @Test
    fun `blank term is rejected without a network call`() = runTest {
        val result = search("   ")
        assertTrue(result is Outcome.Failure && result.error is AppError.Validation)
        assertTrue(repository.searchCalls.isEmpty())
    }

    @Test
    fun `over-long term is rejected without a network call`() = runTest {
        val result = search("x".repeat(SearchProductsUseCase.MAX_LENGTH + 1))
        assertTrue(result is Outcome.Failure && result.error is AppError.Validation)
        assertTrue(repository.searchCalls.isEmpty())
    }

    @Test
    fun `repository errors pass through`() = runTest {
        repository.error = AppError.Network
        assertEquals(Outcome.Failure(AppError.Network), search("jacket"))
    }
}
