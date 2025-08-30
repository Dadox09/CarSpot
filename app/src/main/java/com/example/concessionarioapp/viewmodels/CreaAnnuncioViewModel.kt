package com.example.concessionarioapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.concessionarioapp.network.AIRequest
import com.example.concessionarioapp.network.Message
import com.example.concessionarioapp.network.RetrofitClient
import com.google.gson.Gson
import android.util.Log
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class CreaAnnuncioViewModel : ViewModel() {

    private val _prezzoSuggerito = MutableLiveData<String?>()
    val prezzoSuggerito: LiveData<String?> = _prezzoSuggerito

    private val _descrizioneGenerata = MutableLiveData<String?>()
    val descrizioneGenerata: LiveData<String?> = _descrizioneGenerata

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun suggerisciPrezzo(datiAuto: Map<String, String>) {
        val prompt = """
            Sei un analista esperto del mercato automobilistico italiano. Il tuo compito è eseguire una valutazione dettagliata di un'auto usata e produrre il risultato in formato JSON.
            
            **Analisi Richiesta:**
            Effettua una valutazione passo-passo per determinare il prezzo di vendita più probabile tra privati.
            
            **Dati del Veicolo:**
            - Modello Completo (incluso allestimento): ${datiAuto["titolo"]}
            - Anno: ${datiAuto["anno"]}
            - Chilometraggio: ${datiAuto["chilometraggio"]} km
            - Carburante: ${datiAuto["carburante"]}
            - Cambio: ${datiAuto["cambio"]}
            - Cv: ${datiAuto["cv"]} cv
            - Stato Generale (descrizione o voto 1-10): ${datiAuto["stato"]} // Dato cruciale
            - Manutenzione (es. "regolare", "documentata"): ${datiAuto["manutenzione"]} // Dato utile
            
            **Procedura di Valutazione:**
            1.  **Stima del Valore Base:** Inizia con il valore di mercato medio per un veicolo identico (modello, anno, allestimento) con chilometraggio standard.
            2.  **Correzione Chilometraggio:** Applica un fattore di correzione negativo se il chilometraggio è superiore alla media di 15.000 km/anno, o positivo se è significativamente inferiore.
            3.  **Correzione Condizioni:** Adegua il prezzo in base allo stato generale, alla manutenzione e alla presenza di eventuali danni o difetti.
            4.  **Calcolo Prezzo Finale:** Calcola il prezzo finale consigliato.
            
            **Output:**
            Rispondi **esclusivamente** con un oggetto JSON valido, senza testo prima o dopo. Il JSON deve avere la seguente struttura:
            {
              "ragionamento": "Una breve analisi testuale dei passaggi che hai seguito per arrivare al prezzo finale.",
              "prezzo_consigliato": VALORE_NUMERICO_INTERO
            }
            
            Esempio di output:
            {
              "ragionamento": "Partendo da una base di 16.000€ per una Golf del 2020, ho ridotto il valore di 1.500€ per il chilometraggio elevato e aggiunto 500€ per gli optional presenti, arrivando a un prezzo finale competitivo.",
              "prezzo_consigliato": 15000
            }
            """

        eseguiChiamataAI(prompt) { response ->
            try {
                // Esegui il parsing della risposta JSON
                val jsonObject = org.json.JSONObject(response)
                val prezzo = jsonObject.getInt("prezzo_consigliato")

                // Aggiorna la UI con il valore numerico
                _prezzoSuggerito.postValue(prezzo.toString())

            } catch (e: Exception) {
                // Gestisci eventuali errori di parsing
                _prezzoSuggerito.postValue("Errore")
                // Logga l'errore: e.printStackTrace()
            }
        }
    }

    fun generaDescrizione(datiAuto: Map<String, String>) {
        val prompt = """
        Sei un venditore non esperto che vuole scrivere una descrizione oggettiva della tua macchina usata. Genera una descrizione di massimo 150 caratteri accattivante e professionale per un annuncio di vendita online in italiano.
        Includi i dettagli chiave in modo naturale nel testo. Non usare elenchi puntati. Specifica sempre se si tratta di un suv, sportiva, familiare ect.
        Scrivi in prima persona singolare.
        Nel testo inserisci sempre se si tratta di una di queste categorie: "suv", "sportiva", "berlina", "station", "wagon", "sw",
        "citycar", "utilitaria", "crossover", "cabrio", "coupe",
        "familiare", "monovolume"
        Dati Auto:
        - Modello: ${datiAuto["titolo"]}
        - Anno: ${datiAuto["anno"]}
        - Chilometraggio: ${datiAuto["chilometraggio"]} km
        - Carburante: ${datiAuto["carburante"]}
        - Cambio: ${datiAuto["cambio"]}
        - Cv: ${datiAuto["cv"]} cv
        - Descrizione utente: ${datiAuto["descrizione"]}
        """
        eseguiChiamataAI(prompt) { response ->
            _descrizioneGenerata.postValue(response)
        }
    }

    private fun eseguiChiamataAI(prompt: String, onSuccess: (String) -> Unit) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val request = AIRequest(
                    model = "gpt-4o-mini",
                    messages = listOf(Message("user", prompt)),
                    maxTokens = 150,
                    temperature = 0.7
                )
                val response = RetrofitClient.instance.getCompletion(request)

                if (response.isSuccessful && response.body() != null) {
                    val content = response.body()!!.choices.firstOrNull()?.message?.content?.trim()
                    onSuccess(content ?: "Nessuna risposta dall'IA.")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AI_ERROR", "Errore API: $errorBody")
                    _errorMessage.postValue("Errore dalla API. Controlla il Logcat per i dettagli.")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Errore di rete: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
