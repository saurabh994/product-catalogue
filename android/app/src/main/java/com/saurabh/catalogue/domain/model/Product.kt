package com.saurabh.catalogue.domain.model

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** The app's own product model. API DTOs are mapped into this in the data layer. */
data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: Price,
    /** Absolute URL, already resolved against the API base URL. */
    val imageUrl: String,
    val category: Category,
    val rating: Rating,
)

data class Category(val slug: String, val name: String)

data class Rating(val average: Double, val count: Int)

/** Money in minor units (cents) — never a Double — formatted only at the edge. */
data class Price(val amountMinor: Long, val currencyCode: String) {
    fun formatted(locale: Locale = Locale.getDefault()): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        val currency = runCatching { Currency.getInstance(currencyCode) }.getOrNull()
        val digits = currency?.defaultFractionDigits?.takeIf { it >= 0 } ?: 2
        currency?.let { format.currency = it }
        format.minimumFractionDigits = digits
        format.maximumFractionDigits = digits
        return format.format(BigDecimal.valueOf(amountMinor, digits))
    }
}

/** Category page: business-chosen "top rated" plus the rest of the category. */
data class CategoryProducts(
    val category: Category,
    val topRated: List<Product>,
    val others: List<Product>,
)
