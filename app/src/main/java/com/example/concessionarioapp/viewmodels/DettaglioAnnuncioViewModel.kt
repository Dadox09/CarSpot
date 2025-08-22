package com.example.concessionarioapp.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.concessionarioapp.classes.Annuncio
import com.example.concessionarioapp.classes.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DettaglioAnnuncioViewModel : ViewModel() {

    private val _annuncio = MutableLiveData<Annuncio?>()
    val annuncio: LiveData<Annuncio?> = _annuncio

    private val _venditore = MutableLiveData<User?>()
    val venditore: LiveData<User?> = _venditore

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _canDelete = MutableLiveData<Boolean>()
    val canDelete: LiveData<Boolean> = _canDelete

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun caricaDettagliAnnuncio(annuncioId: String) {
        _isLoading.value = true
        _error.value = null
        firestore.collection("annunci")
            .document(annuncioId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.data != null) {
                    val annuncio = document.toObject(Annuncio::class.java)
                    _annuncio.value = annuncio

                    val currentUserId = auth.currentUser?.uid
                    _canDelete.value = currentUserId != null && currentUserId == annuncio?.userId

                    annuncio?.userId?.let { caricaDatiVenditore(it) }
                } else {
                    _error.value = "Annuncio non trovato"
                }
                _isLoading.value = false
            }
            .addOnFailureListener { exception ->
                _error.value = "Errore caricamento: ${exception.message}"
                _isLoading.value = false
            }
    }

    private fun caricaDatiVenditore(userId: String) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.data != null) {
                    val venditore = User(
                        id = document.getString("id") ?: userId,
                        nome = document.getString("nome") ?: "Nome non disponibile",
                        email = document.getString("email") ?: "Email non disponibile",
                        telefono = document.getString("telefono")
                    )
                    Log.d("DettaglioAnnuncioViewModel", "Venditore caricato: ${venditore.email}")
                    _venditore.value = venditore
                } else {
                    _venditore.value = User(
                        id = userId,
                        nome = "Utente non trovato",
                        email = "Contatto non disponibile",
                        telefono = null
                    )
                }
            }
            .addOnFailureListener { exception ->
                Log.e("DettaglioAnnuncioViewModel", "Errore nel caricamento venditore", exception)
                _venditore.value = User(
                    id = userId,
                    nome = "Utente non trovato",
                    email = "Contatto non disponibile",
                    telefono = null
                )
            }
    }

    fun eliminaAnnuncio(annuncioId: String) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Get the ad data to find the image URL
                val annuncioDoc = firestore.collection("annunci").document(annuncioId).get().await()
                val annuncioData = annuncioDoc.toObject(Annuncio::class.java)

                val imageUrls = annuncioData?.immagini ?: emptyList()
                if (imageUrls.isNotEmpty()) {
                    // Delete the first image from Firebase Storage
                    val imageUrl = imageUrls.first()
                    deleteImageFromStorage(imageUrl)
                }

                // Delete the Firestore document
                firestore.collection("annunci").document(annuncioId).delete().await()

                _deleteSuccess.value = true
            } catch (e: Exception) {
                Log.e("DettaglioAnnuncioViewModel", "Errore nell'eliminazione dell'annuncio", e)
                _error.value = "Errore nell'eliminazione: ${e.message}"
                _deleteSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun deleteImageFromStorage(imageUrl: String) {
        return withContext(Dispatchers.IO) {
            try {
                val imageRef = storage.getReferenceFromUrl(imageUrl)
                imageRef.delete().await()
                Log.d("DettaglioAnnuncioViewModel", "Immagine eliminata con successo: $imageUrl")
            } catch (e: Exception) {
                // Log the error but don't re-throw it.
                // We want to continue and delete the ad from Firestore even if the image delete fails.
                Log.e("DettaglioAnnuncioViewModel", "Errore durante l'eliminazione dell'immagine: $imageUrl", e)
            }
        }
    }
}