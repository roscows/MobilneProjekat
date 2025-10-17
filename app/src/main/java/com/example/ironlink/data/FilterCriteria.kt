package com.example.ironlink.data

import com.google.firebase.Timestamp

data class FilterCriteria(
    val partnerName: String? = null,
    val type: String? = null,
    val dateRange: Pair<Timestamp?, Timestamp?> = Pair(null, null),
    val radius: Float? = null
)