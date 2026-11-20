package com.saurabh.catalogue.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurabh.catalogue.domain.model.CategoryProducts
import com.saurabh.catalogue.domain.usecase.GetCategoryProductsUseCase
import com.saurabh.catalogue.navigation.RouteArgs
import com.saurabh.catalogue.ui.common.UiState
import com.saurabh.catalogue.ui.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCategoryProducts: GetCategoryProductsUseCase,
) : ViewModel() {

    private val slug: String = checkNotNull(savedStateHandle[RouteArgs.CATEGORY_SLUG]) { "Missing category slug" }

    /** Shown in the top bar immediately, before the products load. */
    val categoryName: String = savedStateHandle[RouteArgs.CATEGORY_NAME] ?: slug

    private val _uiState = MutableStateFlow<UiState<CategoryProducts>>(UiState.Loading)
    val uiState: StateFlow<UiState<CategoryProducts>> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        loadJob?.cancel()
        _uiState.value = UiState.Loading
        loadJob = viewModelScope.launch { _uiState.value = getCategoryProducts(slug).toUiState() }
    }
}
