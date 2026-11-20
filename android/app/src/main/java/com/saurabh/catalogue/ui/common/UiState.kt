package com.saurabh.catalogue.ui.common

import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.common.Outcome

/** The one screen-state shape every ViewModel exposes. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: AppError) : UiState<Nothing>
}

fun <T> Outcome<T>.toUiState(): UiState<T> = when (this) {
    is Outcome.Success -> UiState.Success(value)
    is Outcome.Failure -> UiState.Error(error)
}
