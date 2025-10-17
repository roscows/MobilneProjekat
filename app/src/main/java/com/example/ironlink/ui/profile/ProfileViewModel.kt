package com.example.ironlink.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ironlink.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Definišemo stanja UI-ja
sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val user: User) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // StateFlow koji će Composable da "sluša"
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    init {
        // Čim se ViewModel kreira, pokreni učitavanje podataka
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            // Proveri da li je korisnik uopšte ulogovan
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _profileState.value = ProfileState.Error("User not logged in.")
                return@launch
            }

            try {
                // Pokušaj da dohvatiš dokument korisnika iz 'users' kolekcije
                val userDoc = db.collection("users").document(currentUser.uid).get().await()

                if (userDoc.exists()) {
                    // Ako dokument postoji, pretvori ga u naš User objekat
                    val user = userDoc.toObject(User::class.java)
                    if (user != null) {
                        _profileState.value = ProfileState.Success(user)
                    } else {
                        _profileState.value = ProfileState.Error("Failed to parse user data.")
                    }
                } else {
                    _profileState.value = ProfileState.Error("User data not found in database.")
                }
            } catch (e: Exception) {
                // Uhvati bilo koju grešku (npr. nema interneta)
                _profileState.value = ProfileState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }
}