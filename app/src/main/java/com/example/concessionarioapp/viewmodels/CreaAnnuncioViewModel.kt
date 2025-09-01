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
        Sei un esperto valutatore di auto usate specializzato nel mercato italiano. Analizza i seguenti dati e fornisci una valutazione precisa del prezzo di vendita tra privati.
        
        **DATI VEICOLO:**
        - Modello: ${datiAuto["titolo"]}
        - Anno: ${datiAuto["anno"]}
        - Chilometraggio: ${datiAuto["chilometraggio"]} km
        - Carburante: ${datiAuto["carburante"]}
        - Cambio: ${datiAuto["cambio"]}
        - Potenza: ${datiAuto["cv"]} cv
        - Manutenzione: ${datiAuto["manutenzione"]}
        
        **METODOLOGIA DI VALUTAZIONE:**
        
        1. **Valore Base di Mercato:**
           - Consulta mentalmente i valori attuali di mercato per questo specifico modello/anno
           - Considera la fascia di mercato (city car, utilitaria, berlina, SUV, premium)
           - Tieni conto della popolarità e affidabilità del modello
        
        2. **Analisi Chilometraggio:**
           - Media annuale: 12.000-15.000 km/anno
           - Calcola l'età del veicolo: ${2024 - (datiAuto["anno"]?.toIntOrNull() ?: 2024)} anni
           - Chilometraggio atteso: ${(2024 - (datiAuto["anno"]?.toIntOrNull() ?: 2024)) * 13500} km circa
           - Applica correzioni: -3% ogni 10.000 km in eccesso, +2% ogni 10.000 km in difetto
        
        3. **Fattori di Motorizzazione:**
           - Benzina: valore base
           - Diesel: +5-10% se Euro 6, -10-15% se Euro 5 o inferiore
           - GPL/Metano: -5% per età impianto, +8% per risparmio carburante
           - Ibrido/Elettrico: valuta incentivi e deprezzamento batterie
        
        5. **Fattori di Mercato:**
           - Considera stagionalità (cabrio in estate, 4WD in inverno)
           - Valuta richiesta specifica del modello
           - Applica trend di mercato attuale
        
        **IMPORTANTE:** 
        - Sii realistico sui prezzi di mercato italiani 2024
        - Considera che i privati vendono generalmente 5-10% sotto i concessionari
        - Il prezzo deve essere competitivo per una vendita in tempi ragionevoli (2-3 mesi)
        
        Rispondi SOLO con questo JSON, senza altro testo:
        {
          "ragionamento": "Analisi dettagliata: valore base €X, chilometraggio [alto/nella media/basso] con correzione ±Y%, condizioni [ottime/buone/discrete] ±Z%, motorizzazione [vantaggi/svantaggi], prezzo finale competitivo per vendita privata",
          "prezzo_consigliato": NUMERO_INTERO
        }
    """.trimIndent()

        eseguiChiamataAI(prompt) { response ->
            try {
                // Pulizia della risposta per rimuovere eventuali caratteri extra
                val cleanResponse = response.trim()
                    .removePrefix("```json")
                    .removeSuffix("```")
                    .trim()

                val jsonObject = org.json.JSONObject(cleanResponse)
                val prezzo = jsonObject.getInt("prezzo_consigliato")

                // Validazione del prezzo (tra 500€ e 150.000€)
                if (prezzo in 500..150000) {
                    _prezzoSuggerito.postValue(prezzo.toString())
                } else {
                    _prezzoSuggerito.postValue("Errore: Prezzo non valido")
                }

            } catch (e: org.json.JSONException) {
                // Errore di parsing JSON
                _prezzoSuggerito.postValue("Errore di formato")
            } catch (e: Exception) {
                // Altri errori
                _prezzoSuggerito.postValue("Errore di valutazione")
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
