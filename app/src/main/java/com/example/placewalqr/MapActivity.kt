package com.example.placewalqr

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import com.example.kotlindemos.PermissionUtils
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment


class MapActivity: AppCompatActivity(), OnMyLocationButtonClickListener, OnMyLocationClickListener, OnMapReadyCallback, OnRequestPermissionsResultCallback {
    private var permissionDenied = false
    private lateinit var map: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.map_activity)

        val mapFragment=supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment ?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap){
        map=googleMap
        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)
        enableMyLocation()
    }

    private fun enableMyLocation(){
        //check if permission are granted and enble location layer
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            map.isMyLocationEnabled=true
            return
        }

        //if a permission rationale dialog should be shown
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) ||
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)){
            PermissionUtils.PermissionDeniedDialog.newInstance("LOCATION_PERMISSION_REQUEST_CODE", "true").show(supportFragmentManager, "dialog")
            //PermissionUtils.PermissionDeniedDialog.newInstance(LOCATION_PERMISSION_REQUEST_CODE, true).show(supportFragmentManager, "dialog")
            //DA VEDERE UN ATTIMO
            return
        }

        //otherwise request hte permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "Location button clicked", Toast.LENGTH_LONG).show()
        return false //false for not consuming the event
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this, "Current location: $location", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if(requestCode != LOCATION_PERMISSION_REQUEST_CODE){
            super<AppCompatActivity>.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        //enable location
        if(PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION) ||
            PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_COARSE_LOCATION)){
            enableMyLocation()
        }else{
            permissionDenied=true
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (permissionDenied){
            showMissingPermissionError()
            permissionDenied=false
        }
    }

    private fun showMissingPermissionError(){
        PermissionUtils.PermissionDeniedDialog.newInstance("true", "").show(supportFragmentManager, "dialog")
        //newInstance(true).show(supportFragmentManager, "dialog")
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE=1
    }
}