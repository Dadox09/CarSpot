package com.example.concessionarioapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.concessionarioapp.classes.User
import com.example.concessionarioapp.databinding.ActivityWelcomeBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private val auth = FirebaseAuth.getInstance()

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        handleSignInResult(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Se l'utente è già loggato, vai direttamente alla MainActivity
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder()
                .setScopes(listOf("email", "profile"))
                .build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.FirebaseUI)
            .setLogo(R.drawable.logo)
            .build()

        binding.registerButton.setOnClickListener {
            signInLauncher.launch(signInIntent)
        }

        binding.loginButton.setOnClickListener {
            signInLauncher.launch(signInIntent)
        }
    }

    private fun handleSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            Log.d("Auth", "Login successful")
            // Salva i dati utente in Firestore
            saveUserToFirestore()

            // Utente loggato con successo, vai alla MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            // Gestisci gli errori
            val response = result.idpResponse
            if (response != null) {
                Log.w("Auth", "Login failed", response.error)
            } else {
                Log.d("Auth", "Login cancelled by user")
            }
        }
    }

    private fun saveUserToFirestore() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val user = User(
                id = currentUser.uid,
                nome = currentUser.displayName,
                email = currentUser.email,
                telefono = null
            )

            FirebaseFirestore.getInstance().collection("users")
                .document(currentUser.uid)
                .set(user)
                .addOnSuccessListener {
                    Log.d("Firestore", "Utente salvato con successo - ID: ${user.id}, Email: ${user.email}")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Errore nel salvare l'utente: ${e.message}")
                }
        }
    }
}