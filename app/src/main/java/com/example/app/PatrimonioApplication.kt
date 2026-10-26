package com.patrimonialima.app

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class PatrimonioApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Habilitar persistencia local offline (RNF04)
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }
}