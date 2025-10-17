package com.example.ironlink.data

data class User(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val points: Long = 0
)