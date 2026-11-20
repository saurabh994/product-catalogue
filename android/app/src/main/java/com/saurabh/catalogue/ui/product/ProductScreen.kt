package com.saurabh.catalogue.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.saurabh.catalogue.R
import com.saurabh.catalogue.domain.model.Category
import com.saurabh.catalogue.domain.model.Price
import com.saurabh.catalogue.domain.model.Product
import com.saurabh.catalogue.domain.model.Rating
import com.saurabh.catalogue.ui.common.AppTopBar
import com.saurabh.catalogue.ui.common.ErrorView
import com.saurabh.catalogue.ui.common.LoadingView
import com.saurabh.catalogue.ui.common.RatingRow
import com.saurabh.catalogue.ui.common.UiState
import com.saurabh.catalogue.ui.theme.CatalogueTheme

@Composable
fun ProductScreen(
    onBack: () -> Unit,
    onEnquire: (productId: String, productTitle: String) -> Unit,
    viewModel: ProductViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProductContent(state, onBack, viewModel::retry, onEnquire)
}

@Composable
fun ProductContent(
    state: UiState<Product>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEnquire: (productId: String, productTitle: String) -> Unit,
) {
    val title = (state as? UiState.Success)?.data?.title ?: stringResource(R.string.product_title)
    Scaffold(
        topBar = { AppTopBar(title, onBack) },
        bottomBar = {
            if (state is UiState.Success) {
                // Pinned so the call to action is always visible, however long the description.
                Surface(tonalElevation = CatalogueTheme.spacing.xs) {
                    Button(
                        onClick = { onEnquire(state.data.id, state.data.title) },
                        modifier = Modifier.fillMaxWidth().padding(CatalogueTheme.spacing.md).testTag("enquire_button"),
                    ) { Text(stringResource(R.string.product_enquire)) }
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (state) {
            UiState.Loading -> LoadingView(contentModifier)
            is UiState.Error -> ErrorView(state.error, onRetry, contentModifier)
            is UiState.Success -> ProductDetails(state.data, contentModifier)
        }
    }
}

@Composable
private fun ProductDetails(product: Product, modifier: Modifier = Modifier) {
    val spacing = CatalogueTheme.spacing
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("product_details"),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        AsyncImage(
            model = product.imageUrl,
            contentDescription = product.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color.White).padding(spacing.lg),
        )
        Column(Modifier.padding(horizontal = spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(product.category.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(product.title, style = MaterialTheme.typography.headlineSmall)
            Text(product.price.formatted(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            RatingRow(product.rating)
            Text(product.description, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = spacing.md))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductContentPreview() {
    CatalogueTheme {
        ProductContent(
            UiState.Success(
                Product("3", "Mens Cotton Jacket", "Great outerwear jackets for spring, autumn and winter.",
                    Price(5599, "USD"), "", Category("mens-clothing", "Men's clothing"), Rating(4.7, 500)),
            ),
            onBack = {}, onRetry = {}, onEnquire = { _, _ -> },
        )
    }
}
