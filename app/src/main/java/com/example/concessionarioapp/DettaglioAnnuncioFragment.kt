package com.example.concessionarioapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.concessionarioapp.databinding.FragmentDettaglioAnnuncioBinding
import java.text.SimpleDateFormat
import java.util.Locale

class DettaglioAnnuncioFragment : Fragment() {

    private var _binding: FragmentDettaglioAnnuncioBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DettaglioAnnuncioViewModel by viewModels()
    
    private var annuncioId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDettaglioAnnuncioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Recupera l'ID dell'annuncio dagli argomenti
        annuncioId = arguments?.getString("annuncioId")
        
        if (annuncioId == null) {
            showError("ID annuncio non valido")
            return
        }
        
        // Configurazione degli observer
        setupObservers()
        
        // Carica i dati dell'annuncio usando l'ID ricevuto dagli argomenti
        viewModel.caricaDettagliAnnuncio(annuncioId!!)
        
        // Configurazione del pulsante per contattare il venditore
        binding.contattaButton.setOnClickListener {
            contattaVenditore()
        }
    }
    
    private fun setupObservers() {
        // Osserva i cambiamenti dell'annuncio
        viewModel.annuncio.observe(viewLifecycleOwner) { annuncio ->
            annuncio?.let { updateUI(it) }
        }
        
        // Osserva i cambiamenti del venditore
        viewModel.venditore.observe(viewLifecycleOwner) { venditore ->
            venditore?.let { updateVenditoreUI(it) }
        }
        
        // Gestisce lo stato di caricamento
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.contentLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
        }
        
        // Gestisce gli errori
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let { showError(it) }
        }
    }
    
    private fun updateUI(annuncio: Annuncio) {
        binding.titoloTextView.text = annuncio.titolo
        binding.prezzoTextView.text = String.format("€ %.2f", annuncio.prezzo)
        binding.descrizioneTextView.text = annuncio.descrizione
        
        // Mostra i dettagli aggiuntivi dell'auto
        binding.annoTextView.text = annuncio.anno.toString()
        binding.cilindrataTextView.text = "${annuncio.cilindrata} cc"
        binding.chilometraggioTextView.text = "${annuncio.chilometraggio} km"
        binding.carburanteTextView.text = annuncio.carburante
        binding.cambioTextView.text = annuncio.cambio
        
        // Formatta e mostra la data di pubblicazione
        val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.dataTextView.text = annuncio.dataCreazione?.let { formatoData.format(it) } ?: "N/D"
    }
    
    private fun updateVenditoreUI(venditore: User) {
        binding.venditoreTextView.text = venditore.nome ?: "Utente anonimo"
        println("DEBUG: Aggiornamento UI venditore - Email: '${venditore.email}', Email null? ${venditore.email == null}")
        binding.contattoTextView.text = venditore.email ?: "N/D"
        println("DEBUG: Testo impostato su contattoTextView: '${binding.contattoTextView.text}'")
    }
    
    private fun showError(message: String) {
        binding.errorTextView.text = message
        binding.errorTextView.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
    }
    
    private fun contattaVenditore() {
        // Ottieni l'email del venditore dal ViewModel
        val emailVenditore = viewModel.venditore.value?.email
        
        if (emailVenditore.isNullOrEmpty() || emailVenditore == "Contatto non disponibile") {
            Toast.makeText(requireContext(), "Email del venditore non disponibile", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Ottieni il titolo dell'annuncio per l'oggetto dell'email
        val titoloAnnuncio = viewModel.annuncio.value?.titolo ?: "il tuo annuncio"
        
        // Crea l'intent per inviare l'email
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") 
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailVenditore))
            putExtra(Intent.EXTRA_SUBJECT, "Informazioni su: $titoloAnnuncio")
            putExtra(Intent.EXTRA_TEXT, "Salve, sono interessato all'annuncio \"$titoloAnnuncio\" da te pubblicato. Potrebbe fornirmi maggiori informazioni?\n\nGrazie.")
        }
        
        try {
            startActivity(Intent.createChooser(intent, "Invia email"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Nessuna app email trovata", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
