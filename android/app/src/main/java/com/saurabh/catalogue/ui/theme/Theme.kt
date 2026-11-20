package com.saurabh.catalogue.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val LightColors = lightColorScheme(
    primary = BrandColors.Primary,
    onPrimary = BrandColors.OnPrimary,
    primaryContainer = BrandColors.PrimaryContainer,
    onPrimaryContainer = BrandColors.OnPrimaryContainer,
    secondary = BrandColors.Secondary,
    onSecondary = BrandColors.OnSecondary,
    background = BrandColors.Background,
    surface = BrandColors.Surface,
    onSurface = BrandColors.OnSurface,
    onSurfaceVariant = BrandColors.OnSurfaceVariant,
    outline = BrandColors.Outline,
    error = BrandColors.Error,
)

private val DarkColors = darkColorScheme(
    primary = BrandColors.PrimaryDark,
    onPrimary = BrandColors.OnPrimaryDark,
    primaryContainer = BrandColors.PrimaryContainerDark,
    onPrimaryContainer = BrandColors.OnPrimaryContainerDark,
    secondary = BrandColors.SecondaryDark,
    background = BrandColors.BackgroundDark,
    surface = BrandColors.SurfaceDark,
    onSurface = BrandColors.OnSurfaceDark,
    onSurfaceVariant = BrandColors.OnSurfaceVariantDark,
    outline = BrandColors.OutlineDark,
    error = BrandColors.ErrorDark,
)

/**
 * App theme built only from tokens. Dynamic (wallpaper) colour is deliberately off:
 * a retail brand needs consistent colours, and the agency's look must apply everywhere.
 */
@Composable
fun CatalogueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalExtendedColors provides ExtendedColors(rating = BrandColors.Rating),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            shapes = CatalogueShapes,
            content = content,
        )
    }
}

/** Access to the non-Material tokens, mirroring MaterialTheme.colorScheme etc. */
object CatalogueTheme {
    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current

    val extendedColors: ExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current
}
