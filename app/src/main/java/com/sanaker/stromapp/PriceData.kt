// app/src/main/java/com/sanaker/stromapp/PriceData.kt
package com.sanaker.stromapp // Pass på at dette er riktig pakkenavn for appen din

import kotlinx.serialization.Serializable // Viktig import!

@Serializable
data class PriceData(
    val time: String,
    val price: Double,
    val image: String?
)