package com.sanaker.stromapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
//import androidx.compose.ui.semantics.text
//import androidx.compose.ui.tooling.data.position
import androidx.core.content.ContextCompat
import androidx.core.text.color
//import androidx.glance.visibility
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
// Important: Use MaterialToolbar if that's what you have in XML
import com.google.android.material.appbar.MaterialToolbar // If using MaterialToolbar
// import androidx.appcompat.widget.Toolbar // If using androidx.appcompat.widget.Toolbar

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

// Assuming PriceData class is defined in your project
// import com.sanaker.stromapp.PriceData

class MainActivity : AppCompatActivity() {

    companion object {
        private const val HIGH_PRICE_THRESHOLD = 0.60
        private const val LOW_PRICE_THRESHOLD = 0.20
        private const val DEFAULT_BASE_URL = "https://api.sanakerdagestad.no"
    }

    private lateinit var priceTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var lineChart: LineChart
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: MaterialToolbar // Use MaterialToolbar

    private var currentBaseUrl: String = DEFAULT_BASE_URL

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

    private val apiTimeInputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("dd. MMM HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Setup Toolbar ---
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        // You can customize the toolbar further here, e.g., toolbar.setTitle("My Title")
        // The title from app:title in XML should already be applied.

        currentBaseUrl = getBaseUrlFromPreferences()

        priceTextView = findViewById(R.id.priceTextView)
        timeTextView = findViewById(R.id.timeTextView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        lineChart = findViewById(R.id.lineChart)
        statusTextView = findViewById(R.id.statusTextView)
        progressBar = findViewById(R.id.progressBar)

        swipeRefreshLayout.setOnRefreshListener {
            fetchAndDisplayData()
        }

        setupLineChartAppearance()
        fetchAndDisplayData()
    }

    override fun onResume() {
        super.onResume()
        val newBaseUrl = getBaseUrlFromPreferences()
        val newShowValuesSetting = shouldShowValuesOnChart()

        var settingsChanged = false
        if (currentBaseUrl != newBaseUrl) {
            currentBaseUrl = newBaseUrl
            settingsChanged = true
        }

        if (lineChart.data != null) {
            val dataSet = lineChart.data.dataSets.firstOrNull() as? LineDataSet
            if (dataSet != null && dataSet.isDrawValuesEnabled != newShowValuesSetting) {
                dataSet.setDrawValues(newShowValuesSetting)
                lineChart.invalidate()
            }
        }

        if (settingsChanged) {
            fetchAndDisplayData()
        }
    }

    // --- Options Menu Callbacks ---
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // --- Preference Helper Functions ---
    private fun getBaseUrlFromPreferences(): String {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPreferences.getString("base_url", DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    private fun shouldShowValuesOnChart(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPreferences.getBoolean("show_values_on_chart", false)
    }


    // --- Chart and Data Functions ---
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
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = ContextCompat.getColor(this, R.color.text_color_secondary_transparent)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(Locale.getDefault(), "%02d", value.toInt())
            }
        }
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 23f

        val leftAxis = lineChart.axisLeft
        leftAxis.textColor = ContextCompat.getColor(this, R.color.text_color_secondary)
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = ContextCompat.getColor(this, R.color.text_color_secondary_transparent)
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(Locale.getDefault(), "%.2f kr", value)
            }
        }

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

        lifecycleScope.launch {
            try {
                Log.d("API_CALL", "Fetching from: $currentBaseUrl")
                val responseData: List<PriceData> = client.get(currentBaseUrl).body()
                Log.d("API_RESPONSE", "Received ${responseData.size} PriceData items")

                if (responseData.isNotEmpty()) {
                    val calendar = Calendar.getInstance()
                    val currentSystemHour = calendar.get(Calendar.HOUR_OF_DAY)

                    val currentPriceDataPoint = responseData.find { priceData ->
                        try {
                            apiTimeInputFormat.parse(priceData.time)?.let { date ->
                                val tempCalendar = Calendar.getInstance()
                                tempCalendar.time = date
                                tempCalendar.get(Calendar.HOUR_OF_DAY) == currentSystemHour
                            } ?: false
                        } catch (e: Exception) {
                            Log.e("TimeParseFind", "Error parsing time for find: ${priceData.time}", e)
                            false
                        }
                    } ?: responseData.firstOrNull()

                    if (currentPriceDataPoint != null) {
                        val currentPriceValue = currentPriceDataPoint.price
                        priceTextView.text = "${String.format(Locale.US, "%.2f", currentPriceValue)} kr/kWh"

                        val priceColor = when {
                            currentPriceValue > HIGH_PRICE_THRESHOLD ->
                                ContextCompat.getColor(this@MainActivity, R.color.high_price_red)
                            currentPriceValue < LOW_PRICE_THRESHOLD ->
                                ContextCompat.getColor(this@MainActivity, R.color.low_price_green)
                            else ->
                                ContextCompat.getColor(this@MainActivity, R.color.medium_price_black)
                        }
                        priceTextView.setTextColor(priceColor)

                        try {
                            apiTimeInputFormat.parse(currentPriceDataPoint.time)?.let { parsedDate ->
                                timeTextView.text = displayTimeFormat.format(parsedDate)
                            } ?: run {
                                timeTextView.text = currentPriceDataPoint.time
                            }
                        } catch (e: Exception) {
                            timeTextView.text = currentPriceDataPoint.time
                            Log.e("TimeParseDisplay", "Could not parse time for display: ${currentPriceDataPoint.time}", e)
                        }
                    } else {
                        priceTextView.text = "Data utilgjengelig"
                        priceTextView.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_color_primary))
                        timeTextView.text = "Ukjent tid"
                    }

                    populateLineChart(responseData)
                    statusTextView.text = ""
                    statusTextView.visibility = View.GONE

                } else {
                    Log.d("API_RESPONSE", "Response data is empty.")
                    handleApiError("Ingen prisdata mottatt.")
                    priceTextView.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_color_primary))
                }

            } catch (e: Exception) {
                Log.e("API_ERROR", "Error fetching or processing data: ${e.message}", e)
                handleApiError("Feil: ${e.localizedMessage ?: "Ukjent feil"}")
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
        val calendar = Calendar.getInstance()

        for (dataPoint in priceHistory) {
            try {
                apiTimeInputFormat.parse(dataPoint.time)?.let { date ->
                    calendar.time = date
                    val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY).toFloat()
                    entries.add(Entry(hourOfDay, dataPoint.price.toFloat()))
                }
            } catch (e: Exception) {
                Log.e("ChartData", "Error parsing date or creating entry for chart: ${dataPoint.time}", e)
            }
        }
        Log.d("ChartPopulate", "Total entries created for chart: ${entries.size}")

        if (entries.isEmpty()) {
            lineChart.setNoDataText("Ingen gyldig data for graf.")
            lineChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_color_secondary))
            lineChart.data = null
            lineChart.invalidate()
            return
        }

        entries.sortBy { it.x }

        val dataSet = LineDataSet(entries, "Pris (kr/kWh)")
        dataSet.color = ContextCompat.getColor(this, R.color.good_price)
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.text_color_primary)
        dataSet.lineWidth = 2.5f
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.normal_price))
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 10f
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawValues(shouldShowValuesOnChart())

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
        lineChart.animateX(1000)
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        // lineChart.setTouchEnabled(!isLoading) // Consider if you want chart interaction disabled during load
        if (isLoading) {
            statusTextView.visibility = View.VISIBLE
        }
    }

    private fun handleApiError(message: String) {
        priceTextView.text = "Feil"
        timeTextView.text = "Data utilgjengelig"
        statusTextView.text = message
        statusTextView.visibility = View.VISIBLE
        lineChart.setNoDataText(message)
        lineChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.bad_price))
        lineChart.data = null // Clear previous data
        lineChart.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close()
    }
}