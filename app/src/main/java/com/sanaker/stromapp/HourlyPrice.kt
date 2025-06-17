package com.sanaker.stromapp

import com.google.gson.annotations.SerializedName

/**
 * Dataklasse som representerer strømpris for en gitt time.
 * @property time Tidspunktet for prisen.
 * @property date Datoen for prisen.
 * @property rawPriceMva Råprisen inkludert MVA.
 * @property adjustedPriceMva justert pris (med strømstøtte) inkludert MVA.
 */
data class HourlyPrice(
    @SerializedName("time") val time: String,
    @SerializedName("date") val date: String,
    @SerializedName("raw_price_mva") val rawPriceMva: Double,
    @SerializedName("adjusted_price_mva") val adjustedPriceMva: Double
)