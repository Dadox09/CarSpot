package com.example.concessionarioapp

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AnnunciViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // LiveData per la lista di annunci
    private val _annunci = MutableLiveData<List<Annuncio>>()
    val annunci: LiveData<List<Annuncio>> = _annunci

    // LiveData per eventuali messaggi all'utente (es. errori, info)
    private val _messaggio = MutableLiveData<String?>()
    val messaggio: LiveData<String?> = _messaggio

    fun caricaAnnunci(userId: String? = null) {
        var query: Query = db.collection("annunci")

        // Se viene fornito un userId, filtra la query
        if (userId != null) {
            query = query.whereEqualTo("userId", userId)
        }

        query.orderBy("dataCreazione", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val listaAnnunci = documents.toObjects(Annuncio::class.java)
                _annunci.value = listaAnnunci

            }
            .addOnFailureListener { exception ->
                _messaggio.value = "Errore caricamento: ${exception.message}"
            }
    }

}
