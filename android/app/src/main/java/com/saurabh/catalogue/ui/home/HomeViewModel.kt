package com.saurabh.catalogue.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurabh.catalogue.domain.common.Outcome
import com.saurabh.catalogue.domain.model.Category
import com.saurabh.catalogue.domain.model.Product
import com.saurabh.catalogue.domain.usecase.GetCategoriesUseCase
import com.saurabh.catalogue.domain.usecase.GetFeaturedProductsUseCase
import com.saurabh.catalogue.ui.common.UiState
import com.saurabh.catalogue.ui.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val featured: UiState<List<Product>> = UiState.Loading,
    /** Category links are secondary: if they fail to load, Home still works without them. */
    val categories: List<Category> = emptyList(),
)

/** Home: featured (top-rated) products plus category links, loaded in parallel. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getFeaturedProducts: GetFeaturedProductsUseCase,
    private val getCategories: GetCategoriesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        loadJob?.cancel() // a retry replaces any in-flight load instead of racing it
        _uiState.update { it.copy(featured = UiState.Loading) }
        loadJob = viewModelScope.launch {
            launch { _uiState.update { it.copy(featured = getFeaturedProducts().toUiState()) } }
            launch {
                val categories = (getCategories() as? Outcome.Success)?.value
                if (categories != null) _uiState.update { it.copy(categories = categories) }
            }
        }
    }
}
