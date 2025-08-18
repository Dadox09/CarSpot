package com.example.concessionarioapp.fragments

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.concessionarioapp.classes.Annuncio
import com.example.concessionarioapp.R
import com.example.concessionarioapp.classes.User
import com.example.concessionarioapp.databinding.FragmentDettaglioAnnuncioBinding
import com.example.concessionarioapp.viewmodels.DettaglioAnnuncioViewModel
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

        setupObservers()

        // Carica i dati dell'annuncio usando l'ID ricevuto dagli argomenti
        viewModel.caricaDettagliAnnuncio(annuncioId!!)

        // Configurazione del pulsante per contattare il venditore
        binding.contattaButton.setOnClickListener {
            contattaVenditore()
        }

        // Configurazione del pulsante per eliminare l'annuncio
        binding.eliminaButton.setOnClickListener {
            mostraDialogConfermaEliminazione()
        }
    }

    private fun setupObservers() {
        // Osservo e associo l'annuncio
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

        // Osserva se l'utente può eliminare l'annuncio
        viewModel.canDelete.observe(viewLifecycleOwner) { canDelete ->
            binding.eliminaButton.visibility = if (canDelete) View.VISIBLE else View.GONE
        }

        // Osserva il successo dell'eliminazione
        viewModel.deleteSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Annuncio eliminato con successo", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun updateUI(annuncio: Annuncio) {
        binding.titoloTextView.text = annuncio.titolo
        binding.prezzoTextView.text = String.format("€ %.2f", annuncio.prezzo)
        binding.descrizioneTextView.text = annuncio.descrizione

        // Immagine dell'annuncio
        Glide.with(this)
            .load(annuncio.imageUrl)
            .placeholder(R.drawable.placeholder) // Immagine di default
            .error(R.drawable.placeholder) // Immagine di errore
            .into(binding.immagineAnnuncioImageView)


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
        //gestisce in caso di errore di racconta dati di un utente
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

        // Indirizzamento a gmail per inviare l'email
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

    private fun mostraDialogConfermaEliminazione() {
        // Mostra un popup di conferma per l'eliminazione dell'annuncio
        AlertDialog.Builder(requireContext())
            .setTitle("Conferma eliminazione")
            .setMessage("Sei sicuro di voler eliminare questo annuncio? L'operazione non può essere annullata.")
            .setPositiveButton("Elimina") { _, _ ->
                annuncioId?.let { viewModel.eliminaAnnuncio(it) }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}