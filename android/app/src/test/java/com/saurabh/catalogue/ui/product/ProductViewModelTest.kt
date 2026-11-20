package com.saurabh.catalogue.ui.product

import androidx.lifecycle.SavedStateHandle
import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.usecase.GetProductUseCase
import com.saurabh.catalogue.fakes.FakeProductRepository
import com.saurabh.catalogue.navigation.RouteArgs
import com.saurabh.catalogue.testutil.MainDispatcherRule
import com.saurabh.catalogue.ui.common.UiState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProductViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProductRepository()

    private fun createViewModel(id: String) =
        ProductViewModel(SavedStateHandle(mapOf(RouteArgs.PRODUCT_ID to id)), GetProductUseCase(repository))

    @Test
    fun `loads the product from the route id`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel("3")
        assertEquals(UiState.Loading, viewModel.uiState.value)

        testScheduler.advanceUntilIdle()

        val product = (viewModel.uiState.value as UiState.Success).data
        assertEquals("3", product.id)
        assertEquals("Portable SSD", product.title)
    }

    @Test
    fun `unknown product shows NotFound`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel("999")
        testScheduler.advanceUntilIdle()

        assertEquals(UiState.Error(AppError.NotFound), viewModel.uiState.value)
    }

    @Test
    fun `retry recovers after a network error`() = runTest(mainDispatcherRule.dispatcher) {
        repository.error = AppError.Network
        val viewModel = createViewModel("1")
        testScheduler.advanceUntilIdle()
        assertEquals(UiState.Error(AppError.Network), viewModel.uiState.value)

        repository.error = null
        viewModel.retry()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Success)
    }
}
