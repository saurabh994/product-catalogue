package com.saurabh.catalogue.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurabh.catalogue.R
import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.model.Category
import com.saurabh.catalogue.domain.model.Price
import com.saurabh.catalogue.domain.model.Product
import com.saurabh.catalogue.domain.model.Rating
import com.saurabh.catalogue.ui.common.ErrorView
import com.saurabh.catalogue.ui.common.LoadingView
import com.saurabh.catalogue.ui.common.MessageView
import com.saurabh.catalogue.ui.common.ProductTile
import com.saurabh.catalogue.ui.common.UiState
import com.saurabh.catalogue.ui.theme.CatalogueTheme

/** Stateful entry point used by navigation: wires the ViewModel to the stateless screen. */
@Composable
fun HomeScreen(
    onProductClick: (productId: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state = state, onRetry = viewModel::retry, onProductClick = onProductClick)
}

/** Stateless: everything it shows comes from [state], so it's easy to preview and UI-test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: UiState<List<Product>>,
    onRetry: () -> Unit,
    onProductClick: (productId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (state) {
            UiState.Loading -> LoadingView(contentModifier)
            is UiState.Error -> ErrorView(state.error, onRetry = onRetry, modifier = contentModifier)
            is UiState.Success ->
                if (state.data.isEmpty()) {
                    MessageView(stringResource(R.string.home_empty), contentModifier.testTag("home_empty"))
                } else {
                    FeaturedGrid(state.data, onProductClick, contentModifier)
                }
        }
    }
}

@Composable
private fun FeaturedGrid(
    products: List<Product>,
    onProductClick: (productId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = CatalogueTheme.spacing
    LazyVerticalGrid(
        // Adaptive: 2 columns on a phone, more on tablets/landscape — no breakpoint code needed.
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier.fillMaxSize().testTag("featured_grid"),
        contentPadding = PaddingValues(spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.home_top_rated),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
        items(products, key = { it.id }) { product ->
            ProductTile(product = product, onClick = { onProductClick(product.id) })
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    val sample = (1..5).map {
        Product(
            id = "$it",
            title = "Sample product $it",
            description = "",
            price = Price(1999L * it, "USD"),
            imageUrl = "",
            category = Category("electronics", "Electronics"),
            rating = Rating(4.0 + it / 10.0, 100 * it),
        )
    }
    CatalogueTheme { HomeContent(UiState.Success(sample), onRetry = {}, onProductClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun HomeErrorPreview() {
    CatalogueTheme { HomeContent(UiState.Error(AppError.Network), onRetry = {}, onProductClick = {}) }
}
