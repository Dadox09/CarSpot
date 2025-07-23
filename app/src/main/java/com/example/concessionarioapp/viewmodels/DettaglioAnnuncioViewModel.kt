package com.example.concessionarioapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.concessionarioapp.classes.Annuncio
import com.example.concessionarioapp.classes.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

    fun caricaDettagliAnnuncio(annuncioId: String) {
        // Prendo i dati relativi all'annuncio
        _isLoading.value = true
        _error.value = null
        firestore.collection("annunci")
            .document(annuncioId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.data != null) {
                    val annuncio = document.toObject(Annuncio::class.java)
                    _annuncio.value = annuncio

                    // Controlla se l'utente corrente può eliminare l'annuncio
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
        // Prendo i dati relativi al venditore
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.data != null) {
                    // Mapping manuale per evitare problemi
                    val venditore = User(
                        id = document.getString("id") ?: userId,
                        nome = document.getString("nome") ?: "Nome non disponibile",
                        email = document.getString("email") ?: "Email non disponibile",
                        telefono = document.getString("telefono")
                    )

                    println("DEBUG: Venditore creato manualmente - ID: ${venditore.id}, Nome: ${venditore.nome}, Email: ${venditore.email}")
                    _venditore.value = venditore
                } else {
                    val venditoreDefault = User(
                        id = userId,
                        nome = "Utente non trovato",
                        email = "Contatto non disponibile",
                        telefono = null
                    )
                    _venditore.value = venditoreDefault
                }
            }
            .addOnFailureListener { exception ->
                println("DEBUG: Errore nel caricamento venditore: ${exception.message}")
                val venditoreDefault = User(
                    id = userId,
                    nome = "Utente non trovato",
                    email = "Contatto non disponibile",
                    telefono = null
                )
                _venditore.value = venditoreDefault
            }
    }

    fun eliminaAnnuncio(annuncioId: String) {
        // Elimina l'annuncio
        _isLoading.value = true
        _error.value = null

        firestore.collection("annunci")
            .document(annuncioId)
            .delete()
            .addOnSuccessListener {
                _isLoading.value = false
                _deleteSuccess.value = true
            }
            .addOnFailureListener { exception ->
                _error.value = "Errore nell'eliminazione: ${exception.message}"
                _isLoading.value = false
            }
    }
}