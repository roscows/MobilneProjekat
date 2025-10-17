package com.example.ironlink.data

import kotlinx.serialization.Serializable

@Serializable  // Ako koristiš serialization, inače ukloni
data class LocationData(
    val userId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)