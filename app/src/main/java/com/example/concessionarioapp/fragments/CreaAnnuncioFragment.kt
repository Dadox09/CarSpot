package com.example.concessionarioapp.fragments

import android.R
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.concessionarioapp.classes.Annuncio
import com.example.concessionarioapp.NotificationService
import com.example.concessionarioapp.adapters.FotoAdapter
import com.example.concessionarioapp.databinding.FragmentCreaAnnuncioBinding
import com.example.concessionarioapp.viewmodels.CreaAnnuncioViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class CreaAnnuncioFragment : Fragment() {

    private var _binding: FragmentCreaAnnuncioBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private val viewModel: CreaAnnuncioViewModel by viewModels()

    // Lista delle foto selezionate
    private val fotoSelezionate = mutableListOf<Uri>()
    private lateinit var fotoAdapter: FotoAdapter

    // Launcher per selezionare le foto
    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val spazioDisponibile = 5 - fotoSelezionate.size
            val daAggiungere = uris.take(spazioDisponibile)

            daAggiungere.forEach { uri ->
                fotoAdapter.addFoto(uri)
            }

            if (uris.size > spazioDisponibile) {
                Toast.makeText(requireContext(),
                    "Puoi aggiungere massimo 5 foto. Alcune foto non sono state aggiunte.",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreaAnnuncioBinding.inflate(inflater, container, false)

        // Inizializza Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupScelte()
        setupRecyclerView()

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

    private fun setupRecyclerView() {
        fotoAdapter = FotoAdapter(
            foto = fotoSelezionate,
            onRemoveFoto = { position ->
                fotoAdapter.removeFoto(position)
            },
            onAddFoto = {
                pickMultipleMedia.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )

        binding.recyclerViewFoto.apply {
            adapter = fotoAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupScelte() {
        //dropdown per il tipo di carburante
        val tipiCarburante = arrayOf("Benzina", "Diesel", "GPL", "Metano", "Elettrico", "Ibrido")
        val carburanteAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipiCarburante)
        binding.carburanteAutoComplete.setAdapter(carburanteAdapter)
        binding.carburanteAutoComplete.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))

        //dropdown per il tipo di cambio
        val tipiCambio = arrayOf("Manuale", "Automatico", "Semiautomatico")
        val cambioAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipiCambio)
        binding.cambioAutoComplete.setAdapter(cambioAdapter)
        binding.cambioAutoComplete.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
    }

    private fun salvaAnnuncio() {
        // Valori inseriti dall'utente
        val titolo = binding.titoloEditText.text.toString().trim()
        val descrizione = binding.descrizioneEditText.text.toString().trim()
        val prezzoText = binding.prezzoEditText.text.toString().trim()
        val annoText = binding.annoEditText.text.toString().trim()
        val cvText = binding.cvEditText.text.toString().trim()
        val chilometraggioText = binding.chilometraggioEditText.text.toString().trim()
        val carburante = binding.carburanteAutoComplete.text.toString().trim()
        val cambio = binding.cambioAutoComplete.text.toString().trim()
        val userId = auth.currentUser?.uid

        // Verifica che i campi obbligatori siano compilati
        if (titolo.isEmpty() || descrizione.isEmpty() || prezzoText.isEmpty() || annoText.isEmpty() ||
            cvText.isEmpty() || chilometraggioText.isEmpty() || carburante.isEmpty() || cambio.isEmpty()) {
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

        val cv = cvText.toIntOrNull()
        if (cv == null || cv <= 0) {
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

        // Mostra progress bar
        binding.progressBar.visibility = View.VISIBLE

        // Upload delle foto prima di salvare l'annuncio
        uploadFoto { urlImmagini ->
            salvaAnnuncioConImmagini(titolo, descrizione, prezzo, anno, cv, chilometraggio,
                carburante, cambio, userId, urlImmagini)
        }
    }

    private fun uploadFoto(onComplete: (List<String>) -> Unit) {
        if (fotoSelezionate.isEmpty()) {
            onComplete(emptyList())
            return
        }

        val urlImmagini = mutableListOf<String>()
        var uploadCompletati = 0
        val totalUploads = fotoSelezionate.size

        fotoSelezionate.forEach { uri ->
            val fileName = "annunci/${UUID.randomUUID()}_${System.currentTimeMillis()}.jpg"
            val imageRef = storage.reference.child(fileName)

            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        urlImmagini.add(downloadUrl.toString())
                        uploadCompletati++

                        if (uploadCompletati == totalUploads) {
                            onComplete(urlImmagini)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(),
                        "Errore nel caricamento foto: ${e.message}",
                        Toast.LENGTH_LONG).show()
                    binding.salvaButton.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
        }
    }

    private fun salvaAnnuncioConImmagini(
        titolo: String, descrizione: String, prezzo: Double, anno: Int,
        cv: Int, chilometraggio: Int, carburante: String, cambio: String,
        userId: String, urlImmagini: List<String>
    ) {
        val nuovoAnnuncioRef = firestore.collection("annunci").document()

        val annuncio = Annuncio(
            id = nuovoAnnuncioRef.id,
            titolo = titolo,
            descrizione = descrizione,
            prezzo = prezzo,
            anno = anno,
            cv = cv,
            chilometraggio = chilometraggio,
            carburante = carburante,
            cambio = cambio,
            userId = userId,
            immagini = urlImmagini
        )

        nuovoAnnuncioRef.set(annuncio)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
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
                binding.progressBar.visibility = View.GONE
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
            "cv" to binding.cvEditText.text.toString().trim(),
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