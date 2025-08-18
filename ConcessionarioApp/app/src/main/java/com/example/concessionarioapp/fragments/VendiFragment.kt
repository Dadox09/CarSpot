package com.example.concessionarioapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.concessionarioapp.classes.Annuncio
import com.example.concessionarioapp.adapters.AnnuncioAdapter
import com.example.concessionarioapp.R
import com.example.concessionarioapp.databinding.FragmentVendiBinding
import com.example.concessionarioapp.viewmodels.AnnunciViewModel
import com.google.firebase.auth.FirebaseAuth

class VendiFragment : Fragment() {

    private var _binding: FragmentVendiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnnunciViewModel by viewModels()
    private lateinit var annunciAdapter: AnnuncioAdapter
    private lateinit var auth: FirebaseAuth
    private val fullAnnunciList = mutableListOf<Annuncio>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVendiBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        observeViewModel()

        // Carica gli annunci per l'utente corrente
        val userId = auth.currentUser?.uid
        viewModel.caricaAnnunci(userId)
    }

    private fun setupRecyclerView() {
        annunciAdapter = AnnuncioAdapter(
            mutableListOf(),
            onAnnuncioClick = { annuncio ->
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.vendiFragment) {
                    val bundle = Bundle()
                    bundle.putString("annuncioId", annuncio.id)
                    navController.navigate(
                        R.id.action_vendiFragment_to_dettaglioAnnuncioFragment,
                        bundle
                    )
                }
            },
            onLikeClick = { annuncio ->
                viewModel.toggleLike(annuncio)
            },
            showLikes = false
        )
        binding.recyclerViewAnnunciVendi.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = annunciAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.annunci.observe(viewLifecycleOwner) { annunci ->
            if (annunci.isNotEmpty()) {
                // Mostra la lista degli annunci
                binding.recyclerViewAnnunciVendi.visibility = View.VISIBLE
                binding.messageTextView.visibility = View.GONE

                fullAnnunciList.clear()
                fullAnnunciList.addAll(annunci)
                annunciAdapter.updateAnnunci(fullAnnunciList)
                binding.searchViewVendi.setQuery("", false)
            }
            else{
                mostraMessaggio("Nessun annuncio trovato")
            }
        }
    }

    private fun mostraMessaggio(messaggio: String) {
        binding.recyclerViewAnnunciVendi.visibility = View.GONE
        binding.messageTextView.visibility = View.VISIBLE
        binding.messageTextView.text = messaggio
    }

    private fun setupSearchView() {
        binding.searchViewVendi.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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