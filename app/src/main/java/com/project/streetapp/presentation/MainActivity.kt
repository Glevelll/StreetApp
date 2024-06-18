package com.project.streetapp.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.project.streetapp.R
import com.project.streetapp.data.local.BoundingBox
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var mapController: IMapController
    private lateinit var zoomInButton: ImageButton
    private lateinit var zoomOutButton: ImageButton
    private lateinit var myLocationButton: ImageButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var myLocationOverlay: FixedLocationOverlay? = null
    private var currentMarkerIndex = 0
    private lateinit var markerOverlays: List<FixedMarkerOverlay>
    companion object {
        const val LOCATION_PERMISSION_CODE = 101
    }

    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        zoomInButton = findViewById(R.id.zoomIn)
        zoomOutButton = findViewById(R.id.zoomOut)
        myLocationButton = findViewById(R.id.myLocation)
        markerOverlays = mapView.overlays.filterIsInstance<FixedMarkerOverlay>()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

        viewModel.locationPermissionGranted.observe(this) { granted ->
            if (granted) {
                initializeMap()
            } else {
                viewModel.requestLocationPermission(this)
            }
        }

        setClickListeners()
        viewModel.checkLocationPermission()
    }

    private fun initializeMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(false)
        mapController = mapView.controller
        mapController.setZoom(15.0)

        if (viewModel.locationPermissionGranted.value == true) {
            getCurrentLocationAndCenterMap(initialSetup = true)
        } else {
            val defaultLocation = GeoPoint(55.7558, 37.6176)
            mapController.setCenter(defaultLocation)
            addRandomMarkersAround(defaultLocation)
        }
    }

    private fun getCurrentLocationAndCenterMap(initialSetup: Boolean = false) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val myLocation = GeoPoint(location.latitude, location.longitude)
                mapController.setCenter(myLocation)

                myLocationOverlay?.let {
                    mapView.overlays.remove(it)
                }

                val myLocationDrawable =
                    ContextCompat.getDrawable(this, R.drawable.ic_my_tracker_46dp)
                if (myLocationDrawable != null) {
                    myLocationOverlay = FixedLocationOverlay(myLocation, myLocationDrawable)
                    mapView.overlays.add(myLocationOverlay)
                }

                if (initialSetup) {
                    addRandomMarkersAround(myLocation)
                }
                mapView.invalidate()
            } else {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to get current location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addRandomMarkersAround(center: GeoPoint) {
        val random = Random()
        val numMarkers = 3
        val boundingBox = BoundingBox(
            center.latitude - 0.01, center.longitude - 0.01,
            center.latitude + 0.01, center.longitude + 0.01
        )

        val markerNames = arrayOf("Виталий", "Сергей", "Мария")

        markerOverlays = (0 until numMarkers).map {
            val randomLat = boundingBox.minLat + random.nextDouble() * (boundingBox.maxLat - boundingBox.minLat)
            val randomLon = boundingBox.minLon + random.nextDouble() * (boundingBox.maxLon - boundingBox.maxLat)
            val randomPosition = GeoPoint(randomLat, randomLon)
            val markerDrawable = ContextCompat.getDrawable(this, R.drawable.ic_tracker_75dp)
            FixedMarkerOverlay(randomPosition, markerDrawable!!, this, markerNames[it])
        }

        mapView.overlays.addAll(markerOverlays)
        mapView.invalidate()
    }

    private fun setClickListeners() {
        zoomInButton.setOnClickListener {
            mapController.zoomIn()
        }

        zoomOutButton.setOnClickListener {
            mapController.zoomOut()
        }

        myLocationButton.setOnClickListener {
            if (viewModel.locationPermissionGranted.value == true) {
                getCurrentLocationAndCenterMap()
            } else {
                viewModel.requestLocationPermission(this)
            }
        }

        findViewById<ImageButton>(R.id.nextTracker).setOnClickListener {
            markerOverlays.let { overlays ->
                if (overlays.isNotEmpty()) {
                    val nextMarker = overlays[currentMarkerIndex]
                    mapController.animateTo(nextMarker.geoPoint)
                    currentMarkerIndex = (currentMarkerIndex + 1) % overlays.size
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }

    fun showBottomSheet(name: String, gps: String, time: String) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_layout, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        val nameTextView = bottomSheetView.findViewById<TextView>(R.id.textViewName)
        val gpsTextView = bottomSheetView.findViewById<TextView>(R.id.textViewGPS)
        val dateTextView = bottomSheetView.findViewById<TextView>(R.id.textViewDate)
        val timeTextView = bottomSheetView.findViewById<TextView>(R.id.textViewTime)
        val imageView = bottomSheetView.findViewById<ImageView>(R.id.imageView)

        nameTextView.text = name
        gpsTextView.text = gps
        dateTextView.text = SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date())
        timeTextView.text = time

        when (name) {
            "Виталий" -> imageView.setImageResource(R.drawable.img1)
            "Сергей" -> imageView.setImageResource(R.drawable.img2)
            "Мария" -> imageView.setImageResource(R.drawable.img3)
        }

        bottomSheetDialog.show()
    }
}