package com.example.gastromaps.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.gastromaps.R
import com.example.gastromaps.databinding.ActivityMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var map: GoogleMap
    private var selectedMarker: Marker? = null
    private var selectedLocation: String = ""
    private var selectedLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }

        // Set up toolbar
        setSupportActionBar(binding.toolbarMap)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Select Restaurant"

        binding.toolbarMap.setNavigationOnClickListener {
            onBackPressed()
        }

        // Set up map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up search
        setupSearch()

        // Set up select button
        binding.btnSelectLocation.isEnabled = false
        binding.btnSelectLocation.setOnClickListener {
            if (selectedLocation.isNotEmpty() && selectedLatLng != null) {
                val intent = Intent().apply {
                    putExtra("address", selectedLocation)
                    putExtra("lat", selectedLatLng?.latitude ?: 0.0)
                    putExtra("lng", selectedLatLng?.longitude ?: 0.0)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Set default location (Belgrade)
        val belgrade = LatLng(44.787197, 20.457273)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(belgrade, 12f))

        // Enable zoom controls
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isZoomGesturesEnabled = true
        }

        // Set marker click listener
        map.setOnMarkerClickListener { marker ->
            selectedMarker = marker
            selectedLocation = "${marker.title}, ${marker.snippet}"
            selectedLatLng = marker.position
            binding.btnSelectLocation.isEnabled = true
            marker.showInfoWindow()
            true
        }

        // Enable my location if permission is granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        }
    }

    private fun setupSearch() {
        val autocompleteFragment = supportFragmentManager
            .findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        try {
            autocompleteFragment.apply {
                setTypeFilter(TypeFilter.ESTABLISHMENT)
                setLocationBias(RectangularBounds.newInstance(
                    LatLng(44.7, 20.3), // SW bounds
                    LatLng(44.9, 20.6)  // NE bounds
                ))
                setPlaceFields(listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS,
                    Place.Field.TYPES
                ))
                setHint("Search for restaurants")

                setOnPlaceSelectedListener(object : PlaceSelectionListener {
                    override fun onPlaceSelected(place: Place) {
                        Log.d("MapActivity", "Place selected: ${place.name}")
                        place.latLng?.let { latLng ->
                            // Clear previous marker
                            selectedMarker?.remove()

                            // Add new marker
                            selectedMarker = map.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title(place.name)
                                    .snippet(place.address)
                            )
                            selectedMarker?.showInfoWindow()

                            // Store selected location details
                            selectedLocation = "${place.name}, ${place.address}"
                            selectedLatLng = latLng

                            // Enable select button
                            binding.btnSelectLocation.isEnabled = true

                            // Move camera to selected location
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        }
                    }

                    override fun onError(status: com.google.android.gms.common.api.Status) {
                        Log.e("MapActivity", "An error occurred: $status")
                        Toast.makeText(this@MapActivity,
                            "Error searching for location: ${status.statusMessage}",
                            Toast.LENGTH_SHORT).show()
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("MapActivity", "Error setting up search: ${e.message}")
            Toast.makeText(this, "Error setting up search", Toast.LENGTH_SHORT).show()
        }
    }
}