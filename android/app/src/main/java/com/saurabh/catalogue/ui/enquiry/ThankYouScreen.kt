package com.saurabh.catalogue.ui.enquiry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.saurabh.catalogue.R
import com.saurabh.catalogue.ui.theme.CatalogueTheme

/** Shown only after the backend confirmed the sales email was sent. */
@Composable
fun ThankYouScreen(productTitle: String, onDone: () -> Unit) {
    val spacing = CatalogueTheme.spacing
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(spacing.lg).testTag("thank_you"),
            verticalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.thank_you_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.thank_you_body, productTitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onDone, modifier = Modifier.testTag("thank_you_done")) {
                Text(stringResource(R.string.thank_you_done))
            }
        }
    }
}
