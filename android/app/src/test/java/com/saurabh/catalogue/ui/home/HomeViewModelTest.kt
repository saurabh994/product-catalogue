package com.saurabh.catalogue.ui.home

import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.usecase.GetCategoriesUseCase
import com.saurabh.catalogue.domain.usecase.GetFeaturedProductsUseCase
import com.saurabh.catalogue.fakes.FakeProductRepository
import com.saurabh.catalogue.testutil.MainDispatcherRule
import com.saurabh.catalogue.ui.common.UiState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProductRepository()

    private fun createViewModel() =
        HomeViewModel(GetFeaturedProductsUseCase(repository), GetCategoriesUseCase(repository))

    @Test
    fun `starts in Loading`() {
        assertEquals(UiState.Loading, createViewModel().uiState.value.featured)
    }

    @Test
    fun `shows the 5 featured products and the categories on success`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("1", "2", "3", "4", "5"), (state.featured as UiState.Success).data.map { it.id })
        assertEquals(listOf("mens-clothing", "electronics"), state.categories.map { it.slug })
    }

    @Test
    fun `shows the error when loading fails`() = runTest(mainDispatcherRule.dispatcher) {
        repository.error = AppError.Network
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        assertEquals(UiState.Error(AppError.Network), viewModel.uiState.value.featured)
        assertTrue("categories are optional; failure just hides them", viewModel.uiState.value.categories.isEmpty())
    }

    @Test
    fun `retry goes back to Loading and then succeeds`() = runTest(mainDispatcherRule.dispatcher) {
        repository.error = AppError.Server(null)
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.featured is UiState.Error)

        repository.error = null
        viewModel.retry()
        assertEquals(UiState.Loading, viewModel.uiState.value.featured)

        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.featured is UiState.Success)
        assertTrue(viewModel.uiState.value.categories.isNotEmpty())
    }

    @Test
    fun `an empty featured list is a success, not an error`() = runTest(mainDispatcherRule.dispatcher) {
        repository.products = emptyList()
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        assertEquals(UiState.Success(emptyList<Any>()), viewModel.uiState.value.featured)
    }
}
