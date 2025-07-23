package com.example.concessionarioapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.concessionarioapp.WelcomeActivity
import com.example.concessionarioapp.databinding.FragmentProfiloBinding
import com.example.concessionarioapp.viewmodels.ProfiloViewModel
import com.google.android.material.snackbar.Snackbar

class ProfiloFragment : Fragment() {

    private var _binding: FragmentProfiloBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfiloViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfiloBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza il ViewModel
        viewModel = ViewModelProvider(this)[ProfiloViewModel::class.java]

        // Osserva i dati LiveData
        setupObservers()

        // Configura i listener
        setupListeners()
    }

    private fun setupObservers() {
        // Osserva il nome utente
        viewModel.userName.observe(viewLifecycleOwner) { userName ->
            binding.textViewUsername.text = userName
        }

        // Osserva l'email dell'utente
        viewModel.userEmail.observe(viewLifecycleOwner) { email ->
            binding.textViewEmail.text = email
        }

        // Osserva il numero di annunci attivi
        viewModel.annunciAttivi.observe(viewLifecycleOwner) { count ->
            binding.textViewAnnunciAttivi.text = count
        }

        // Osserva la data di registrazione
        viewModel.dataRegistrazione.observe(viewLifecycleOwner) { date ->
            binding.textViewDataRegistrazione.text = date
        }

        // Osserva lo stato di caricamento
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Qui puoi mostrare/nascondere un indicatore di caricamento se necessario
        }

        // Osserva messaggi di errore
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        // Osserva lo stato dell'utente corrente
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user == null) {
                // L'utente non è loggato, reindirizza alla schermata di login
                val intent = Intent(requireContext(), WelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    private fun setupListeners() {
        // Configura il pulsante di logout
        binding.buttonLogout.setOnClickListener {
            viewModel.logout()
            val intent = Intent(requireContext(), WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}