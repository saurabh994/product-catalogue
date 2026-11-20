package com.saurabh.catalogue.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurabh.catalogue.R
import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.model.Category
import com.saurabh.catalogue.domain.model.Price
import com.saurabh.catalogue.domain.model.Product
import com.saurabh.catalogue.domain.model.Rating
import com.saurabh.catalogue.ui.common.AppTopBar
import com.saurabh.catalogue.ui.common.ErrorView
import com.saurabh.catalogue.ui.common.LoadingView
import com.saurabh.catalogue.ui.common.MessageView
import com.saurabh.catalogue.ui.common.ProductGrid
import com.saurabh.catalogue.ui.common.SearchField
import com.saurabh.catalogue.ui.common.UiState
import com.saurabh.catalogue.ui.common.fullWidthItem
import com.saurabh.catalogue.ui.common.productSection
import com.saurabh.catalogue.ui.theme.CatalogueTheme

/** Stateful entry point used by navigation: wires the ViewModel to the stateless screen. */
@Composable
fun HomeScreen(
    onProductClick: (productId: String) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onSearch: (query: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state, viewModel::retry, onProductClick, onCategoryClick, onSearch)
}

/** Stateless: everything it shows comes from [state], so it's easy to preview and UI-test. */
@Composable
fun HomeContent(
    state: HomeUiState,
    onRetry: () -> Unit,
    onProductClick: (productId: String) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onSearch: (query: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, topBar = { AppTopBar(stringResource(R.string.app_name)) }) { innerPadding ->
        // Search and categories stay usable even while featured products load or fail.
        ProductGrid(Modifier.padding(innerPadding).testTag("home_grid")) {
            fullWidthItem(key = "search") { SearchField(onSearch = onSearch) }
            if (state.categories.isNotEmpty()) {
                fullWidthItem(key = "categories") { CategoryChips(state.categories, onCategoryClick) }
            }
            when (val featured = state.featured) {
                UiState.Loading -> fullWidthItem(key = "loading") { LoadingView() }
                is UiState.Error -> fullWidthItem(key = "error") { ErrorView(featured.error, onRetry = onRetry) }
                is UiState.Success ->
                    if (featured.data.isEmpty()) {
                        fullWidthItem(key = "empty") {
                            MessageView(stringResource(R.string.home_empty), Modifier.testTag("home_empty"))
                        }
                    } else {
                        productSection(stringResource(R.string.home_top_rated), featured.data, onProductClick, "featured")
                    }
            }
        }
    }
}

@Composable
private fun CategoryChips(categories: List<Category>, onCategoryClick: (Category) -> Unit) {
    val label = stringResource(R.string.categories_label)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(CatalogueTheme.spacing.sm),
        modifier = Modifier.semantics { contentDescription = label },
    ) {
        items(categories, key = { it.slug }) { category ->
            AssistChip(
                onClick = { onCategoryClick(category) },
                label = { Text(category.name) },
                modifier = Modifier.testTag("category_${category.slug}"),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    val electronics = Category("electronics", "Electronics")
    val sample = (1..5).map {
        Product("$it", "Sample product $it", "", Price(1999L * it, "USD"), "", electronics, Rating(4.0 + it / 10.0, 100 * it))
    }
    CatalogueTheme {
        HomeContent(
            state = HomeUiState(UiState.Success(sample), listOf(electronics, Category("jewelery", "Jewelery"))),
            onRetry = {}, onProductClick = {}, onCategoryClick = {}, onSearch = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeErrorPreview() {
    CatalogueTheme {
        HomeContent(HomeUiState(UiState.Error(AppError.Network)), {}, {}, {}, {})
    }
}
