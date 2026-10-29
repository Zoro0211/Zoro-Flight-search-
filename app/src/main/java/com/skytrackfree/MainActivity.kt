package com.skytrackfree

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var map: MapView
    private lateinit var status: TextView
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(12, 18, 31))
        }
        root.addView(TextView(this).apply {
            text = "✈ SKYTRACK FREE"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(12, 16, 12, 8)
        })
        status = TextView(this).apply {
            text = "Loading aircraft..."
            setTextColor(Color.WHITE)
            setPadding(12, 8, 12, 8)
        }
        root.addView(status)
        root.addView(Button(this).apply {
            text = "Refresh aircraft"
            setOnClickListener { loadAircraft() }
        })
        map = MapView(this).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(5.0)
            controller.setCenter(GeoPoint(13.0827, 80.2707))
        }
        root.addView(map, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
        loadAircraft()
    }

    private fun loadAircraft() {
        status.text = "Requesting aircraft positions..."
        thread {
            try {
                val url = "https://opensky-network.org/api/states/all?lamin=8&lomin=76&lamax=17&lomax=85"
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) error("Data service HTTP ${response.code}")
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val states = json.optJSONArray("states")
                    val planes = mutableListOf<Triple<String, GeoPoint, String>>()
                    if (states != null) {
                        for (i in 0 until states.length()) {
                            val a = states.optJSONArray(i) ?: continue
                            val lon = a.optDouble(5, Double.NaN)
                            val lat = a.optDouble(6, Double.NaN)
                            if (!lat.isFinite() || !lon.isFinite()) continue
                            val call = a.optString(1, "").trim().ifBlank { "Unknown aircraft" }
                            val altitude = a.optDouble(7, Double.NaN)
                            val speed = a.optDouble(9, Double.NaN)
                            val info = "ICAO: ${a.optString(0)}\nAltitude: " +
                                (if (altitude.isFinite()) "${(altitude * 3.28084).toInt()} ft" else "N/A") +
                                "\nSpeed: " +
                                (if (speed.isFinite()) "${(speed * 1.94384).toInt()} kt" else "N/A")
                            planes.add(Triple(call, GeoPoint(lat, lon), info))
                        }
                    }
                    runOnUiThread {
                        map.overlays.removeAll { it is Marker }
                        planes.forEach { p ->
                            val marker = Marker(map)
                            marker.position = p.second
                            marker.title = p.first
                            marker.snippet = p.third
                            map.overlays.add(marker)
                        }
                        map.invalidate()
                        status.text = "Aircraft found: ${planes.size} · OpenSky data"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Could not load aircraft. Check internet and retry."
                    Toast.makeText(this, e.message ?: "Request failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::map.isInitialized) map.onResume()
    }
    override fun onPause() {
        if (::map.isInitialized) map.onPause()
        super.onPause()
    }
    override fun onDestroy() {
        if (::map.isInitialized) map.onDetach()
        super.onDestroy()
    }
}
