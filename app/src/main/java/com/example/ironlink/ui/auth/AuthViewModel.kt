package com.example.ironlink.ui.auth

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.ironlink.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val email = "$username@ironlink.com"
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("No UID")

                val userDoc = db.collection("users").document(uid).get().await()
                val user = if (userDoc.exists()) {
                    userDoc.toObject(User::class.java)?.copy(id = uid) ?: throw Exception("User not found")
                } else {
                    throw Exception("User not registered")
                }

                _authState.value = AuthState.Success(user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(username: String, password: String, fullName: String, phone: String, photoUri: Uri?, context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val email = "$username@ironlink.com"
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("No UID")

                val photoUrl = photoUri?.let { uploadToCloudinary(it, context) }

                val user = User(
                    id = uid,
                    username = username,
                    fullName = fullName,
                    phone = phone,
                    photoUrl = photoUrl
                )
                db.collection("users").document(uid).set(user).await()

                _authState.value = AuthState.Success(user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Idle // Resetujemo stanje da bi se UI eventualno ažurirao
    }

    internal suspend fun uploadToCloudinary(uri: Uri, context: Context): String? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val uploadRequest = MediaManager.get().upload(uri).callback(object : UploadCallback {
                override fun onStart(filePath: String?) {
                    // Početak upload-a
                }

                override fun onProgress(filePath: String?, bytes: Long, totalBytes: Long) {
                    // Progress: bytes / totalBytes
                }

                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    val secureUrl = resultData?.get("secure_url") as? String
                    if (secureUrl != null) {
                        continuation.resume(secureUrl)
                    } else {
                        continuation.resumeWithException(Exception("Upload uspešan, ali nema URL-a"))
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    continuation.resumeWithException(Exception(error?.description ?: "Upload nije uspešan"))
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    // Ponovni pokušaj ako treba
                }
            })

            val requestId = uploadRequest.dispatch()  // dispatch vraća requestId kao String?

            // Otkaži upload ako se coroutine otkaže
            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
            }
        }
    }
}