package com.example.ironlink.ui.location

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location as AndroidLocation
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ironlink.data.LocationData
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val db = FirebaseFirestore.getInstance()
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location: AndroidLocation ->
                val latLng = LatLng(location.latitude, location.longitude)
                _currentLocation.value = latLng

                // Pošalji u Firestore ako je userId dostupan
                userId?.let { uid ->
                    viewModelScope.launch {
                        try {
                            val locData = LocationData(
                                userId = uid,
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                            db.collection("locations").document(uid).set(locData).await()
                        } catch (e: Exception) {
                            e.printStackTrace() // ili loguj po želji
                        }
                    }
                }
            }
        }
    }

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return // Zahtevaj dozvolu u UI
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000L)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: AndroidLocation? ->
                location?.let {
                    _currentLocation.value = LatLng(it.latitude, it.longitude)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
