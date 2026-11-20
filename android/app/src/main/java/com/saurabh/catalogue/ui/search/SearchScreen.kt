package com.saurabh.catalogue.ui.search

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurabh.catalogue.R
import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.ui.common.AppTopBar
import com.saurabh.catalogue.ui.common.ErrorView
import com.saurabh.catalogue.ui.common.LoadingView
import com.saurabh.catalogue.ui.common.MessageView
import com.saurabh.catalogue.ui.common.ProductGrid
import com.saurabh.catalogue.ui.common.SearchField
import com.saurabh.catalogue.ui.common.UiState
import com.saurabh.catalogue.ui.common.fullWidthItem
import com.saurabh.catalogue.ui.common.productSection

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onProductClick: (productId: String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchContent(state, onBack, viewModel::search, viewModel::retry, onProductClick)
}

@Composable
fun SearchContent(
    state: SearchUiState,
    onBack: () -> Unit,
    onSearch: (query: String) -> Unit,
    onRetry: () -> Unit,
    onProductClick: (productId: String) -> Unit,
) {
    Scaffold(topBar = { AppTopBar(stringResource(R.string.search_title, state.query), onBack) }) { innerPadding ->
        ProductGrid(Modifier.padding(innerPadding).testTag("search_grid")) {
            // Refine the search without going back; still only runs on the button / keyboard action.
            fullWidthItem(key = "search") { SearchField(onSearch = onSearch, initialQuery = state.query) }
            when (val results = state.results) {
                UiState.Loading -> fullWidthItem(key = "loading") { LoadingView() }
                is UiState.Error ->
                    if (results.error is AppError.Validation) {
                        fullWidthItem(key = "enter-term") {
                            MessageView(stringResource(R.string.search_enter_term), Modifier.testTag("search_enter_term"))
                        }
                    } else {
                        fullWidthItem(key = "error") { ErrorView(results.error, onRetry) }
                    }
                is UiState.Success ->
                    if (results.data.isEmpty()) {
                        fullWidthItem(key = "no-results") {
                            MessageView(stringResource(R.string.search_no_results, state.query), Modifier.testTag("search_no_results"))
                        }
                    } else {
                        productSection(title = null, products = results.data, onProductClick = onProductClick, keyPrefix = "result")
                    }
            }
        }
    }
}
