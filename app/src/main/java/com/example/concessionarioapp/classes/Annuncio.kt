package com.example.concessionarioapp.classes

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Annuncio(
    var id: String = "",
    val titolo: String = "",
    val descrizione: String = "",
    val prezzo: Double = 0.0,
    val anno: Int = 0,
    val cilindrata: Int = 0,
    val chilometraggio: Int = 0,
    val carburante: String = "",
    val cambio: String = "",
    val userId: String = "",
    @ServerTimestamp
    val dataCreazione: Date? = null,
    val likes: Int = 0,
    val likedBy: MutableList<String> = mutableListOf(),
    val imageUrl: String = ""
)