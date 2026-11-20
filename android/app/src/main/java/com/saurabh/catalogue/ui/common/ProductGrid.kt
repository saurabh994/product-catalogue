package com.saurabh.catalogue.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.saurabh.catalogue.domain.model.Product
import com.saurabh.catalogue.ui.theme.CatalogueTheme

/** Adaptive product grid used by Home, Category and Search: 2 columns on a phone, more on larger screens. */
@Composable
fun ProductGrid(modifier: Modifier = Modifier, content: LazyGridScope.() -> Unit) {
    val spacing = CatalogueTheme.spacing
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        content = content,
    )
}

/** A full-width item (search bar, chips, headings, inline states). */
fun LazyGridScope.fullWidthItem(key: Any? = null, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

/** Heading + tiles. Keys are prefixed per section so the same product can't clash across sections. */
fun LazyGridScope.productSection(
    title: String?,
    products: List<Product>,
    onProductClick: (productId: String) -> Unit,
    keyPrefix: String,
) {
    if (title != null) {
        fullWidthItem(key = "$keyPrefix-heading") { SectionHeading(title) }
    }
    items(products, key = { "$keyPrefix-${it.id}" }) { product ->
        ProductTile(product = product, onClick = { onProductClick(product.id) })
    }
}

@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
            .padding(top = CatalogueTheme.spacing.sm)
            .semantics { heading() },
    )
}
