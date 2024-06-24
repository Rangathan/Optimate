package com.example.optimate.businessOwner

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import com.example.optimate.R
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.example.optimate.loginAndRegister.GlobalUserData
import com.google.firebase.firestore.FirebaseFirestore

class TitlesActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fetchTitles()
        val callback = object : OnBackPressedCallback(true /* default to enabled */) {
            override fun handleOnBackPressed() {

                val intent = Intent(this@TitlesActivity, DynamicLandingActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun fetchTitles() {
        db.collection("titles")
            .whereEqualTo("bid", GlobalUserData.bid)
            .get()
            .addOnSuccessListener { documents ->
                val titlesList = mutableListOf<Title>()

                for (document in documents) {
                    val title = document.getString("title") ?: ""
                    val role = document.getString("role") ?: ""
                    val category = when (role) {
                        "Manager" -> "Managers:"
                        "Employee" -> "Employees:"
                        else -> "Others:"
                    }
                    titlesList.add(Title(title, category))
                }

                setContent {
                    MaterialTheme {
                        TitlesScreen(titlesList) // Pass the fetched roles list here
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Handle the error
            }
    }
}


