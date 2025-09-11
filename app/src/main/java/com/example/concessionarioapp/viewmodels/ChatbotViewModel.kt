package com.example.concessionarioapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.concessionarioapp.network.AIRequest
import com.example.concessionarioapp.network.Message
import com.example.concessionarioapp.network.RetrofitClient
import com.example.concessionarioapp.classes.Annuncio
import kotlinx.coroutines.launch

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val annuncio: Annuncio? = null
)

class ChatbotViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val aiService = RetrofitClient.instance
    private val conversationHistory = mutableListOf<ChatMessage>()

    // Lista degli annunci disponibili
    private var availableAnnunci = listOf<Annuncio>()

    // Callback per navigare al dettaglio annuncio
    private var onAnnuncioSelected: ((Annuncio) -> Unit)? = null

    init {
        // Messaggio di benvenuto
        val welcomeMessage = ChatMessage(
            content = "Ciao! Sono CarBot il tuo assistente virtuale per l'acquisto della tua prossima auto. Che tipo di auto stai cercando?",
            isUser = false
        )
        conversationHistory.add(welcomeMessage)
        _messages.value = conversationHistory.toList()
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.trim().isEmpty()) return

        val userChatMessage = ChatMessage(content = userMessage.trim(), isUser = true)
        conversationHistory.add(userChatMessage)
        _messages.value = conversationHistory.toList()

        // Mostra loading
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val matchResult = findBestMatchingAnnuncio(userMessage)

                // Crea solo messaggio di sistema + informazioni match
                val systemMessage = Message(
                    role = "system",
                    content = """Sei un assistente virtuale professionale di un concessionario auto italiano. 
                    Le tue caratteristiche:
                    - Rispondi sempre in italiano
                    - Sei cordiale, professionale e ONESTO
                    - Aiuti i clienti con domande su auto, finanziamenti, assicurazioni, manutenzione
                    - Mantieni le risposte concise ma informative (max 200 parole)
                    - NO ELENCHI PUNTATI
                    
                    IMPORTANTE - Comportamento ONESTO per suggerimenti auto:
                    
                    MATCH PERFETTO: Se l'auto corrisponde esattamente alla richiesta, sii entusiasta:
                    - "Perfetto! Ho trovato esattamente quello che cerchi!"
                    - Concludi con "Ti mostro l'auto che abbiamo trovato!"
                    
                    MATCH BUONO: Se l'auto è simile ma non identica, sii onesto ma positivo:
                    - "Non ho trovato esattamente quello che cercavi, ma ho questa interessante alternativa..."
                    - Spiega sempre le differenze e i punti di forza dell'alternativa
                    
                    MATCH SCARSO: Se l'auto è molto diversa, sii completamente onesto:
                    - "Al momento non abbiamo quello che stai cercando, ma ho questa opzione disponibile..."
                    - Spiega sempre e chiaramente le differenze
                    - Suggerisci di contattare un consulente per altre opzioni
                    
                    NESSUN MATCH: Sii onesto e utile:
                    - "Mi dispiace, al momento non abbiamo annunci che corrispondono alla tua ricerca"
                    - Suggerisci di parlare con un consulente o di modificare i criteri
                    
                    NON mentire mai sulla corrispondenza tra richiesta e auto trovata!
                """
                )

                val updatedMessages = listOf(systemMessage) + listOf(
                    Message(
                        role = "user",
                        content = when (matchResult.matchQuality) {
                            MatchQuality.EXCELLENT -> {
                                """
                                MATCH PERFETTO: Ho trovato un'auto che corrisponde ESATTAMENTE alla richiesta dell'utente.
                
                                Auto trovata: ${matchResult.annuncio!!.titolo}
                                Anno: ${matchResult.annuncio.anno}
                                Chilometraggio: ${matchResult.annuncio.chilometraggio} km
                                Carburante: ${matchResult.annuncio.carburante}
                                Cambio: ${matchResult.annuncio.cambio}
                                Potenza: ${matchResult.annuncio.cv} CV
                                Prezzo: €${matchResult.annuncio.prezzo}
                                
                                Presenta questa auto in modo entusiasta se corrisponde perfettamente alla richiesta.
                                
                                SE TI ARRIVA UN ANNUNCIO CHE PRESENTA DEGLI ELEMENTI NON CONFORMI ALLA RICHIESTA DELL'UTENTE MA TI É ARRIVATO ANALIZZA LE DIFFERENZE E 
                                SPIEGA PER BENE PERCHE LA STAI PRESENTANDO COMUNQUE.
                                
                                RICORDA CHE OGNI AUTO CHE RICEVI TU LA PRESENTERAI IN UN MODO O NELL'ALTRO
                                
                                Messaggio originale dell'utente: "$userMessage"
                                """
                            }
                            MatchQuality.GOOD -> {
                                """
                                MATCH BUONO: Ho trovato un'auto simile ma non identica alla richiesta dell'utente.
                
                                Auto trovata: ${matchResult.annuncio!!.titolo}
                                Anno: ${matchResult.annuncio.anno}
                                Chilometraggio: ${matchResult.annuncio.chilometraggio} km
                                Carburante: ${matchResult.annuncio.carburante}
                                Cambio: ${matchResult.annuncio.cambio}
                                Potenza: ${matchResult.annuncio.cv} CV
                                Prezzo: €${matchResult.annuncio.prezzo}
                                
                                Spiega sempre e chiaramente che non è esattamente quello che cercava, ma proponi questa alternativa valida.
                                
                                Messaggio originale dell'utente: "$userMessage"
                                """
                            }
                            MatchQuality.POOR -> {
                                """
                                MATCH SCARSO: Ho trovato un'auto ma non è molto simile a quello che cerca l'utente.
                
                                Auto trovata: ${matchResult.annuncio!!.titolo}
                                Anno: ${matchResult.annuncio.anno}
                                Chilometraggio: ${matchResult.annuncio.chilometraggio} km
                                Carburante: ${matchResult.annuncio.carburante}
                                Cambio: ${matchResult.annuncio.cambio}
                                Potenza: ${matchResult.annuncio.cv} CV
                                Prezzo: €${matchResult.annuncio.prezzo}
                                
                                Sii onesto: ammetti che non è quello che cercava, ma proponi questa come opzione disponibile.
                                
                                Messaggio originale dell'utente: "$userMessage"
                                """
                            }
                            MatchQuality.NONE -> {
                                """
                                NESSUN MATCH: Non abbiamo auto che corrispondono alla richiesta dell'utente.
                        
                                Comunica onestamente che al momento non abbiamo quello che cerca.
                                Suggerisci di contattare un consulente o di modificare i criteri di ricerca.
                                            
                                Messaggio originale dell'utente: "$userMessage"
                                """
                            }
                        }
                    )
                )

                val response = aiService.getCompletion(
                    AIRequest(
                        model = "gpt-4o-mini",
                        messages = updatedMessages,
                        maxTokens = 300,
                        temperature = 0.7
                    )
                )

                if (response.isSuccessful) {
                    val aiResponse = response.body()
                    val botMessage = aiResponse?.choices?.firstOrNull()?.message?.content
                        ?: "Mi dispiace, non sono riuscito a elaborare la tua richiesta."

                    val botChatMessage = ChatMessage(
                        content = botMessage.trim(),
                        isUser = false,
                        annuncio = matchResult.annuncio
                    )
                    conversationHistory.add(botChatMessage)
                    _messages.value = conversationHistory.toList()
                } else {
                    handleApiError(response.code())
                }
            } catch (e: Exception) {
                _error.value = "Si è verificato un errore: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleApiError(errorCode: Int) {
        val errorMessage = when (errorCode) {
            401 -> "API Key non valida. Controlla le tue credenziali."
            429 -> "Troppe richieste. Riprova tra qualche minuto."
            500 -> "Errore del server OpenAI. Riprova più tardi."
            else -> "Errore nella comunicazione: $errorCode"
        }
        _error.value = errorMessage
    }

    enum class MatchQuality {
        EXCELLENT,  // Match perfetto
        GOOD,       // Match buono ma non perfetto
        POOR,       // Match scarso
        NONE        // Nessun match
    }

    data class MatchResult(
        val annuncio: Annuncio?,
        val matchQuality: MatchQuality,
        val score: Int
    )

    private val fuelTypes = listOf("diesel", "benzina", "elettrica", "ibrida", "gasolio")
    private val kmWords = listOf("km", "chilometri", "chilometraggio")
    private val userKmRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:mila\s*)?(?:k?m|chilometr[io])""")
    private val yearRegex = Regex("""\b(20[0-2]\d)\b(?!\d)(?!\s*(?:euro|€|mila|km))""")
    private fun extractValidYears(userMessage: String): List<Int> {
        val userLower = userMessage.lowercase()

        // Trova tutti i potenziali anni
        val potentialYears = yearRegex.findAll(userLower)
            .map { it.value.toInt() }
            .filter { it in 1980..2030 } // Solo anni ragionevoli per auto
            .toList()

        // Filtra quelli che sono chiaramente parte di prezzi/km
        return potentialYears.filter { year ->
            val yearStr = year.toString()
            // Controlla se l'anno è seguito da indicatori di prezzo/km
            val isPriceOrKm = Regex("""${yearStr}\s*(?:euro|€|km|chilometr|mila)""")
                .containsMatchIn(userLower)
            !isPriceOrKm
        }
    }
    private val priceRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:euro|€|mila\s*euro)""")

    // Categorie di auto che spesso appaiono nelle descrizioni
    private val carCategories = listOf("suv", "sportiva", "berlina", "station", "wagon", "sw",
        "citycar", "utilitaria", "crossover", "cabrio", "coupe",
        "familiare", "monovolume", "pickup")

    private fun findBestMatchingAnnuncio(userMessage: String): MatchResult {

        if (availableAnnunci.isEmpty()) {
            return MatchResult(null, MatchQuality.NONE, 0)
        }

        val userLower = userMessage.lowercase()
        var bestMatch: Annuncio? = null
        var bestScore = 0

        for (annuncio in availableAnnunci) {
            var score = 0
            val titoloLower = annuncio.titolo.lowercase()
            val descrizioneText = "${annuncio.descrizione} ${annuncio.anno} ${annuncio.chilometraggio} ${annuncio.carburante} ${annuncio.cambio} ${annuncio.cv} ${annuncio.prezzo}".lowercase()

            // LOGICA PRINCIPALE: Parole nel titolo valgono MOLTO di più
            val userWords = userLower.split(Regex("\\s+")).filter {
                it.isNotEmpty() && it != "del" && it != "di" && it != "con" &&
                        it != "una" && it != "un" && it != "che" && it != "auto" &&
                        it.length > 2
            }

            userWords.forEach { word ->
                when {
                    // Parola nel titolo = punteggio alto
                    titoloLower.contains(word) -> score += 40
                    // Se è una categoria di auto e appare nella descrizione = punteggio alto
                    carCategories.contains(word) && descrizioneText.contains(word) -> score += 36
                }
            }

            // LOGICA CHILOMETRI
            val hasKmReference = kmWords.any { userLower.contains(it) }
            if (hasKmReference) {
                val match = userKmRegex.find(userLower)
                if (match != null) {
                    val kmValue = match.groupValues[1].toDoubleOrNull()
                    if (kmValue != null) {
                        val actualKm = if (userLower.contains("mila")) (kmValue * 1000).toInt() else kmValue.toInt()
                        if (annuncio.chilometraggio <= actualKm) {
                                score += 35
                        }
                    }
                }
            }

            // LOGICA CARBURANTE
            fuelTypes.forEach { fuel ->
                if (userLower.contains(fuel) && (titoloLower.contains(fuel) || descrizioneText.contains(fuel))) {
                        score += 35
                }
            }

            // LOGICA ANNI - con penalità per anni molto diversi
            val userYears = extractValidYears(userMessage)
            println("DEBUG - Anni validi trovati: $userYears")
            userYears.forEach { year ->
                when {
                    annuncio.anno == year -> score += 35
                    kotlin.math.abs(annuncio.anno - year) <= 1 -> score += 6
                    annuncio.anno > year -> score -= 5
                }
            }

            // LOGICA PREZZO
            val priceMatches = priceRegex.findAll(userLower)
            priceMatches.forEach { match ->
                val priceValue = match.groupValues[1].toDoubleOrNull()
                if (priceValue != null) {
                    val actualPrice = if (userLower.contains("mila")) (priceValue * 1000) else priceValue

                    if (annuncio.prezzo <= actualPrice) {
                        score += 35
                    } else {
                        // Calcola quanto supera il budget (in percentuale)
                        val percentageOver = ((annuncio.prezzo - actualPrice) / actualPrice) * 100

                        when {
                            percentageOver <= 150-> score -= 5
                            percentageOver <= 200 -> score -= 10
                            percentageOver <= 300 -> score -= 15
                            else -> score -= 25               // Supera di molto = penalità massima
                        }

                        println("DEBUG - Prezzo annuncio: ${annuncio.prezzo}, Budget: $actualPrice")
                        println("DEBUG - Supera budget del: ${percentageOver.toInt()}%")
                        println("DEBUG - Penalità applicata: ${if (percentageOver <= 15) -5 else if (percentageOver <= 40) -15 else if (percentageOver <= 60) -25 else -35}")
                    }
                }
            }

            if (score > bestScore) {
                bestScore = score
                bestMatch = annuncio
            }
        }

        // Debug logging
        bestMatch?.let { annuncio ->
            println("DEBUG - Match trovato: ${annuncio.titolo}")
            println("DEBUG - Score: $bestScore")
            println("DEBUG - Richiesta utente: $userMessage")
        }

        val matchQuality = when {
            bestScore >= 35 -> MatchQuality.EXCELLENT
            bestScore >= 15 -> MatchQuality.GOOD
            bestScore >= 8  -> MatchQuality.POOR
            else -> MatchQuality.NONE
        }

        return MatchResult(bestMatch, matchQuality, bestScore)
    }

    fun clearError() {
        _error.value = null
    }

    fun setAnnunci(annunci: List<Annuncio>) {
        availableAnnunci = annunci
    }

    fun setOnAnnuncioSelectedCallback(callback: (Annuncio) -> Unit) {
        onAnnuncioSelected = callback
    }

    fun onAnnuncioClicked(annuncio: Annuncio) {
        onAnnuncioSelected?.invoke(annuncio)
    }
}