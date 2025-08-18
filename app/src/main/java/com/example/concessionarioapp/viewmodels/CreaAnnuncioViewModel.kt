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
        Sei un esperto di valutazione di auto usate per il mercato italiano.
        Basandoti sui seguenti dati, calcola un prezzo di vendita realistico e competitivo.
        Rispondi solo con il valore numerico del prezzo, senza valuta o altro testo.

        Dati Auto:
        - Modello: ${datiAuto["titolo"]}
        - Anno: ${datiAuto["anno"]}
        - Chilometraggio: ${datiAuto["chilometraggio"]} km
        - Carburante: ${datiAuto["carburante"]}
        - Cambio: ${datiAuto["cambio"]}
        - Cilindrata: ${datiAuto["cilindrata"]} cc
        """
        eseguiChiamataAI(prompt) { response ->
            _prezzoSuggerito.postValue(response)
        }
    }

    fun generaDescrizione(datiAuto: Map<String, String>) {
        val prompt = """
        Sei un esperto venditore di auto. Genera una descrizione di massimo 150 caratteri accattivante e professionale per un annuncio di vendita online in italiano.
        Includi i dettagli chiave in modo naturale nel testo. Non usare elenchi puntati.

        Dati Auto:
        - Modello: ${datiAuto["titolo"]}
        - Anno: ${datiAuto["anno"]}
        - Chilometraggio: ${datiAuto["chilometraggio"]} km
        - Carburante: ${datiAuto["carburante"]}
        - Cambio: ${datiAuto["cambio"]}
        - Cilindrata: ${datiAuto["cilindrata"]} cc
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
