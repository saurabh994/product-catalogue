package com.saurabh.catalogue.ui.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurabh.catalogue.domain.model.Product
import com.saurabh.catalogue.domain.usecase.GetProductUseCase
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
class ProductViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProduct: GetProductUseCase,
) : ViewModel() {

    private val productId: String = checkNotNull(savedStateHandle[RouteArgs.PRODUCT_ID]) { "Missing product id" }

    private val _uiState = MutableStateFlow<UiState<Product>>(UiState.Loading)
    val uiState: StateFlow<UiState<Product>> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        loadJob?.cancel()
        _uiState.value = UiState.Loading
        loadJob = viewModelScope.launch { _uiState.value = getProduct(productId).toUiState() }
    }
}
