package com.receparslan.travelbook.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.receparslan.travelbook.R
import com.receparslan.travelbook.databinding.ActivityMapsBinding
import com.receparslan.travelbook.model.Location
import com.receparslan.travelbook.roomDB.AppDatabase
import com.receparslan.travelbook.roomDB.LocationDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap // GoogleMap object to interact with the map

    private lateinit var binding: ActivityMapsBinding // ViewBinding object to interact with the views

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient // FusedLocationProviderClient object to get the user's location

    private lateinit var locationDAO: LocationDAO // LocationDAO object to interact with the database

    private var isOld: Boolean? = null // Boolean to check if the location is old or new

    private var selectedLocation: Location? = null // Location object to store the selected location

    private var oldLocation: Location? = null // Location object to selected location from the list on the home screen

    private var markerOptions: MarkerOptions? = null // MarkerOptions object to set the marker on the map

    // Request permission for location access
    private val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission is granted
            startLocationUpdates()
        } else {
            // Permission is denied
            Toast.makeText(this, "Permission needed for location!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using ViewBinding
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Get the intent extras and set the views accordingly based on the location is old or new
        isOld = intent.getBooleanExtra("isOld", false)

        // Room Database - Initialize the LocationDAO
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "locations").build()
        locationDAO = db.locationDAO()

        // Check if the location is old or new
        if (isOld == true) {
            // If the location is old, set the views accordingly
            binding.saveButton.visibility = View.GONE
            binding.nameEditText.visibility = View.GONE
            binding.deleteButton.visibility = View.VISIBLE
            binding.nameTextView.visibility = View.VISIBLE

            // Load the selected location from the database
            lifecycleScope.launch {
                // Load the selected location from the database
                oldLocation = locationDAO.loadByID(intent.getIntExtra("locationID", -1)).firstOrNull()

                oldLocation?.let { location ->
                    // Set the marker on the map for the selected location from the list on the home screen
                    markerOptions = MarkerOptions().position(LatLng(location.latitude ?: 0.0, location.longitude ?: 0.0))

                    // Add the marker to the map and move the camera to the marker
                    markerOptions?.let {
                        mMap.addMarker(it)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it.position, 15f))
                    }
                }

                // Set the views based on the selected location
                withContext(Dispatchers.Main) {
                    // Set the name of the location
                    oldLocation?.let { binding.nameTextView.text = it.name }
                }
            }
        } else {
            // If the location is new, set the views accordingly
            binding.saveButton.visibility = View.VISIBLE
            binding.nameEditText.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.GONE
            binding.nameTextView.visibility = View.GONE
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Initialize the FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        checkPermission() // Check the location permission to start the location updates

        // Set the my location button to the bottom right corner
        val locationButton = (findViewById<View>("1".toInt()).parent as View).findViewById<View>("2".toInt())
        val params = locationButton.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        params.setMargins(0, 0, 30, 150)
        locationButton.setLayoutParams(params)

        // Check if the location is old or new
        if (isOld == true) {
            mMap.clear() // Clear the map to set the marker for the selected location

            // Set the marker on the map for the selected location from the list on the home screen
            oldLocation?.let { location ->
                markerOptions = MarkerOptions().position(LatLng(location.latitude ?: 0.0, location.longitude ?: 0.0))
                markerOptions?.let {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it.position, 15f))
                    mMap.addMarker(it)

                    binding.nameTextView.setOnClickListener { _ -> mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it.position, 15f)) }
                }
            }

            binding.deleteButton.setOnClickListener { _ -> delete() } // Set the delete button click listener
        } else {
            // Set the long click listener to select a location on the map
            mMap.setOnMapLongClickListener { latLng ->
                mMap.clear() // Clear the map to set the marker for delete the previous marker

                // Set the marker on the map for the selected location
                markerOptions = MarkerOptions().position(latLng)
                markerOptions?.let { mMap.addMarker(it) }

                // Set the selected location's latitude and longitude based on the selected location
                selectedLocation = Location()
                selectedLocation?.let { it.latitude = latLng.latitude }
                selectedLocation?.let { it.longitude = latLng.longitude }
            }

            binding.saveButton.setOnClickListener { _ -> save() } // Set the save button click listener
        }
    }

    // Save the location to the database
    private fun save() {
        // Check if the location is selected
        if (selectedLocation == null) {
            Toast.makeText(this, "Please select a location!", Toast.LENGTH_LONG).show()
        } else {
            selectedLocation?.let { location ->
                location.name = binding.nameEditText.text.toString() // Set the name of the location

                location.name?.let { // Check if the name is empty
                    if (it.isEmpty()) {
                        Toast.makeText(this, "Please enter a name!", Toast.LENGTH_LONG).show()
                    } else {
                        lifecycleScope.launch {
                            locationDAO.insert(location) // Insert the location to the database

                            // Go back to the home screen
                            val intent = Intent(applicationContext, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    // Delete the location from the database
    private fun delete() {
        // Show the alert dialog to confirm the deletion
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Delete Location")
        alertDialog.setMessage("Are you sure you want to delete this location?")
        alertDialog.setPositiveButton("yes") { _, _ ->
            // Delete the location from the database and go back to the home screen
            oldLocation?.let {
                lifecycleScope.launch {
                    locationDAO.delete(it) // Delete the location from the database

                    // Go back to the home screen
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                }
            }
        }
        alertDialog.setNegativeButton("no") { dialogInterface, _ -> dialogInterface.dismiss() }
        alertDialog.show()
    }

    // Check the location permission to show the user's location on the map
    private fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ->
                // Permission is granted and show the user's location on the map
                startLocationUpdates() // Start the location updates to show the user's location on the map

            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) ->
                // Permission is denied
                Snackbar.make(binding.root, "Permission needed for location!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Give Permission") { locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                    .show()

            else ->
                // Permission is never asked before
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Start the location updates to show the user's location on the map
    private fun startLocationUpdates() {

        // Request the location updates
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Get the user's location and show it on the map and start the location updates
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, { location ->
                val userLocation = LatLng(location.latitude, location.longitude) // Get the user's location

                // Check if the activity started for the first time
                if (!mMap.isMyLocationEnabled) {
                    mMap.isMyLocationEnabled = true
                    if (markerOptions == null)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                }
            }, Looper.getMainLooper())
        }
    }
}