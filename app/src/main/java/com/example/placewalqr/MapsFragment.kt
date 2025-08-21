package com.example.placewalqr

import android.content.pm.PackageManager
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.ActivityResultLauncher

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.placewalqr.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.launch

class MapsFragment : Fragment(R.layout.activity_maps), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var currentLocationMarker: Marker?=null

    companion object{
        private const val LOCATION_PERMISSION_REQUEST_CODE=1
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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = ActivityMapsBinding.bind(view)

        //Initialization client for localization
        fusedLocationClient= LocationServices.getFusedLocationProviderClient(requireContext())

        //localization request initialization
        createLocationRequest()
        createLocationCallback()

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //FAB configuration for centering on current position
        //binding.fabMyLocation.setOnClickListener{
        //    centerOnCurrentLocation()
        //}
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

        // Add a marker in Sydney and move the camera
        // val sydney = LatLng(-34.0, 151.0)
        // mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        // mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        Log.d("MapReady", "About to load place markers")
        setupInfoAdapter()

        //map configuration
        mMap.uiSettings.isZoomControlsEnabled=true
        mMap.uiSettings.isMyLocationButtonEnabled=true
        mMap.uiSettings.isCompassEnabled=true

        loadPlaceMarkers()

        //check localization permission
        if(checkLocationPermission()){
            Log.d("MapReady", "Location permission granted, enabling location")
            enableMyLocation()
        } else {
            Log.d("MapReady", "Requesting location permission")
            requestLocationPermission()
        }

    }

    private fun setupInfoAdapter(){
        mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter{
            override fun getInfoContents(marker: Marker): View? {
                return null
            }

            override fun getInfoWindow(marker: Marker): View? {
                val infoView=layoutInflater.inflate(R.layout.custom_info_window, null)

                val titleTextView = infoView.findViewById<TextView>(R.id.title_marker)
                val descriptionView= infoView.findViewById<TextView>(R.id.description_marker)
                val photoImageView=infoView.findViewById<ImageView>(R.id.photo_marker)

                val placeData=marker.tag as? VisitedPlaceResponse
                if(placeData!=null){
                    titleTextView.text=placeData.name
                    descriptionView.text=placeData.information

                    val imageBytes=placeData.getImageBytes()
                    if (imageBytes!=null && imageBytes.isNotEmpty()){
                        val bitmap= resizeBitmap(imageBytes, 150, 150)
                        if(bitmap!=null){
                            photoImageView.setImageBitmap(bitmap)
                            photoImageView.visibility= View.VISIBLE
                        }else{
                            photoImageView.visibility=View.GONE
                            Log.w("InfoWindow", "Fail to load image ${placeData.name}")
                        }
                    }else{
                        photoImageView.visibility=View.GONE
                    }
                }else{
                    titleTextView.text=marker.title
                    descriptionView.text=marker.title
                    photoImageView.visibility= View.GONE
                }

                return infoView
            }
        })
    }

    private fun createLocationRequest(){
        locationRequest= LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                                        .apply {
                                            setMinUpdateIntervalMillis(5000)
                                            setMaxUpdateDelayMillis(15000)
                                        }.build()
    }

    private fun createLocationCallback(){
        locationCallback=object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationOnMap(location)
                }
            }
        }
    }

    private fun checkLocationPermission():Boolean{
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission(){
        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation(){
        if(!checkLocationPermission())
            return

        mMap.isMyLocationEnabled=true
        getCurrentLocationAndCenter()
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocationAndCenter(){
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if(location!=null){
                val currentLatLng= LatLng(location.latitude, location.longitude)
                currentLocationMarker?.remove()
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM))
                updateLocationOnMap(location)
            }else{
                requestSingleLocationUpdate()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Impossible to get current position", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleLocationUpdate(){
        val singleUpdateRequest= LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0).setMaxUpdates(1).build()
        fusedLocationClient.requestLocationUpdates(
            singleUpdateRequest,
            object : LocationCallback(){
                override fun onLocationResult(locationRequest: LocationResult) {
                    locationRequest.lastLocation?.let{location ->
                        val currentLatLng= LatLng(location.latitude, location.longitude)
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

    private fun updateLocationOnMap(location: Location){
        val currentLatLng= LatLng(location.latitude, location.longitude)
        val latFormatted = "%.6f".format(location.latitude)
        val lngFormatted = "%.6f".format(location.longitude)
        Log.d("Location", "Current location: $latFormatted, $lngFormatted")
        //remove previous marker
        currentLocationMarker?.remove()

        //add new marker
        currentLocationMarker=mMap.addMarker(
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
    private fun startLocationUpdates(){
        if(!checkLocationPermission())
            return

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

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

    //keep for manually centering the current position
    private fun centerOnCurrentLocation(){
        if(!checkLocationPermission()){
            requestLocationPermission()
            return
        }
    }

    private fun resizeBitmap(byteArray: ByteArray, maxWidth: Int, maxHeight: Int): Bitmap?{
        return  try {
            val options= BitmapFactory.Options().apply {
                inJustDecodeBounds=true
            }
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)

            val scaleFactor=minOf(options.outWidth/maxWidth, options.outHeight/maxHeight).coerceAtLeast(1)
            val finalOptions= BitmapFactory.Options().apply {
                inSampleSize=scaleFactor
                inJustDecodeBounds=false
            }

            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, finalOptions)

        }catch (e: Exception){
            Log.e("Bitmap resize", "Error: ${e.message}")
            null
        }
    }

    private fun isValidImageData(imageData: ByteArray?): Boolean {
        return imageData != null &&
                imageData.isNotEmpty() &&
                imageData.size > 100 // Minimo 100 bytes per un'immagine valida
    }

    override fun onResume() {
        super.onResume()
        if(checkLocationPermission() && ::mMap.isInitialized){
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        if(::fusedLocationClient.isInitialized){
            stopLocationUpdates()
        }
    }
}