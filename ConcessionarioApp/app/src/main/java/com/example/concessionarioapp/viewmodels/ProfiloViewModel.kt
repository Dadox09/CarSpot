package com.example.concessionarioapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfiloViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> = _userEmail

    private val _annunciAttivi = MutableLiveData<String>("0")
    val annunciAttivi: LiveData<String> = _annunciAttivi

    private val _dataRegistrazione = MutableLiveData<String>("Data non disponibile")
    val dataRegistrazione: LiveData<String> = _dataRegistrazione

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadUserData()
    }

    fun loadUserData() {
        _isLoading.value = true
        _errorMessage.value = null

        val user = auth.currentUser
        _currentUser.value = user

        if (user != null) {
            // Imposta l'email
            _userEmail.value = user.email ?: "Email non disponibile"

            // Imposta il nome utente (se disponibile, altrimenti usa l'email)
            _userName.value = user.displayName ?: user.email?.substringBefore('@') ?: "Utente"

            // Carica il numero di annunci attivi
            loadAnnunciAttivi(user.uid)

            // Imposta la data di registrazione
            val creationTime = user.metadata?.creationTimestamp
            if (creationTime != null) {
                val date = Date(creationTime)
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                _dataRegistrazione.value = format.format(date)
            }
        } else {
            _errorMessage.value = "Utente non autenticato"
        }

        _isLoading.value = false
    }

    private fun loadAnnunciAttivi(userId: String) {
        //solamente il numero per ora
        db.collection("annunci")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                _annunciAttivi.value = documents.size().toString()
            }
            .addOnFailureListener {
                _errorMessage.value = "Errore nel caricamento degli annunci"
                _annunciAttivi.value = "0"
            }
    }

    fun logout() {
        auth.signOut()
    }
}