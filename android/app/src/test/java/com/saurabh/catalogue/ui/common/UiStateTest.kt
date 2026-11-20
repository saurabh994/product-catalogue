package com.saurabh.catalogue.ui.common

import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.common.Outcome
import com.saurabh.catalogue.domain.common.map
import org.junit.Assert.assertEquals
import org.junit.Test

class UiStateTest {
    @Test
    fun `success and failure outcomes map to UiState`() {
        assertEquals(UiState.Success(1), Outcome.Success(1).toUiState())
        assertEquals(UiState.Error(AppError.Network), Outcome.Failure(AppError.Network).toUiState())
    }

    @Test
    fun `map transforms success and passes failure through`() {
        assertEquals(Outcome.Success(2), Outcome.Success(1).map { it + 1 })
        val failure: Outcome<Int> = Outcome.Failure(AppError.NotFound)
        assertEquals(failure, failure.map { it + 1 })
    }
}
