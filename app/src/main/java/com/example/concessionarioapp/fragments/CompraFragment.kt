package com.example.concessionarioapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.concessionarioapp.classes.Annuncio
import com.example.concessionarioapp.adapters.AnnuncioAdapter
import com.example.concessionarioapp.adapters.ChatAdapter
import com.example.concessionarioapp.R
import com.example.concessionarioapp.databinding.FragmentCompraBinding
import com.example.concessionarioapp.viewmodels.AnnunciViewModel
import com.example.concessionarioapp.viewmodels.ChatbotViewModel

class CompraFragment : Fragment() {

    private var _binding: FragmentCompraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnnunciViewModel by viewModels()
    private val chatbotViewModel: ChatbotViewModel by viewModels()

    private lateinit var annunciAdapter: AnnuncioAdapter
    private lateinit var chatAdapter: ChatAdapter

    private val fullAnnunciList = mutableListOf<Annuncio>()
    private var isChatVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        setupChatbot()
        observeViewModel()
        observeChatbotViewModel()

        viewModel.caricaAnnunci() // Carica tutti gli annunci

        if (arguments?.getBoolean("openChatbot") == true) {
            toggleChatbot()
        }
    }

    private fun setupRecyclerView() {
        annunciAdapter = AnnuncioAdapter(
            mutableListOf(),
            onAnnuncioClick = { annuncio ->
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.compraFragment) {
                    val bundle = Bundle()
                    bundle.putString("annuncioId", annuncio.id)
                    navController.navigate(
                        R.id.action_compraFragment_to_dettaglioAnnuncioFragment,
                        bundle
                    )
                }
            },
            onLikeClick = { annuncio ->
                viewModel.toggleLike(annuncio)
            }
        )
        binding.recyclerViewAnnunci.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = annunciAdapter
        }
    }

    private fun setupChatbot() {
        chatAdapter = ChatAdapter { annuncio ->
            // Quando l'utente clicca su un annuncio nella chat
            chatbotViewModel.onAnnuncioClicked(annuncio)
        }
        binding.recyclerViewChat.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }

        // Imposta il callback per la navigazione agli annunci
        chatbotViewModel.setOnAnnuncioSelectedCallback { annuncio ->
            // Chiudi la chat
            toggleChatbot()
            // Naviga al dettaglio annuncio
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.compraFragment) {
                val bundle = Bundle()
                bundle.putString("annuncioId", annuncio.id)
                navController.navigate(
                    R.id.action_compraFragment_to_dettaglioAnnuncioFragment,
                    bundle
                )
            }
        }

        // Gestione del toggle del chatbot
        binding.fabChatbot.setOnClickListener {
            toggleChatbot()
        }

        // Gestione dell'invio messaggi
        binding.buttonSendMessage.setOnClickListener {
            val message = binding.editTextMessage.text.toString()
            if (message.isNotBlank()) {
                chatbotViewModel.sendMessage(message)
                binding.editTextMessage.text.clear()
            }
        }

        // Chiusura chat
        binding.buttonCloseChat.setOnClickListener {
            toggleChatbot()
        }
    }

    private fun toggleChatbot() {
        isChatVisible = !isChatVisible
        if (isChatVisible) {
            // Mostra chat
            binding.chatContainer.visibility = View.VISIBLE
            binding.fabChatbot.hide()
            binding.recyclerViewAnnunci.visibility = View.GONE
            binding.searchViewCompra.visibility = View.GONE
            binding.textViewTitle.visibility = View.GONE
        } else {
            // Nasconde chat
            binding.chatContainer.visibility = View.GONE
            binding.fabChatbot.show()
            binding.recyclerViewAnnunci.visibility = View.VISIBLE
            binding.searchViewCompra.visibility = View.VISIBLE
            binding.textViewTitle.text = "Trova la tua prossima auto"
        }
    }

    private fun observeViewModel() {
        viewModel.annunci.observe(viewLifecycleOwner) { annunci ->
            if (annunci.isNotEmpty()) {
                fullAnnunciList.clear()
                fullAnnunciList.addAll(annunci)
                annunciAdapter.updateAnnunci(fullAnnunciList)
                binding.searchViewCompra.setQuery("", false)

                // Passa gli annunci al chatbot per i suggerimenti
                chatbotViewModel.setAnnunci(annunci)
            } else {
                mostraMessaggio("Nessun annuncio trovato")
            }
        }
    }

    private fun observeChatbotViewModel() {
        chatbotViewModel.messages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.updateMessages(messages)
            // Scrolla all'ultimo messaggio
            if (messages.isNotEmpty()) {
                binding.recyclerViewChat.scrollToPosition(messages.size - 1)
            }
        }

        chatbotViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarChat.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonSendMessage.isEnabled = !isLoading
        }

        chatbotViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                chatbotViewModel.clearError()
            }
        }
    }

    private fun mostraMessaggio(messaggio: String) {
        binding.recyclerViewAnnunci.visibility = View.GONE
        binding.messageTextView.visibility = View.VISIBLE
        binding.messageTextView.text = messaggio
    }

    private fun setupSearchView() {
        binding.searchViewCompra.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterAnnunci(newText)
                return true
            }
        })
    }

    private fun filterAnnunci(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            fullAnnunciList
        } else {
            val searchQuery = query.lowercase().trim()
            fullAnnunciList.filter {
                it.titolo.lowercase().contains(searchQuery) || it.descrizione.lowercase().contains(searchQuery)
            }
        }
        annunciAdapter.updateAnnunci(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}