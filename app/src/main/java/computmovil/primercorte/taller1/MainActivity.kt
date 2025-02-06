package computmovil.primercorte.taller1

import android.Manifest
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
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener, OnMapReadyCallback {
    private var sensorManager: SensorManager? = null
    private var locationManager: LocationManager? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var currentLocation: Location? = null
    private var mMap: GoogleMap? = null
    private val webSocketManager = WebSocketManager("ws://your-server-url/ws")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0f, this)
        }

        if (accelerometer != null) sensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        if (gyroscope != null) sensorManager!!.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)

        webSocketManager.connect { message ->
            runOnUiThread {
                Log.d("WebSocket", "Message received: $message")
            }
        }

        setupMap()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val initialLocation = LatLng(-34.0, 151.0)
        mMap!!.addMarker(MarkerOptions().position(initialLocation).title("Ubicación inicial"))
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 15f))
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val json = JSONObject()
            try {
                json.put("sensor", event.sensor.type)
                json.put("x", event.values[0].toDouble())
                json.put("y", event.values[1].toDouble())
                json.put("z", event.values[2].toDouble())
                webSocketManager.sendMessage(json.toString())
            } catch (e: Exception) {
                Log.e("WebSocket", "Error enviando datos del sensor", e)
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
        val json = JSONObject()
        try {
            json.put("latitude", location.latitude)
            json.put("longitude", location.longitude)
            webSocketManager.sendMessage(json.toString())
        } catch (e: Exception) {
            Log.e("WebSocket", "Error enviando datos de ubicación", e)
        }

        val userLocation = LatLng(location.latitude, location.longitude)
        mMap!!.clear()
        mMap!!.addMarker(MarkerOptions().position(userLocation).title("Mi ubicación"))
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}
