package com.saurabh.catalogue.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurabh.catalogue.domain.model.Product
import com.saurabh.catalogue.domain.usecase.GetFeaturedProductsUseCase
import com.saurabh.catalogue.ui.common.UiState
import com.saurabh.catalogue.ui.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Home: loads the business-chosen top-rated products once, with retry on failure. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getFeaturedProducts: GetFeaturedProductsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Product>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Product>>> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        loadJob?.cancel() // a retry replaces any in-flight load instead of racing it
        _uiState.value = UiState.Loading
        loadJob = viewModelScope.launch {
            _uiState.value = getFeaturedProducts().toUiState()
        }
    }
}
