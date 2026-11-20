package com.saurabh.catalogue.data.mapper

import com.saurabh.catalogue.data.remote.dto.CategoryDto
import com.saurabh.catalogue.data.remote.dto.CategoryProductsResponse
import com.saurabh.catalogue.data.remote.dto.ProductDto
import com.saurabh.catalogue.domain.model.Category
import com.saurabh.catalogue.domain.model.CategoryProducts
import com.saurabh.catalogue.domain.model.Price
import com.saurabh.catalogue.domain.model.Product
import com.saurabh.catalogue.domain.model.Rating
import okhttp3.HttpUrl

fun CategoryDto.toDomain() = Category(slug = slug, name = name)

/** The backend returns image URLs relative to itself (e.g. /v1/products/3/image); resolve them here. */
fun ProductDto.toDomain(baseUrl: HttpUrl) = Product(
    id = id,
    title = title,
    description = description,
    price = Price(amountMinor = price.amount, currencyCode = price.currency),
    imageUrl = baseUrl.resolve(imageUrl)?.toString() ?: imageUrl,
    category = category.toDomain(),
    rating = Rating(average = rating.average, count = rating.count),
)

fun CategoryProductsResponse.toDomain(baseUrl: HttpUrl) = CategoryProducts(
    category = category.toDomain(),
    topRated = topRated.map { it.toDomain(baseUrl) },
    others = others.map { it.toDomain(baseUrl) },
)
