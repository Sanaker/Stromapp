package com.sanaker.stromapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.color
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
// Make sure this import points to your PriceData class
// import com.sanaker.stromapp.PriceData

class MainActivity : AppCompatActivity() {

    private val BASE_URL = "http://10.0.2.2:5000" // For Android emulator to reach localhost:5000 on host machine
    // private val BASE_URL = "https://api.sanakerdagestad.no" // If your API is deployed
    // private val BASE_URL = "http://YOUR_COMPUTER_IP:5000" // If testing on physical device on same WiFi
    companion object {
        private const val HIGH_PRICE_THRESHOLD = 0.60
        private const val LOW_PRICE_THRESHOLD = 0.20
    }
    private lateinit var priceTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var lineChart: LineChart
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar

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

    // Input format from API (e.g., "2025-06-18 00:00")
    private val apiTimeInputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    // For formatting time displayed in timeTextView
    private val displayTimeFormat = SimpleDateFormat("dd. MMM HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        priceTextView = findViewById(R.id.priceTextView)
        timeTextView = findViewById(R.id.timeTextView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        lineChart = findViewById(R.id.lineChart)
        statusTextView = findViewById(R.id.statusTextView) // Ensure this ID exists in XML
        progressBar = findViewById(R.id.progressBar)     // Ensure this ID exists in XML

        swipeRefreshLayout.setOnRefreshListener {
            fetchAndDisplayData()
        }

        setupLineChartAppearance()
        fetchAndDisplayData() // Initial data fetch
    }

    private fun setupLineChartAppearance() {
        lineChart.description.isEnabled = true
        lineChart.description.text = "Prisutvikling (NOK/kWh)"
        lineChart.description.textColor = ContextCompat.getColor(this, R.color.text_color_secondary)

        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.setDrawGridBackground(false)
        lineChart.setBackgroundColor(ContextCompat.getColor(this, R.color.background_color))

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = ContextCompat.getColor(this, R.color.text_color_secondary)
        xAxis.granularity = 1f // Show every hour label
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = ContextCompat.getColor(this, R.color.text_color_secondary_transparent)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format("%02d", value.toInt()) // Format as "00", "01", etc.
            }
        }
        xAxis.axisMinimum = 0f  // Assuming hours 0-23
        xAxis.axisMaximum = 23f

        val leftAxis = lineChart.axisLeft
        leftAxis.textColor = ContextCompat.getColor(this, R.color.text_color_secondary)
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = ContextCompat.getColor(this, R.color.text_color_secondary_transparent)
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format("%.2f kr", value)
            }
        }
        // Consider setting axisMinimum based on data or to 0f
        // leftAxis.axisMinimum = 0f // Or adjust based on your price range

        lineChart.axisRight.isEnabled = false
        lineChart.legend.textColor = ContextCompat.getColor(this, R.color.text_color_secondary)
        lineChart.legend.form = com.github.mikephil.charting.components.Legend.LegendForm.LINE

        lineChart.setNoDataText("Laster prisdata...")
        lineChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_color_primary))
    }

    private fun fetchAndDisplayData() {
        swipeRefreshLayout.isRefreshing = true
        showLoading(true)
        statusTextView.text = "Laster prisdata..."
        // lineChart.clear() // You might clear the chart data here or in populateLineChart

        lifecycleScope.launch {
            try {
                Log.d("API_CALL", "Fetching from: $BASE_URL") // Assuming BASE_URL is defined
                val responseData: List<PriceData> = client.get(BASE_URL).body()
                Log.d("API_RESPONSE", "Received ${responseData.size} PriceData items")

                if (responseData.isNotEmpty()) {
                    // ****** START MODIFICATION FOR PRICE TEXT COLOR ******

                    // Logic to determine the current price data point
                    // This could be responseData.firstOrNull() or more complex logic
                    // to find the price for the actual current hour.
                    // For this example, let's assume you have a way to get it:
                    val calendar = Calendar.getInstance() // If needed for current hour logic
                    val currentSystemHour = calendar.get(Calendar.HOUR_OF_DAY) // If needed

                    // Example: Find price data for the current system hour
                    val currentPriceDataPoint = responseData.find { priceData ->
                        try {
                            apiTimeInputFormat.parse(priceData.time)?.let { date ->
                                calendar.time = date // Reuse calendar instance
                                calendar.get(Calendar.HOUR_OF_DAY) == currentSystemHour
                            } ?: false
                        } catch (e: Exception) {
                            false // Handle parsing error for this item
                        }
                    } ?: responseData.firstOrNull() // Fallback to the first item if current hour's not found or error

                    if (currentPriceDataPoint != null) {
                        val currentPriceValue = currentPriceDataPoint.price
                        priceTextView.text = "${String.format(Locale.US, "%.2f", currentPriceValue)} kr/kWh"

                        // Determine and set the text color
                        val priceColor = when {
                            currentPriceValue > HIGH_PRICE_THRESHOLD ->
                                ContextCompat.getColor(this@MainActivity, R.color.high_price_red)
                            currentPriceValue < LOW_PRICE_THRESHOLD ->
                                ContextCompat.getColor(this@MainActivity, R.color.low_price_green)
                            else ->
                                ContextCompat.getColor(this@MainActivity, R.color.medium_price_black) // Or your default text color like R.color.text_color_primary
                        }
                        priceTextView.setTextColor(priceColor)

                        // Update timeTextView as you were doing
                        try {
                            apiTimeInputFormat.parse(currentPriceDataPoint.time)?.let { parsedDate ->
                                timeTextView.text = displayTimeFormat.format(parsedDate)
                            } ?: run {
                                timeTextView.text = currentPriceDataPoint.time // Fallback
                            }
                        } catch (e: Exception) {
                            timeTextView.text = currentPriceDataPoint.time // Fallback
                            Log.e("TimeParse", "Could not parse time for display: ${currentPriceDataPoint.time}", e)
                        }
                    } else {
                        // Handle case where no suitable price data point is found
                        priceTextView.text = "Data utilgjengelig"
                        priceTextView.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_color_primary))
                        timeTextView.text = "Ukjent tid"
                    }
                    // ****** END MODIFICATION FOR PRICE TEXT COLOR ******

                    // Call populateLineChart as before
                    populateLineChart(responseData) // Ensure this function exists and works
                    statusTextView.text = ""
                    statusTextView.visibility = View.GONE

                } else {
                    Log.d("API_RESPONSE", "Response data is empty.")
                    handleApiError("Ingen prisdata mottatt.")
                    // Reset price text color on error
                    priceTextView.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_color_primary))
                }

            } catch (e: Exception) {
                Log.e("API_ERROR", "Error fetching or processing data: ${e.message}", e)
                handleApiError("Feil: ${e.localizedMessage ?: "Ukjent feil"}")
                // Reset price text color on error
                priceTextView.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_color_primary))
            } finally {
                swipeRefreshLayout.isRefreshing = false
                showLoading(false)
            }
        }
    }

    private fun populateLineChart(priceHistory: List<PriceData>) {
        Log.d("ChartPopulate", "populateLineChart called. Price history size: ${priceHistory.size}")
        val entries = ArrayList<Entry>()
        val calendar = Calendar.getInstance() // Reuse calendar instance

        for (dataPoint in priceHistory) {
            try {
                apiTimeInputFormat.parse(dataPoint.time)?.let { date ->
                    calendar.time = date
                    val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY).toFloat() // X value: hour (0-23)
                    entries.add(Entry(hourOfDay, dataPoint.price.toFloat()))
                    Log.d("ChartPopulate", "Added entry: X=${hourOfDay}, Y=${dataPoint.price.toFloat()}")
                }
            } catch (e: Exception) {
                Log.e("ChartData", "Error parsing date or creating entry for chart: ${dataPoint.time}", e)
            }
        }

        Log.d("ChartPopulate", "Total entries created: ${entries.size}")

        if (entries.isEmpty()) {
            Log.d("ChartPopulate", "Entries list is empty. Setting no data text.")
            lineChart.setNoDataText("Ingen gyldig data for graf.")
            lineChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_color_secondary))
            lineChart.data = null // Clear out old data
            lineChart.invalidate() // Refresh chart to show "no data" text
            return
        }

        // Sort entries by hour to ensure the line chart draws correctly
        entries.sortBy { it.x }

        val dataSet = LineDataSet(entries, "Pris (kr/kWh)")
        dataSet.color = ContextCompat.getColor(this, R.color.good_price) // Example color
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.text_color_primary)
        dataSet.lineWidth = 2.5f
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.normal_price)) // Example color
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 10f
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawValues(false) // Hide values on points if too cluttered

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate() // Refresh chart
        lineChart.animateX(1000) // Optional animation
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        lineChart.setTouchEnabled(!isLoading) // Disable chart interaction during load
        if (isLoading) {
            statusTextView.visibility = View.VISIBLE
        }
        // statusTextView will be hidden or updated in fetchAndDisplayData or handleApiError
    }

    private fun handleApiError(message: String) {
        priceTextView.text = "Feil"
        timeTextView.text = "Data utilgjengelig"
        statusTextView.text = message
        statusTextView.visibility = View.VISIBLE
        lineChart.setNoDataText(message) // Show error message on chart
        lineChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.bad_price)) // Example color
        lineChart.data = null // Clear out old data
        lineChart.invalidate() // Refresh chart to show error message
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close()
    }
}