package com.saurabh.catalogue.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.saurabh.catalogue.R
import com.saurabh.catalogue.domain.model.Category
import com.saurabh.catalogue.domain.model.Price
import com.saurabh.catalogue.domain.model.Product
import com.saurabh.catalogue.domain.model.Rating
import com.saurabh.catalogue.ui.theme.CatalogueTheme

/** Product card used on Home, Category and Search. Tapping opens the product page. */
@Composable
fun ProductTile(product: Product, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = CatalogueTheme.spacing
    Card(onClick = onClick, modifier = modifier.testTag("product_tile_${product.id}")) {
        // Product photos have white backgrounds, so keep the image area white in dark mode too.
        AsyncImage(
            model = product.imageUrl,
            contentDescription = null, // the title below describes the tile
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.White)
                .padding(spacing.sm),
        )
        Column(Modifier.padding(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.titleSmall,
                // Always reserve two lines so tiles in a row are the same height and prices line up.
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = product.price.formatted(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            RatingRow(product.rating)
        }
    }
}

@Composable
fun RatingRow(rating: Rating, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // Plain glyph rather than an icon library: newer Material3 no longer bundles the icons artifact.
        Text(text = "★", color = CatalogueTheme.extendedColors.rating, style = MaterialTheme.typography.labelMedium)
        Text(
            text = stringResource(R.string.rating_format, rating.average, rating.count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = CatalogueTheme.spacing.xs),
        )
    }
}

@Preview(widthDp = 180)
@Composable
private fun ProductTilePreview() {
    CatalogueTheme {
        ProductTile(
            product = Product(
                id = "3",
                title = "Mens Cotton Jacket",
                description = "",
                price = Price(5599, "USD"),
                imageUrl = "",
                category = Category("mens-clothing", "Men's clothing"),
                rating = Rating(4.7, 500),
            ),
            onClick = {},
        )
    }
}
