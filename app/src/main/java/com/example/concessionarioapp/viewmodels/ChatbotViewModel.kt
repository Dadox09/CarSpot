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
            content = "Ciao! Sono il tuo assistente virtuale per il concessionario. Come posso aiutarti oggi? Posso rispondere a domande sulle auto che desideri comprare!",
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
                            "L'utente ha cercato un'auto. Ho trovato l'annuncio per una ${suggestedAnnuncio.titolo}. Suggerisci quest'auto all'utente. Messaggio originale: $userMessage"
                        } else {
                            "L'utente ha cercato un'auto, ma non abbiamo trovato annunci compatibili. Comunica all'utente che non abbiamo annunci per la sua richiesta. Messaggio originale: $userMessage"
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

    private fun findBestMatchingAnnuncio(userMessage: String): Annuncio? {
        if (availableAnnunci.isEmpty()) return null

        val userLower = userMessage.lowercase()

        var bestMatch: Annuncio? = null
        var bestScore = 0

        for (annuncio in availableAnnunci) {
            var score = 0
            val annuncioText = "${annuncio.titolo} ${annuncio.descrizione}".lowercase()

            // Scoring per parole generiche
            val userWords = userLower.split(" ").filter { it.length >= 3 }
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

            if (score > bestScore) {
                bestScore = score
                bestMatch = annuncio
            }
        }

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
                - Quando suggerisci un'auto specifica, concludi sempre con "Ti mostro un'auto che potrebbe interessarti!" SOLO se disponibile nel nostro inventario
                - Se non l'abbiamo nell'inventario allora rispondi con "Non abbiamo annunci compatibili con la tua richiesta"
                - Se invece abbiamo qualcosa di simile come la stessa marca ma non compatibile con il prezzo richiesto allora proponi un'alternativa.
                - Se non sai qualcosa, ammettilo onestamente e suggerisci di parlare con un consulente
                - Mantieni la conversazione. Se l'utente di saluta o non ti chiede niente di pertinente continua la conversazione dicendo di chiederti qualcosa.
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