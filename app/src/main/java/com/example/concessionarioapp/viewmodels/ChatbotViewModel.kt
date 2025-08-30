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
                val suggestedAnnuncio = findBestMatchingAnnuncio(userMessage)

                val updatedMessages = buildApiMessages() + listOf(
                    Message(
                        role = "user",
                        content = if (suggestedAnnuncio != null) {
                                """
                                    ANNUNCIO TROVATO: Ho trovato un'auto compatibile con la richiesta dell'utente.
                    
                                    Auto trovata: ${suggestedAnnuncio.titolo}
                                    Anno: ${suggestedAnnuncio.anno}
                                    Chilometraggio: ${suggestedAnnuncio.chilometraggio} km
                                    Carburante: ${suggestedAnnuncio.carburante}
                                    Cambio: ${suggestedAnnuncio.cambio}
                                    Potenza: ${suggestedAnnuncio.cv} CV
                                    Prezzo: €${suggestedAnnuncio.prezzo}
                                    
                                    Presenta questa auto in modo entusiasta e positivo all'utente.
                                    
                                    Messaggio originale dell'utente: "$userMessage"
                                """
                            } else {
                                    """NESSUN ANNUNCIO TROVATO: Non abbiamo auto che corrispondono alla richiesta dell'utente.
                            
                                    Comunica all'utente che al momento non abbiamo annunci compatibili con la sua ricerca.
                                    Suggerisci di contattare un consulente o di modificare i criteri di ricerca.
                                                
                                    Messaggio originale dell'utente: "$userMessage"
                                    """
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

                    //Crea il messaggio del bot con l'annuncio già trovato
                    val botChatMessage = ChatMessage(
                        content = botMessage.trim(),
                        isUser = false,
                        annuncio = suggestedAnnuncio
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

    private val commonBrands = listOf(
        "fiat", "ford", "bmw", "mercedes", "audi", "volkswagen", "toyota",
        "honda", "nissan", "renault", "peugeot", "citroen", "opel", "kia",
        "hyundai", "alfa romeo", "jeep", "land rover", "porsche", "ferrari",
        "lamborghini", "maserati", "tesla", "subaru", "mazda", "lexus",
        "mini", "skoda", "seat", "volvo", "smart", "dacia", "mitsubishi"
    )

    private val fuelTypes = listOf("diesel", "benzina", "elettrica", "ibrida", "gasolio")
    private val kmWords = listOf("km", "chilometri", "chilometraggio")
    private val userKmRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:mila\s*)?(?:k?m|chilometr[io])""")

    private val yearRegex = Regex("""20\d{2}""") // Trova anni dal 2000 al 2099
    private val priceRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:euro|€|mila\s*euro)""") // Trova prezzi

    private fun findBestMatchingAnnuncio(userMessage: String): Annuncio? {
        if (availableAnnunci.isEmpty()) return null

        val userLower = userMessage.lowercase()

        var bestMatch: Annuncio? = null
        var bestScore = 0

        for (annuncio in availableAnnunci) {
            var score = 0
            val annuncioText = "${annuncio.titolo} ${annuncio.descrizione} ${annuncio.anno} ${annuncio.chilometraggio} ${annuncio.carburante} ${annuncio.cambio} ${annuncio.cv} ${annuncio.prezzo}".lowercase()
            // Scoring per parole generiche
            val userWords = userLower.split(Regex("\\s+")).filter { it.isNotEmpty() && it != "del" && it != "di" && it != "con" }
            userWords.forEach { word ->
                if (annuncioText.contains(word)) {
                    score += 2
                }
            }

            // Logica chilometri
            val hasKmReference = kmWords.any { userLower.contains(it) && annuncioText.contains(it) }
            if (hasKmReference) {
                val match = userKmRegex.find(userLower)
                if (match != null) {
                    val kmValue = match.groupValues[1].toDoubleOrNull()
                    if (kmValue != null) {
                        val actualKm = if (userLower.contains("mila")) (kmValue * 1000).toInt() else kmValue.toInt()
                        if (annuncio.chilometraggio <= actualKm) {
                            score += 5
                        }
                    }
                }
            }

            // Logica brand
            commonBrands.forEach { brand ->
                if (userLower.contains(brand) && annuncioText.contains(brand)) {
                    score += 5
                }
            }

            // Logica carburante
            fuelTypes.forEach { fuel ->
                if (userLower.contains(fuel) && annuncioText.contains(fuel)) {
                    score += 3
                }
            }

            //  Matching degli anni
            val userYears = yearRegex.findAll(userLower).map { it.value.toInt() }.toList()
            userYears.forEach { year ->
                if (annuncio.anno == year) {
                    score += 6 // Bonus alto per anno esatto
                } else if (kotlin.math.abs(annuncio.anno - year) <= 1) {
                    score += 3 // Bonus medio per anni vicini (±1 anno)
                }
            }

            //Matching del prezzo
            val priceMatches = priceRegex.findAll(userLower)
            priceMatches.forEach { match ->
                val priceValue = match.groupValues[1].toDoubleOrNull()
                if (priceValue != null) {
                    val actualPrice = if (userLower.contains("mila")) (priceValue * 1000) else priceValue

                    if (annuncio.prezzo <= actualPrice) {
                        score += 4 // Bonus per prezzo compatibile
                    }else if (kotlin.math.abs(annuncio.prezzo - actualPrice) <= 1000) {
                        score += 2 // Bonus medio per prezzo vicino
                    }
                }
            }

            //Bonus per match esatti di parole chiave importanti
            if (userLower.contains("usata") && annuncioText.contains("usata")) {
                score += 2
            }
            if (userLower.contains("nuova") && annuncioText.contains("nuova")) {
                score += 2
            }

            if (score > bestScore) {
                bestScore = score
                bestMatch = annuncio
            }
        }

        // MIGLIORAMENTO: Soglia più bassa per essere meno restrittivi
        return if (bestScore >= 2) bestMatch else null
    }

    fun clearError() {
        _error.value = null
    }

    private fun buildApiMessages(): List<Message> {
        val systemMessage = Message(
            role = "system",
            content = """Sei un assistente virtuale professionale di un concessionario auto italiano. 
            Le tue caratteristiche:
            - Rispondi sempre in italiano
            - Sei cordiale, professionale e competente
            - Aiuti i clienti con domande su auto, finanziamenti, assicurazioni, manutenzione
            - Puoi fornire consigli per l'acquisto di auto nuove e usate
            - Conosci le principali marche automobilistiche e i loro modelli
            - Mantieni le risposte concise ma informative (max 200 parole)
            - NO ELENCHI PUNTATI
            
            IMPORTANTE - Comportamento per suggerimenti auto:
            - Se ti viene comunicato che è stato trovato un annuncio specifico, presentalo SEMPRE come una soluzione positiva
            - Usa frasi come "Perfetto! Ho trovato esattamente quello che cerchi" o "Abbiamo questa fantastica auto che fa al caso tuo"
            - Quando suggerisci un'auto specifica disponibile, concludi sempre con "Ti mostro l'auto che abbiamo trovato!"
            - SOLO se ti viene esplicitamente comunicato che non ci sono annunci compatibili, allora rispondi "Mi dispiace, al momento non abbiamo annunci che corrispondono esattamente alla tua ricerca"
            - Se l'auto trovata è simile ma non esattamente quella richiesta, presentala comunque positivamente: "Ho trovato questa interessante alternativa che potrebbe piacerti"
            
            - Se non sai qualcosa, ammettilo onestamente e suggerisci di parlare con un consulente
            - Mantieni la conversazione. Se l'utente ti saluta o non ti chiede niente di pertinente continua la conversazione dicendo di chiederti qualcosa.
        """
        )

        val conversationMessages = conversationHistory.takeLast(8).map { chatMessage ->
            Message(
                role = if (chatMessage.isUser) "user" else "assistant",
                content = chatMessage.content
            )
        }

        return listOf(systemMessage) + conversationMessages
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