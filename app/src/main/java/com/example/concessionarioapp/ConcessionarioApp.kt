package com.example.concessionarioapp

import android.app.Application
import com.google.firebase.FirebaseApp

class ConcessionarioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inizializza Firebase
        FirebaseApp.initializeApp(this)
    }
}
