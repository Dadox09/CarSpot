package com.example.concessionarioapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.concessionarioapp.classes.Annuncio
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AnnunciViewModel : ViewModel() {

    //connetto a firestore
    private val db = FirebaseFirestore.getInstance()

    // LiveData per la lista di annunci
    private val _annunci = MutableLiveData<List<Annuncio>>()
    val annunci: LiveData<List<Annuncio>> = _annunci

    // LiveData per eventuali messaggi all'utente (es. errori, info)
    private val _messaggio = MutableLiveData<String?>()
    val messaggio: LiveData<String?> = _messaggio

    fun caricaAnnunci(userId: String? = null) {
        var query: Query = db.collection("annunci")

        // Se viene fornito un userId, filtra la query e prendo solo gli annunci dell'utente per VendiFragment
        if (userId != null) {
            query = query.whereEqualTo("userId", userId)
        }

        // Ordina gli annunci per data di creazione in ordine decrescente
        query.orderBy("dataCreazione", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val listaAnnunci = documents.toObjects(Annuncio::class.java).map { annuncio ->
                    if (annuncio.id.isEmpty()) {
                        annuncio.id = documents.find { it.toObject(Annuncio::class.java) == annuncio }?.id ?: ""
                    }
                    annuncio
                }
                _annunci.value = listaAnnunci
            }
            .addOnFailureListener { exception ->
                _messaggio.value = "Errore caricamento: ${exception.message}"
            }
    }

    fun toggleLike(annuncio: Annuncio) {
        if (annuncio.id.isEmpty()) {
            _messaggio.value = "Impossibile aggiornare il like: ID annuncio non valido"
            return
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            _messaggio.value = "Devi essere loggato per mettere like"
            return
        }

        val hasLiked = annuncio.likedBy.contains(currentUserId)

        val updatedLikedBy = annuncio.likedBy.toMutableList()
        val updatedLikes: Int

        if (hasLiked) {
            // Rimuovi il like
            updatedLikedBy.remove(currentUserId)
            updatedLikes = annuncio.likes - 1
        } else {
            // Aggiungi il like
            updatedLikedBy.add(currentUserId)
            updatedLikes = annuncio.likes + 1
        }

        // Crea una copia aggiornata dell'annuncio per l'aggiornamento locale
        val updatedAnnuncio = annuncio.copy(
            likes = updatedLikes,
            likedBy = updatedLikedBy
        )

        // Aggiorna immediatamente la UI
        updateLocalAnnuncio(updatedAnnuncio)

        // Aggiorna Firestore
        val updates = hashMapOf<String, Any>(
            "likes" to updatedLikes,
            "likedBy" to if (hasLiked) {
                FieldValue.arrayRemove(currentUserId)
            } else {
                FieldValue.arrayUnion(currentUserId)
            }
        )

        db.collection("annunci")
            .document(annuncio.id)
            .update(updates)
            .addOnFailureListener { exception ->
                // In caso di errore, ripristina lo stato precedente
                updateLocalAnnuncio(annuncio)
                _messaggio.value = "Errore nell'aggiornamento del like: ${exception.message}"
            }
    }

    private fun updateLocalAnnuncio(updatedAnnuncio: Annuncio) {
        val listaAggiornata = _annunci.value?.toMutableList() ?: mutableListOf()
        val index = listaAggiornata.indexOfFirst { it.id == updatedAnnuncio.id }
        if (index != -1) {
            listaAggiornata[index] = updatedAnnuncio
            _annunci.value = listaAggiornata
        }
    }

}