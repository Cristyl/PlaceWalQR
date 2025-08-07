package com.example.placewalqr

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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

class MapsActivity : BaseActivity(), OnMapReadyCallback {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialization client for localization
        fusedLocationClient= LocationServices.getFusedLocationProviderClient(this)

        //localization request initialization
        createLocationRequest()
        createLocationCallback()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
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
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        // val sydney = LatLng(-34.0, 151.0)
        // mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        // mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        //map configuration
        mMap.uiSettings.isZoomControlsEnabled=true
        mMap.uiSettings.isMyLocationButtonEnabled=true
        mMap.uiSettings.isCompassEnabled=true

        //check localization permission
        if(checkLocationPermission()){
            enableMyLocation()
        }else{
            requestLocationPermission()

        }
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
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission(){
        ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun onRequestPermissionResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray){
        super <BaseActivity>.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    enableMyLocation()
                }else{
                    Toast.makeText(this, "Required localization permission", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
                mMap.clear()
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM))
                updateLocationOnMap(location)
            }else{
                requestSingleLocationUpdate()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Impossible to get current position", Toast.LENGTH_SHORT).show()
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
                        mMap.clear()
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

    //keep for manually centering the current position
    private fun centerOnCurrentLocation(){
        if(!checkLocationPermission()){
            requestLocationPermission()
            return
        }
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