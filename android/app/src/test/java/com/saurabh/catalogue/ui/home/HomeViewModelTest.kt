package com.saurabh.catalogue.ui.home

import com.saurabh.catalogue.domain.common.AppError
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

    private fun createViewModel() = HomeViewModel(GetFeaturedProductsUseCase(repository))

    @Test
    fun `starts in Loading`() {
        assertEquals(UiState.Loading, createViewModel().uiState.value)
    }

    @Test
    fun `shows the 5 featured products on success`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertEquals(listOf("1", "2", "3", "4", "5"), state.data.map { it.id })
    }

    @Test
    fun `shows the error when loading fails`() = runTest(mainDispatcherRule.dispatcher) {
        repository.error = AppError.Network
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        assertEquals(UiState.Error(AppError.Network), viewModel.uiState.value)
    }

    @Test
    fun `retry goes back to Loading and then succeeds`() = runTest(mainDispatcherRule.dispatcher) {
        repository.error = AppError.Server(null)
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Error)

        repository.error = null
        viewModel.retry()
        assertEquals(UiState.Loading, viewModel.uiState.value)

        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)
    }

    @Test
    fun `an empty featured list is a success, not an error`() = runTest(mainDispatcherRule.dispatcher) {
        repository.products = emptyList()
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        assertEquals(UiState.Success(emptyList<Any>()), viewModel.uiState.value)
    }
}
