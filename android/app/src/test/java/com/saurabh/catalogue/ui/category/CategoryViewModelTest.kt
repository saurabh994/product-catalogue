package com.saurabh.catalogue.ui.category

import androidx.lifecycle.SavedStateHandle
import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.usecase.GetCategoryProductsUseCase
import com.saurabh.catalogue.fakes.FakeProductRepository
import com.saurabh.catalogue.navigation.RouteArgs
import com.saurabh.catalogue.testutil.MainDispatcherRule
import com.saurabh.catalogue.ui.common.UiState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CategoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProductRepository()

    private fun createViewModel(slug: String, name: String = "Electronics") = CategoryViewModel(
        SavedStateHandle(mapOf(RouteArgs.CATEGORY_SLUG to slug, RouteArgs.CATEGORY_NAME to name)),
        GetCategoryProductsUseCase(repository),
    )

    @Test
    fun `title is available before products load`() {
        assertEquals("Electronics", createViewModel("electronics").categoryName)
    }

    @Test
    fun `shows top rated and other products for the category`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel("electronics")
        testScheduler.advanceUntilIdle()

        val page = (viewModel.uiState.value as UiState.Success).data
        assertEquals(listOf("3"), page.topRated.map { it.id })
        assertEquals(listOf("4", "6"), page.others.map { it.id })
        assertTrue(page.others.none { other -> page.topRated.any { it.id == other.id } })
    }

    @Test
    fun `unknown category shows NotFound`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel("nope", "Nope")
        testScheduler.advanceUntilIdle()

        assertEquals(UiState.Error(AppError.NotFound), viewModel.uiState.value)
    }

    @Test
    fun `retry recovers after an error`() = runTest(mainDispatcherRule.dispatcher) {
        repository.error = AppError.Network
        val viewModel = createViewModel("electronics")
        testScheduler.advanceUntilIdle()

        repository.error = null
        viewModel.retry()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Success)
    }
}
