package com.sanaker.stromapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class PriceAdapter(private val prices: List<HourlyPrice>) :
    RecyclerView.Adapter<PriceAdapter.PriceViewHolder>() {

    // Definer tersklene her for fargekoding
    private val LOW_THRESHOLD = 0.20
    private val HIGH_THRESHOLD = 0.60

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_price, parent, false) // item_price.xml skal vi lage snart
        return PriceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PriceViewHolder, position: Int) {
        val currentPrice = prices[position]
        holder.bind(currentPrice)

        // Fargekode basert på justert pris
        val colorResId = when {
            currentPrice.adjustedPriceMva <= LOW_THRESHOLD -> R.color.good_price // Updated from blue_low_price
            currentPrice.adjustedPriceMva >= HIGH_THRESHOLD -> R.color.bad_price // Updated from red_high_price
            else -> R.color.normal_price // Updated from green_normal_price
        }
        holder.adjustedPriceTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, colorResId))
    }

    override fun getItemCount(): Int = prices.size

    class PriceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val rawPriceTextView: TextView = itemView.findViewById(R.id.rawPriceTextView)
        val adjustedPriceTextView: TextView = itemView.findViewById(R.id.adjustedPriceTextView)

        fun bind(hourlyPrice: HourlyPrice) {
            timeTextView.text = hourlyPrice.time
            // Bruk formatPrice fra ElectricityPriceCalculator
            rawPriceTextView.text = "Rå: ${ElectricityPriceCalculator.formatPrice(hourlyPrice.rawPriceMva)} kr/kWh"
            adjustedPriceTextView.text = "Just: ${ElectricityPriceCalculator.formatPrice(hourlyPrice.adjustedPriceMva)} kr/kWh"
        }
    }
}