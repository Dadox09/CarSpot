package com.example.concessionarioapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.concessionarioapp.adapters.AnnuncioEvidenzaAdapter
import com.example.concessionarioapp.R
import com.example.concessionarioapp.databinding.FragmentHomeBinding
import com.example.concessionarioapp.viewmodels.AnnunciViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnnunciViewModel by viewModels()
    private lateinit var annunciEvidenzaAdapter: AnnuncioEvidenzaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadFeaturedAnnunci()
    }

    private fun setupRecyclerView() {
        annunciEvidenzaAdapter = AnnuncioEvidenzaAdapter { annuncio ->
            // Quando l'utente clicca su un annuncio in evidenza, naviga verso il fragment di dettaglio
            val bundle = Bundle().apply {
                putString("annuncioId", annuncio.id)
            }
            findNavController().navigate(
                R.id.action_homeFragment_to_dettaglioAnnuncioFragment,
                bundle
            )
        }

        binding.annunciEvidenzaRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = annunciEvidenzaAdapter
        }
    }

    private fun setupClickListeners() {
        // Configurazione dei click listener per le card
        binding.compraCard.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_compraFragment)
        }

        binding.vendiCard.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_vendiFragment)
        }

        binding.profiloCard.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profiloFragment)
        }
    }

    private fun loadFeaturedAnnunci() {
        // Mostra il progress bar durante il caricamento
        binding.loadingProgressBar.visibility = View.VISIBLE

        // Osserva i cambiamenti nella lista di annunci
        viewModel.annunci.observe(viewLifecycleOwner) { annunci ->
            binding.loadingProgressBar.visibility = View.GONE

            if (annunci.isNotEmpty()) {
                // Prendi i primi 5 annunci per ora
                val annunciEvidenza = annunci.take(5)
                annunciEvidenzaAdapter.updateAnnunci(annunciEvidenza)
            }
        }

        viewModel.caricaAnnunci()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}