package com.saurabh.catalogue.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.saurabh.catalogue.R
import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.ui.theme.CatalogueTheme

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().testTag("loading"), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorView(error: AppError, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    MessageView(message = stringResource(error.messageRes()), modifier = modifier.testTag("error")) {
        if (onRetry != null) {
            Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}

/** Centered message, used for errors and empty states (e.g. no search results). */
@Composable
fun MessageView(message: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        modifier = modifier.fillMaxSize().padding(CatalogueTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(CatalogueTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        action()
    }
}

@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.Network -> R.string.error_network
    AppError.NotFound -> R.string.error_not_found
    AppError.RateLimited -> R.string.error_rate_limited
    is AppError.Validation -> R.string.error_validation
    is AppError.Server -> R.string.error_server
    AppError.Unexpected -> R.string.error_unexpected
}
