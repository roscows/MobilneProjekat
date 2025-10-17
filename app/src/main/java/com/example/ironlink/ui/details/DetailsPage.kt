package com.example.ironlink.ui.details

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ironlink.data.Rate
import com.example.ironlink.data.TrainingPartner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsPage(partnerId: String, navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var partner by remember { mutableStateOf<TrainingPartner?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedRating by remember { mutableIntStateOf(0) }
    var averageRating by remember { mutableStateOf<Double?>(null) }
    var userHasRated by remember { mutableStateOf(false) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var ratings by remember { mutableStateOf<List<Rate>>(emptyList()) }

    LaunchedEffect(partnerId) {
        coroutineScope.launch {
            try {
                val docSnapshot = firestore.collection("training_partners")
                    .document(partnerId)
                    .get().await()

                if (docSnapshot.exists()) {
                    partner = docSnapshot.toObject(TrainingPartner::class.java)

                    val ratingsSnapshot = firestore.collection("rates")
                        .whereEqualTo("partnerId", partnerId)
                        .get().await()

                    ratings = ratingsSnapshot.documents.mapNotNull { it.toObject(Rate::class.java) }

                    if (ratings.isNotEmpty()) {
                        averageRating = ratings.map { it.value.toDouble() }.average()
                    }

                    userHasRated = ratings.any { it.userId == userId }
                } else {
                    errorMessage = "Activity not found!"
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load details!"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    )
                }
                else -> {
                    partner?.let { details ->
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = details.name ?: "Activity",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(text = "Type: ${details.type}", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Description: ${details.description}", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Contact: ${details.phone ?: "Not provided"}", style = MaterialTheme.typography.bodyLarge)

                            Spacer(modifier = Modifier.height(16.dp))

                            details.eventTimestamp?.let {
                                Text(
                                    "Time of activity: ${it.toDate().toFormattedString()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            val decimalFormat = DecimalFormat("#.##")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Average Rating: ", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = decimalFormat.format(averageRating ?: 0.0),
                                    color = getRatingColor(averageRating ?: 0.0),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (!userHasRated && userId != null) {
                                StarRatingBar(
                                    currentRating = selectedRating,
                                    onRatingChanged = { rating ->
                                        selectedRating = rating
                                    },
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (selectedRating > 0) { // Onemogući slanje ocene 0
                                            coroutineScope.launch {
                                                try {
                                                    val rate = Rate(
                                                        userId = userId,
                                                        partnerId = partnerId,
                                                        value = selectedRating,
                                                    )
                                                    firestore.collection("rates")
                                                        .document("${userId}_$partnerId")
                                                        .set(rate).await()

                                                    userHasRated = true

                                                    averageRating = if (averageRating == null) {
                                                        selectedRating.toDouble()
                                                    } else {
                                                        ((averageRating!! * ratings.size) + selectedRating) / (ratings.size + 1)
                                                    }

                                                    firestore.collection("users").document(userId)
                                                        .update("points", FieldValue.increment(5))
                                                } catch (e: Exception) {
                                                    Log.e("Firestore", "Failed to add rating", e)
                                                }
                                            }
                                        }
                                    },
                                ) {
                                    Text("Submit Rating")
                                }
                            } else if (userId != null) {
                                Text("You have already rated this activity.")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StarRatingBar(
    currentRating: Int,
    onRatingChanged: (Int) -> Unit,
    maxStars: Int = 5,
) {
    Row {
        for (i in 1..maxStars) {
            Icon(
                imageVector = if (i <= currentRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Star $i",
                tint = if (i <= currentRating) Color(0xFFFFC107) else Color.Gray,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onRatingChanged(i) },
            )
        }
    }
}

fun getRatingColor(rating: Double): Color {
    return when {
        rating >= 4.0 -> Color(0xFF4CAF50) // зелена
        rating >= 2.5 -> Color(0xFFFFC107) // жута
        else -> Color(0xFFF44336) // црвена
    }
}

private fun Date.toFormattedString(): String {
    val format = SimpleDateFormat("dd.MM.yyyy 'at' HH:mm", Locale.getDefault())
    return format.format(this)
}