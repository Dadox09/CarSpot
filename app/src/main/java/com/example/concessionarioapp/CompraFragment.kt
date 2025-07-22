package com.example.concessionarioapp

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
import com.example.concessionarioapp.databinding.FragmentCompraBinding

class CompraFragment : Fragment() {

    private var _binding: FragmentCompraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnnunciViewModel by viewModels()
    private lateinit var annunciAdapter: AnnuncioAdapter
    private val fullAnnunciList = mutableListOf<Annuncio>()

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
        observeViewModel()

        viewModel.caricaAnnunci() // Carica tutti gli annunci
    }

    private fun setupRecyclerView() {
        annunciAdapter = AnnuncioAdapter(mutableListOf(),
            onAnnuncioClick = { annuncio ->
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.compraFragment) {
                    val bundle = Bundle()
                    bundle.putString("annuncioId", annuncio.id)
                    navController.navigate(R.id.action_compraFragment_to_dettaglioAnnuncioFragment, bundle)
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

    private fun observeViewModel() {
        viewModel.annunci.observe(viewLifecycleOwner) { annunci ->
            if (annunci.isNotEmpty()) {
                fullAnnunciList.clear()
                fullAnnunciList.addAll(annunci)
                annunciAdapter.updateAnnunci(fullAnnunciList)
                // Resetta la ricerca per mostrare tutti i risultati quando i dati cambiano
                binding.searchViewCompra.setQuery("", false)
            }else{
                mostraMessaggio("Nessun annuncio trovato")
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
