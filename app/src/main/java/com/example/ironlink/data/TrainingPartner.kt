package com.example.ironlink.data

import com.google.firebase.Timestamp

data class TrainingPartner(
    val id: String = "",
    val name: String? = null,
    val description: String? = null,
    val type: String? = null,
    val phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val userId: String? = null,
    val dateCreated: Timestamp? = null,
    val rating: Float? = 0f,
    val eventTimestamp: Timestamp? = null
)