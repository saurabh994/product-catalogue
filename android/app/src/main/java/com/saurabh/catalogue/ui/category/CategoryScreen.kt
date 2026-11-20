package com.saurabh.catalogue.ui.category

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurabh.catalogue.R
import com.saurabh.catalogue.domain.model.CategoryProducts
import com.saurabh.catalogue.ui.common.AppTopBar
import com.saurabh.catalogue.ui.common.ErrorView
import com.saurabh.catalogue.ui.common.LoadingView
import com.saurabh.catalogue.ui.common.ProductGrid
import com.saurabh.catalogue.ui.common.UiState
import com.saurabh.catalogue.ui.common.fullWidthItem
import com.saurabh.catalogue.ui.common.productSection

@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    onProductClick: (productId: String) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoryContent(viewModel.categoryName, state, onBack, viewModel::retry, onProductClick)
}

/** "Top rated" (business-chosen, or highest-rated fallback — decided by the backend) then "Other products". */
@Composable
fun CategoryContent(
    categoryName: String,
    state: UiState<CategoryProducts>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onProductClick: (productId: String) -> Unit,
) {
    Scaffold(topBar = { AppTopBar(categoryName, onBack) }) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (state) {
            UiState.Loading -> LoadingView(contentModifier)
            is UiState.Error -> ErrorView(state.error, onRetry, contentModifier)
            is UiState.Success -> {
                // Resolve strings here: the grid's item builder below is not a @Composable context.
                val topRatedTitle = stringResource(R.string.category_top_rated)
                val othersTitle = stringResource(R.string.category_others)
                ProductGrid(contentModifier.testTag("category_grid")) {
                productSection(topRatedTitle, state.data.topRated, onProductClick, "top")
                if (state.data.others.isEmpty()) {
                    fullWidthItem(key = "others-empty") {
                        Text(
                            stringResource(R.string.category_others_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    productSection(othersTitle, state.data.others, onProductClick, "others")
                }
                }
            }
        }
    }
}
