package com.example.concessionarioapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.concessionarioapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

        // Imposta la toolbar con sfondo trasparente
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)


        // Inizializza Firebase Auth
        auth = Firebase.auth

        // Ottieni il NavController dal NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Configura il FAB
        binding.fab.setOnClickListener {
            if (navController.currentDestination?.id == R.id.vendiFragment) {
                navController.navigate(R.id.action_vendiFragment_to_creaAnnuncioFragment)
            }
        }
        
        // Mostra/nascondi FAB in base al fragment corrente
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.vendiFragment -> binding.fab.show()
                else -> binding.fab.hide()
            }
        }

        // Configura la navigazione bottom
        binding.bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    if (navController.currentDestination?.id != R.id.homeFragment) {
                        when (navController.currentDestination?.id) {
                            R.id.compraFragment -> navController.navigate(R.id.action_compraFragment_to_homeFragment)
                            R.id.vendiFragment -> navController.navigate(R.id.action_vendiFragment_to_homeFragment)
                            R.id.creaAnnuncioFragment -> navController.navigate(R.id.action_creaAnnuncioFragment_to_homeFragment)
                            R.id.profiloFragment -> navController.navigate(R.id.action_profiloFragment_to_homeFragment)
                            R.id.dettaglioAnnuncioFragment -> navController.navigate(R.id.action_dettaglioAnnuncioFragment_to_homeFragment)
                        }
                    }
                    true
                }
                R.id.navigation_vendi -> {
                    if (navController.currentDestination?.id != R.id.vendiFragment) {
                        when (navController.currentDestination?.id) {
                            R.id.homeFragment -> navController.navigate(R.id.action_homeFragment_to_vendiFragment)
                            R.id.compraFragment -> navController.navigate(R.id.action_compraFragment_to_vendiFragment)
                            R.id.creaAnnuncioFragment -> navController.navigate(R.id.action_creaAnnuncioFragment_to_vendiFragment)
                            R.id.profiloFragment -> navController.navigate(R.id.action_profiloFragment_to_vendiFragment)
                            R.id.dettaglioAnnuncioFragment -> navController.navigate(R.id.action_dettaglioAnnuncioFragment_to_vendiFragment)
                        }
                    }
                    true
                }
                R.id.navigation_compra -> {
                    if (navController.currentDestination?.id != R.id.compraFragment) {
                        when (navController.currentDestination?.id) {
                            R.id.homeFragment -> navController.navigate(R.id.action_homeFragment_to_compraFragment)
                            R.id.vendiFragment -> navController.navigate(R.id.action_vendiFragment_to_compraFragment)
                            R.id.creaAnnuncioFragment -> navController.navigate(R.id.action_creaAnnuncioFragment_to_compraFragment)
                            R.id.profiloFragment -> navController.navigate(R.id.action_profiloFragment_to_compraFragment)
                            R.id.dettaglioAnnuncioFragment -> navController.navigate(R.id.action_dettaglioAnnuncioFragment_to_compraFragment)
                        }
                    }
                    true
                }
                R.id.navigation_profilo -> {
                    if (navController.currentDestination?.id != R.id.profiloFragment) {
                        when (navController.currentDestination?.id) {
                            R.id.homeFragment -> navController.navigate(R.id.action_homeFragment_to_profiloFragment)
                            R.id.compraFragment -> navController.navigate(R.id.action_compraFragment_to_profiloFragment)
                            R.id.vendiFragment -> navController.navigate(R.id.action_vendiFragment_to_profiloFragment)
                            R.id.creaAnnuncioFragment -> navController.navigate(R.id.action_creaAnnuncioFragment_to_profiloFragment)
                            R.id.dettaglioAnnuncioFragment -> navController.navigate(R.id.action_dettaglioAnnuncioFragment_to_profiloFragment)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_logout -> {
                // Esegui il logout
                auth.signOut()
                // Torna alla schermata di login
                val intent = Intent(this, WelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }
}