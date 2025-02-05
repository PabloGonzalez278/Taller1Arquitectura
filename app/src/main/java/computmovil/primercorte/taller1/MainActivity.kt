package computmovil.primercorte.taller1

import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener, OnMapReadyCallback {
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var currentLocation: Location? = null
    private lateinit var webSocket: WebSocket
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Solicitar permisos de ubicación
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0f, this)
        }

        // Registrar sensores
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }

        setupWebSocket()
        setupMap()
    }

    private fun setupWebSocket() {
        val client = OkHttpClient()
        val request = Request.Builder().url("ws://YOUR_SERVER_IP:8000/ws").build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.d("WebSocket", "Connected")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e("WebSocket", "Error: ${t.message}")
            }
        })
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val initialLocation = LatLng(-34.0, 151.0) // Ubicación de prueba
        mMap.addMarker(MarkerOptions().position(initialLocation).title("Ubicación inicial"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 15f))
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val json = JSONObject().apply {
                put("sensor", event.sensor.type)
                put("x", event.values[0])
                put("y", event.values[1])
                put("z", event.values[2])
            }
            webSocket.send(json.toString())
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
        val json = JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
        }
        webSocket.send(json.toString())

        val userLocation = LatLng(location.latitude, location.longitude)
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(userLocation).title("Mi ubicación"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
