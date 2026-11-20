package com.saurabh.catalogue.ui.search

import androidx.lifecycle.SavedStateHandle
import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.usecase.SearchProductsUseCase
import com.saurabh.catalogue.fakes.FakeProductRepository
import com.saurabh.catalogue.navigation.RouteArgs
import com.saurabh.catalogue.testutil.MainDispatcherRule
import com.saurabh.catalogue.ui.common.UiState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProductRepository()

    private fun createViewModel(query: String) =
        SearchViewModel(SavedStateHandle(mapOf(RouteArgs.SEARCH_QUERY to query)), SearchProductsUseCase(repository))

    @Test
    fun `runs the search it was opened with`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel("jacket")
        assertEquals(UiState.Loading, viewModel.uiState.value.results)

        testScheduler.advanceUntilIdle()

        assertEquals("jacket", viewModel.uiState.value.query)
        assertEquals(listOf("1"), (viewModel.uiState.value.results as UiState.Success).data.map { it.id })
    }

    @Test
    fun `no matches is an empty success, not an error`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel("zzz")
        testScheduler.advanceUntilIdle()

        assertEquals(UiState.Success(emptyList<Any>()), viewModel.uiState.value.results)
    }

    @Test
    fun `a blank term is a validation error and never hits the repository`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel("   ")
        testScheduler.advanceUntilIdle()

        val results = viewModel.uiState.value.results
        assertTrue(results is UiState.Error && results.error is AppError.Validation)
        assertTrue(repository.searchCalls.isEmpty())
    }

    @Test
    fun `a new search replaces the previous one`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel("jacket")
        testScheduler.advanceUntilIdle()

        viewModel.search("  drive ")
        assertEquals("drive", viewModel.uiState.value.query)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("4"), (viewModel.uiState.value.results as UiState.Success).data.map { it.id })
    }

    @Test
    fun `retry repeats the current search after an error`() = runTest(mainDispatcherRule.dispatcher) {
        repository.error = AppError.Network
        val viewModel = createViewModel("jacket")
        testScheduler.advanceUntilIdle()
        assertEquals(UiState.Error(AppError.Network), viewModel.uiState.value.results)

        repository.error = null
        viewModel.retry()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.results is UiState.Success)
        assertEquals(listOf("jacket", "jacket"), repository.searchCalls)
    }
}
