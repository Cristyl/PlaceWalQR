package com.example.placewalqr

import RetrofitInstance
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.placewalqr.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import androidx.compose.foundation.isSystemInDarkTheme

class MapsFragment : Fragment(R.layout.activity_maps), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var currentLocationMarker: Marker?=null
    private var selectedPlace by mutableStateOf<VisitedPlaceResponse?>(null)
    private var isInfoWindowVisible by mutableStateOf(false)

    companion object{
        private const val DEFAULT_ZOOM=15f
    }
    private val locationPermissionRequest =registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){ permissions ->
        when{
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                enableMyLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                enableMyLocation()
            }
            else -> {
                Toast.makeText(requireContext(), "Permission location not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    MapsScreen()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Initialization client for localization
        fusedLocationClient= LocationServices.getFusedLocationProviderClient(requireContext())

        //localization request initialization
        createLocationRequest()
        createLocationCallback()
    }

    @Composable
    private fun MapsScreen() {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            id = if (isSystemInDarkTheme()) R.drawable.placewalqr_logo_dark_lol
                            else R.drawable.placewalqr_logo
                        ),
                        modifier = Modifier.width(250.dp),
                        contentDescription = "App Logo"
                    )
                }

                // Maps from google
                AndroidView(
                    factory = { context ->
                        MapView(context).apply {
                            mapView = this
                            onCreate(null)
                            getMapAsync(this@MapsFragment)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { mapView ->
                    mapView.onResume()
                }
            }

            // Info view for a point on the maps
            if (isInfoWindowVisible && selectedPlace != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CustomInfoWindow(
                        place = selectedPlace!!,
                        onDismiss = {
                            isInfoWindowVisible = false
                            selectedPlace = null
                        }
                    )
                }
            }
        }
    }

    // Element for the info view
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun CustomInfoWindow(
        place: VisitedPlaceResponse,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier
                .width(280.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Image of the place
                val imageBytes = place.getImageBytes()
                if (imageBytes != null && imageBytes.isNotEmpty()) {
                    val bitmap = loadBitmap(imageBytes)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Place photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                androidx.compose.ui.graphics.Color.Gray,
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_gallery),
                            contentDescription = "No image",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title of the place
                Text(
                    text = place.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = androidx.compose.ui.graphics.Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Information about the place
                Text(
                    text = place.information,
                    fontSize = 14.sp,
                    color = androidx.compose.ui.graphics.Color.Gray,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status and Coordinates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status (visited or not)
                    Text(
                        text = if (place.visited) "Visited" else "Not visited yet",
                        fontSize = 12.sp,
                        color = if (place.visited) {
                            androidx.compose.ui.graphics.Color.Green
                        } else {
                            androidx.compose.ui.graphics.Color.Red
                        },
                        modifier = Modifier
                            .background(
                                androidx.compose.ui.graphics.Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(4.dp)
                    )
                    //Coordinates
                    Text(
                        text = "${place.latitude}, ${place.longitude}",
                        fontSize = 11.sp,
                        color = androidx.compose.ui.graphics.Color.Gray,
                        modifier = Modifier.padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("MapReady", "Map is ready!")
        mMap = googleMap

        Log.d("MapReady", "About to load place markers")
        setupInfoAdapter()

        // map configuration
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isCompassEnabled = true

        loadPlaceMarkers()

        // check localization permission
        if (checkLocationPermission()) {
            Log.d("MapReady", "Location permission granted, enabling location")
            enableMyLocation()
        } else {
            Log.d("MapReady", "Requesting location permission")
            requestLocationPermission()
        }
    }

    private fun setupInfoAdapter() {
        mMap.setOnMarkerClickListener { marker ->
            val placeData = marker.tag as? VisitedPlaceResponse
            if (placeData != null) {
                selectedPlace = placeData
                isInfoWindowVisible = true
            }
            true // Return true to prevent default behavior
        }

        mMap.setOnMapClickListener {
            // Hide info window when clicking on map
            isInfoWindowVisible = false
            selectedPlace = null
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .apply {
                setMinUpdateIntervalMillis(5000)
                setMaxUpdateDelayMillis(15000)
            }.build()
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationOnMap(location)
                }
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (!checkLocationPermission())
            return

        mMap.isMyLocationEnabled = true
        getCurrentLocationAndCenter()
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocationAndCenter() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                currentLocationMarker?.remove()
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM))
                updateLocationOnMap(location)
            } else {
                requestSingleLocationUpdate()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Impossible to get current position", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleLocationUpdate() {
        val singleUpdateRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
            .setMaxUpdates(1).build()
        fusedLocationClient.requestLocationUpdates(
            singleUpdateRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationRequest: LocationResult) {
                    locationRequest.lastLocation?.let { location ->
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        currentLocationMarker?.remove()
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM))
                        updateLocationOnMap(location)
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
            },
            Looper.getMainLooper()
        )
    }

    // Add user position on the map
    private fun updateLocationOnMap(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)
        val latFormatted = "%.6f".format(location.latitude)
        val lngFormatted = "%.6f".format(location.longitude)
        Log.d("Location", "Current location: $latFormatted, $lngFormatted")

        // remove previous marker
        currentLocationMarker?.remove()

        // add new marker
        currentLocationMarker = mMap.addMarker(
            MarkerOptions()
                .position(currentLatLng)
                .title("Position")
                .snippet("Lat: $latFormatted, Lng: $lngFormatted")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )

        Log.d("Location", "Location marker added: ${currentLocationMarker != null}")
        Log.d("Location", "Marker position: ${currentLocationMarker?.position}")

        val currentCamera = mMap.cameraPosition
        Log.d("Camera", "Camera target: ${currentCamera.target}")
        Log.d("Camera", "Camera zoom: ${currentCamera.zoom}")
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!checkLocationPermission())
            return

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // Load place markers from the database
    private fun loadPlaceMarkers() {
        Log.d("Markers", "Starting to load place markers")

        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val id = sharedPreferences.getString("id", "0").toString().toInt()

        Log.d("Markers", "User ID: $id")

        lifecycleScope.launch {
            try {
                Log.d("Markers", "Making API call...")
                val response = RetrofitInstance.apiService.findVisitedPlaceById(id)

                Log.d("Markers", "API Response code: ${response.code()}")
                Log.d("Markers", "API Response successful: ${response.isSuccessful}")

                val places = response.body()
                Log.d("Markers", "Places received: ${places?.size ?: 0}")

                places?.forEach { place ->
                    Log.d("Markers", "Adding marker for: ${place.name} at ${place.latitude}, ${place.longitude}")
                    addPlaceMarker(place)
                }
            } catch (e: Exception) {
                Log.e("Markers", "Error loading places: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Add place markers on the map
    private fun addPlaceMarker(place: VisitedPlaceResponse) {
        val placeLatLng = LatLng(place.latitude, place.longitude)
        Log.d("PlaceMarker", "Adding place marker at: ${place.latitude}, ${place.longitude}")

        val marker = if (place.visited) {
            mMap.addMarker(
                MarkerOptions()
                    .position(placeLatLng)
                    .title(place.name)
                    .snippet("Lat: ${place.latitude}, Lng: ${place.longitude}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
        } else {
            mMap.addMarker(
                MarkerOptions()
                    .position(placeLatLng)
                    .title(place.name)
                    .snippet("Lat: ${place.latitude}, Lng: ${place.longitude}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }

        marker?.tag = place
        Log.d("PlaceMarker", "Marker added successfully for ${place.name}")
        Log.d("PlaceMarker", "Place marker added: ${marker != null}")
        Log.d("PlaceMarker", "Place marker position: ${marker?.position}")
    }

    private fun loadBitmap(byteArray: ByteArray): Bitmap? {
        return try {
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: Exception) {
            Log.e("Bitmap load", "Error: ${e.message}")
            null
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mapView.isInitialized) {
            mapView.onResume()
        }
        if (checkLocationPermission() && ::mMap.isInitialized) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mapView.isInitialized) {
            mapView.onPause()
        }
        if (::fusedLocationClient.isInitialized) {
            stopLocationUpdates()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mapView.isInitialized) {
            mapView.onDestroy()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (::mapView.isInitialized) {
            mapView.onLowMemory()
        }
    }
}