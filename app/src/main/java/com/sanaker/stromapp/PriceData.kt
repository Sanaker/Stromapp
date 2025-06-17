package com.sanaker.stromapp // Or your correct package name

import kotlinx.serialization.Serializable

@Serializable // Make sure this is present
data class PriceData(
    val time: String, // Expected format e.g., "yyyy-MM-dd HH:mm"
    val price: Double,
    val image: String? = null // Optional Base64 image
)

// You could add another data class here if needed, for example,
// if your API returns a list of these under a different structure:
@Serializable
data class ApiResponse(
    val prices: List<PriceData>
    // other fields if your API returns more than just a list of prices
)