package com.saurabh.catalogue.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurabh.catalogue.domain.model.Product
import com.saurabh.catalogue.domain.usecase.SearchProductsUseCase
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

data class SearchUiState(
    val query: String,
    val results: UiState<List<Product>> = UiState.Loading,
)

/** Runs the search it was opened with; a new search from the results screen replaces it. */
@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchProducts: SearchProductsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState(query = savedStateHandle[RouteArgs.SEARCH_QUERY] ?: ""))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        search(_uiState.value.query)
    }

    fun search(query: String) {
        searchJob?.cancel() // the newest search wins; an older, slower response can't overwrite it
        _uiState.value = SearchUiState(query = query.trim(), results = UiState.Loading)
        searchJob = viewModelScope.launch {
            val results = searchProducts(query).toUiState()
            _uiState.value = _uiState.value.copy(results = results)
        }
    }

    fun retry() = search(_uiState.value.query)
}
