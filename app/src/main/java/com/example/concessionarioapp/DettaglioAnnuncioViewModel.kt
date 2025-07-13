package com.example.concessionarioapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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

    private val firestore = FirebaseFirestore.getInstance()

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
                    val venditore = document.toObject(User::class.java)
                    println("DEBUG: Venditore caricato - ID: ${venditore?.id}, Nome: ${venditore?.nome}, Email: ${venditore?.email}")
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
                val venditoreDefault = User(
                    id = userId,
                    nome = "Utente non trovato",
                    email = "Contatto non disponibile",
                    telefono = null
                )
                _venditore.value = venditoreDefault
            }
    }

}