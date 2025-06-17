package com.sanaker.stromapp

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat // Ny import for farger
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.call.body
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Data class (forblir uendret, men sørg for at den er i PriceData.kt)
// @Serializable
// data class PriceData(...)

class MainActivity : AppCompatActivity() {

    private val BASE_URL = "https://api.sanakerdagestad.no" // Eller http://10.0.2.2:5000/prices hvis du bruker den ruten

    private lateinit var priceTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var imageView: ImageView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var priceLabel: TextView // Ny label
    private lateinit var graphLabel: TextView // Ny label

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("KtorClient", message)
                }
            }
            level = LogLevel.ALL
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        priceTextView = findViewById(R.id.priceTextView)
        timeTextView = findViewById(R.id.timeTextView)
        imageView = findViewById(R.id.imageView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        priceLabel = findViewById(R.id.priceLabel) // Initialiser label
        graphLabel = findViewById(R.id.graphLabel) // Initialiser label

        // Konfigurer "dra ned for å oppdatere"
        swipeRefreshLayout.setOnRefreshListener {
            fetchPrices() // Kall fetchPrices når brukeren drar ned
        }

        fetchPrices() // Hent priser ved oppstart
    }

    private fun fetchPrices() {
        // Vis "laster"-indikator
        swipeRefreshLayout.isRefreshing = true
        // Nullstill tekstene mens vi laster
        priceTextView.text = "Laster pris..."
        timeTextView.text = "Laster tid..."
        priceTextView.setTextColor(ContextCompat.getColor(this, R.color.text_color_primary)) // Sett til standard farge
        imageView.setImageDrawable(null) // Tøm bildet

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response: List<PriceData> = client.get(BASE_URL).body<List<PriceData>>()

                withContext(Dispatchers.Main) {
                    if (response.isNotEmpty()) {
                        val firstPrice = response[0]

                        // Oppdater pris og tid
                        priceTextView.text = "${String.format("%.2f", firstPrice.price)} kr/kWh" // Legg til enhet
                        timeTextView.text = "${formatTime(firstPrice.time)}" // Formater tid

                        // Endre farge basert på pris
                        val priceColor = when {
                            firstPrice.price < 0.20 -> R.color.good_price // Billig (juster grenser)
                            firstPrice.price < 0.80 -> R.color.normal_price // Normal (juster grenser)
                            else -> R.color.bad_price // Dyrt
                        }
                        priceTextView.setTextColor(ContextCompat.getColor(this@MainActivity, priceColor))


                        // Håndter bilde
                        firstPrice.image?.let { base64Image ->
                            try {
                                if (base64Image.isNotBlank()) {
                                    val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    imageView.setImageBitmap(decodedImage)
                                } else {
                                    Log.d("ImageDecode", "Received empty or blank Base64 image string.")
                                    imageView.setImageDrawable(null)
                                }
                            } catch (e: IllegalArgumentException) {
                                Log.e("ImageDecode", "Base64 string is not valid: ${e.message}", e)
                                imageView.setImageDrawable(null)
                            }
                        } ?: run {
                            Log.d("ImageDecode", "No image string provided in JSON.")
                            imageView.setImageDrawable(null)
                        }

                    } else {
                        priceTextView.text = "Ingen prisdata tilgjengelig."
                        timeTextView.text = ""
                        imageView.setImageDrawable(null)
                    }
                }
            } catch (e: Exception) {
                Log.e("Network", "Nettverksfeil eller feil ved databehandling: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    priceTextView.text = "Klarte ikke å hente pris: ${e.message}"
                    priceTextView.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.bad_price)) // Vis feilfarge
                    timeTextView.text = "Sjekk serverstatus og internettilkobling."
                    imageView.setImageDrawable(null)
                }
            } finally {
                // Skjul "laster"-indikator uansett om det var suksess eller feil
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    // Hjelpefunksjon for å formatere tid
    private fun formatTime(dateTimeString: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd. MMM HH:mm", java.util.Locale.getDefault())
            val date = inputFormat.parse(dateTimeString)
            if (date != null) {
                outputFormat.format(date)
            } else {
                dateTimeString // Returner original hvis parsing feiler
            }
        } catch (e: Exception) {
            Log.e("TimeFormat", "Failed to parse time string: $dateTimeString", e)
            dateTimeString // Returner original hvis parsing feiler
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close()
    }
}