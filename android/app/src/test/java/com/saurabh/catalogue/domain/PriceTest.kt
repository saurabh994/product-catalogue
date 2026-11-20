package com.saurabh.catalogue.domain

import com.saurabh.catalogue.domain.model.Price
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PriceTest {
    @Test
    fun `formats minor units with the currency's decimals`() {
        assertEquals("$55.99", Price(5599, "USD").formatted(Locale.US))
        assertEquals("$0.29", Price(29, "USD").formatted(Locale.US))
    }

    @Test
    fun `uses zero decimals for currencies that have none`() {
        assertEquals("¥500", Price(500, "JPY").formatted(Locale.US))
    }

    @Test
    fun `falls back to two decimals for an unknown currency code`() {
        // Should not crash; exact symbol depends on the locale's default currency.
        Price(1234, "???").formatted(Locale.US)
    }
}
