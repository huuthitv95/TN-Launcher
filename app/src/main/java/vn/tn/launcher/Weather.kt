package vn.tn.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class Weather(val tempC: Int, val text: String)

private fun describe(code: Int) = when (code) {
    0 -> "Trời quang"
    1, 2 -> "Có mây"
    3 -> "Nhiều mây"
    45, 48 -> "Sương mù"
    in 51..67 -> "Mưa"
    in 71..77 -> "Tuyết"
    in 80..82 -> "Mưa rào"
    in 95..99 -> "Dông"
    else -> "—"
}

/** Open-Meteo, không cần khóa API. */
suspend fun fetchWeather(lat: Float, lon: Float): Weather? = withContext(Dispatchers.IO) {
    try {
        val u = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code"
        val c = JSONObject(URL(u).readText()).getJSONObject("current")
        Weather(Math.round(c.getDouble("temperature_2m")).toInt(), describe(c.getInt("weather_code")))
    } catch (e: Exception) { null }
}
