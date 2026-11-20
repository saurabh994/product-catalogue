package com.saurabh.catalogue.data.remote.dto

import kotlinx.serialization.Serializable

// Wire shapes of our backend API (see DECISIONS.md §3). Never used directly by the UI.

@Serializable
data class PriceDto(val amount: Long, val currency: String)

@Serializable
data class CategoryDto(val slug: String, val name: String)

@Serializable
data class RatingDto(val average: Double, val count: Int)

@Serializable
data class ProductDto(
    val id: String,
    val title: String,
    val description: String,
    val price: PriceDto,
    val imageUrl: String,
    val category: CategoryDto,
    val rating: RatingDto,
)

@Serializable
data class ProductsResponse(val products: List<ProductDto>)

@Serializable
data class ProductResponse(val product: ProductDto)

@Serializable
data class SearchResponse(val query: String, val products: List<ProductDto>)

@Serializable
data class CategoriesResponse(val categories: List<CategoryDto>)

@Serializable
data class CategoryProductsResponse(
    val category: CategoryDto,
    val topRated: List<ProductDto>,
    val others: List<ProductDto>,
)

@Serializable
data class EnquiryRequestDto(
    val productId: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val message: String? = null,
)

@Serializable
data class EnquiryResponseDto(val enquiryId: String, val status: String)

@Serializable
data class ErrorResponseDto(val error: ErrorBodyDto)

@Serializable
data class ErrorBodyDto(
    val code: String,
    val message: String,
    val details: List<FieldErrorDto> = emptyList(),
)

@Serializable
data class FieldErrorDto(val field: String, val message: String)
