package com.example.concessionarioapp.fragments

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.concessionarioapp.classes.Annuncio
import com.example.concessionarioapp.NotificationService
import com.example.concessionarioapp.databinding.FragmentCreaAnnuncioBinding
import com.example.concessionarioapp.viewmodels.CreaAnnuncioViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreaAnnuncioFragment : Fragment() {

    private var _binding: FragmentCreaAnnuncioBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val viewModel: CreaAnnuncioViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreaAnnuncioBinding.inflate(inflater, container, false)

        // Inizializza Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupScelte()

        binding.salvaButton.setOnClickListener {
            salvaAnnuncio()
        }

        binding.suggerisciPrezzoButton.setOnClickListener {
            val datiAuto = raccogliDatiAuto()
            val datiObbligatori = datiAuto.filterKeys { it != "descrizione" && it != "prezzo" }
            if (datiObbligatori.any { it.value.isBlank() }) {
                Toast.makeText(requireContext(), "Compila i dati dell'auto per generare un prezzo", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.suggerisciPrezzo(datiAuto)
            }
        }

        binding.generaDescrizioneButton.setOnClickListener {
            val datiAuto = raccogliDatiAuto()
            val datiObbligatori = datiAuto.filterKeys { it != "descrizione" && it != "prezzo" }
            if (datiObbligatori.any { it.value.isBlank() }) {
                Toast.makeText(requireContext(), "Compila i dati dell'auto per generare una descrizione", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.generaDescrizione(datiAuto)
            }
        }

        setupObservers()
    }

    private fun setupScelte() {
        //dropdown per il tipo di carburante
        val tipiCarburante = arrayOf("Benzina", "Diesel", "GPL", "Metano", "Elettrico", "Ibrido")
        val carburanteAdapter =
            ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, tipiCarburante)
        binding.carburanteAutoComplete.setAdapter(carburanteAdapter)
        binding.carburanteAutoComplete.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        //dropdown per il tipo di cambio
        val tipiCambio = arrayOf("Manuale", "Automatico", "Semiautomatico")
        val cambioAdapter =
            ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, tipiCambio)
        binding.cambioAutoComplete.setAdapter(cambioAdapter)
        binding.cambioAutoComplete.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    private fun salvaAnnuncio() {
        // Valori inseriti dall'utente
        val titolo = binding.titoloEditText.text.toString().trim()
        val descrizione = binding.descrizioneEditText.text.toString().trim()
        val prezzoText = binding.prezzoEditText.text.toString().trim()
        val annoText = binding.annoEditText.text.toString().trim()
        val cilindrataText = binding.cilindrataEditText.text.toString().trim()
        val chilometraggioText = binding.chilometraggioEditText.text.toString().trim()
        val carburante = binding.carburanteAutoComplete.text.toString().trim()
        val cambio = binding.cambioAutoComplete.text.toString().trim()
        val userId = auth.currentUser?.uid

        // Verifica che i campi obbligatori siano compilati
        if (titolo.isEmpty() || descrizione.isEmpty() || prezzoText.isEmpty() || annoText.isEmpty() ||
            cilindrataText.isEmpty() || chilometraggioText.isEmpty() || carburante.isEmpty() || cambio.isEmpty()) {
            Toast.makeText(requireContext(), "Per favore, compila tutti i campi", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId == null) {
            Toast.makeText(requireContext(), "Errore: utente non autenticato. Prova a ri-accedere.", Toast.LENGTH_LONG).show()
            return
        }

        // Validazione dei campi numerici
        val prezzo = prezzoText.toDoubleOrNull()
        if (prezzo == null || prezzo <= 0) {
            binding.prezzoInputLayout.error = "Inserisci un prezzo valido"
            binding.prezzoInputLayout.setErrorTextColor(ContextCompat.getColorStateList(requireContext(), R.color.white))
            return
        } else {
            binding.prezzoInputLayout.error = null
        }

        val anno = annoText.toIntOrNull()
        if (anno == null || anno < 1900 || anno > 2100) {
            binding.annoInputLayout.error = "Inserisci un anno valido"
            binding.annoInputLayout.setErrorTextColor(ContextCompat.getColorStateList(requireContext(), R.color.white))
            return
        } else {
            binding.annoInputLayout.error = null
        }

        val cilindrata = cilindrataText.toIntOrNull()
        if (cilindrata == null || cilindrata <= 0) {
            binding.cilindrataInputLayout.error = "Inserisci una cilindrata valida"
            binding.cilindrataInputLayout.setErrorTextColor(ContextCompat.getColorStateList(requireContext(), R.color.white))
            return
        } else {
            binding.cilindrataInputLayout.error = null
        }

        val chilometraggio = chilometraggioText.toIntOrNull()
        if (chilometraggio == null || chilometraggio < 0) {
            binding.chilometraggioInputLayout.error = "Inserisci un chilometraggio valido"
            binding.chilometraggioInputLayout.setErrorTextColor(ContextCompat.getColorStateList(requireContext(), R.color.white))
            return
        } else {
            binding.chilometraggioInputLayout.error = null
        }

        // Bottone non abilitato per evitare doppi click
        binding.salvaButton.isEnabled = false

        //Creo varaibile per salvare su firebas
        val nuovoAnnuncioRef = firestore.collection("annunci").document()

        val annuncio = Annuncio(
            id = nuovoAnnuncioRef.id,
            titolo = titolo,
            descrizione = descrizione,
            prezzo = prezzo,
            anno = anno,
            cilindrata = cilindrata,
            chilometraggio = chilometraggio,
            carburante = carburante,
            cambio = cambio,
            userId = userId
        )

        nuovoAnnuncioRef.set(annuncio)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Annuncio salvato con successo!", Toast.LENGTH_SHORT).show()

                // Invia una notifica a tutti gli utenti per il nuovo annuncio
                NotificationService.Companion.sendNotificationToAll(
                    requireContext(),
                    "Nuovo annuncio disponibile",
                    "${annuncio.titolo} - ${annuncio.anno} - ${annuncio.prezzo}€"
                )

                findNavController().navigate(com.example.concessionarioapp.R.id.action_creaAnnuncioFragment_to_vendiFragment)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Errore nel salvataggio: ${e.message}", Toast.LENGTH_LONG).show()
                // Riabilita il pulsante in caso di errore
                binding.salvaButton.isEnabled = true
            }
    }

    private fun raccogliDatiAuto(): Map<String, String> {
        return mapOf(
            "titolo" to binding.titoloEditText.text.toString().trim(),
            "descrizione" to binding.descrizioneEditText.text.toString().trim(),
            "anno" to binding.annoEditText.text.toString().trim(),
            "cilindrata" to binding.cilindrataEditText.text.toString().trim(),
            "chilometraggio" to binding.chilometraggioEditText.text.toString().trim(),
            "carburante" to binding.carburanteAutoComplete.text.toString().trim(),
            "cambio" to binding.cambioAutoComplete.text.toString().trim()
        )
    }

    private fun setupObservers() {
        viewModel.prezzoSuggerito.observe(viewLifecycleOwner) { prezzo ->
            prezzo?.let {
                binding.prezzoEditText.setText(it)
                binding.layoutPrezzoAI.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Prezzo suggerito!", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.descrizioneGenerata.observe(viewLifecycleOwner) { descrizione ->
            descrizione?.let {
                binding.descrizioneEditText.setText(it)
                binding.layoutDescrizioneAI.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Descrizione generata!", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.suggerisciPrezzoButton.isEnabled = !isLoading
            binding.generaDescrizioneButton.isEnabled = !isLoading
            binding.salvaButton.isEnabled = !isLoading
            // Qui potresti anche mostrare/nascondere una ProgressBar
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), "Errore AI: $it", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}