package com.sanaker.stromapp

import java.math.BigDecimal
import java.math.RoundingMode

// Konstanter for beregning
const val MVA_RATE = 0.25
const val STROEMSTOTTE_DEKNING = 0.90 // 90% dekning av støtten
const val STROEMSTOTTE_GRENSE = 0.9375 //i NOK

object ElectricityPriceCalculator {

    /**
     * Legger til MVA på gitt pris.
     * @param pris Prisen før MVA i NOK/kWh.
     * @return Prisen med MVA i NOK/kWh.
     */
    fun leggTilMva(pris: Double): Double {
        return pris * (1 + MVA_RATE)
    }

    /**
     * Justerer pris med strømstøtte basert på gjeldene grense og dekningsgrad.
     * @param pris Prisen før strømstøttejustering i NOK/kWh.
     * @return Prisen justert med strømstøtte i NOK/kWh.
     */
    fun justerMedStotte(pris: Double): Double {
        return if (pris > STROEMSTOTTE_GRENSE) {
            val stotte = (pris - STROEMSTOTTE_GRENSE) * STROEMSTOTTE_DEKNING
            pris - stotte
        } else {
            pris
        }
    }

    /**
     * Hjelpefunksjon for å formatere Double til to desimaler for visning.
     * Bruker BigDecimal for nøyaktighet i formatering.
     */
    fun formatPrice(price: Double): String {
        return BigDecimal(price).setScale(2, RoundingMode.HALF_UP).toPlainString()
    }
}