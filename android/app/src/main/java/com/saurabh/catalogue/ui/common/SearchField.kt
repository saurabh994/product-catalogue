package com.saurabh.catalogue.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.saurabh.catalogue.R

/**
 * Search bar. Per the acceptance criterion, a search only runs when the search button (or the keyboard's
 * search action) is pressed — not as you type. The button is disabled while the term is blank.
 */
@Composable
fun SearchField(
    onSearch: (query: String) -> Unit,
    modifier: Modifier = Modifier,
    initialQuery: String = "",
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    val keyboard = LocalSoftwareKeyboardController.current
    val canSearch = query.isNotBlank()
    val submit = {
        if (canSearch) {
            keyboard?.hide()
            onSearch(query.trim())
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = modifier.fillMaxWidth().testTag("search_field"),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { submit() }),
        trailingIcon = {
            IconButton(onClick = submit, enabled = canSearch, modifier = Modifier.testTag("search_button")) {
                Icon(painterResource(R.drawable.ic_search), contentDescription = stringResource(R.string.search_action))
            }
        },
    )
}
