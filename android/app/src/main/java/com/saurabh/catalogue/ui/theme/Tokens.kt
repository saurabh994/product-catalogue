package com.saurabh.catalogue.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale. Screens use CatalogueTheme.spacing.md etc., never raw dp values. */
@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
)

/** Non-Material tokens the brand needs. */
@Immutable
data class ExtendedColors(
    val rating: androidx.compose.ui.graphics.Color,
)

internal val LocalSpacing = staticCompositionLocalOf { Spacing() }
internal val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors(rating = BrandColors.Rating) }

internal val CatalogueShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)
