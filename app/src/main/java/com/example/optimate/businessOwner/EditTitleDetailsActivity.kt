package com.example.optimate.businessOwner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.example.optimate.loginAndRegister.GlobalUserData
import com.google.firebase.firestore.FirebaseFirestore

class EditTitleDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val titleName = intent.getStringExtra("title_name")
        Log.d("TitleTransfer", "Received title_name: $titleName")

        if (titleName != null) {
            fetchRoleAndSetupUI(titleName)
        } else {
            Log.e("EditTitleDetails", "Title name is null")
            // Handle the case where titleName is null
        }
    }

    private fun fetchRoleAndSetupUI(titleName: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("titles")
            .whereEqualTo("bid", GlobalUserData.bid)
            .whereEqualTo("title", titleName)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e("EditTitleDetails", "No matching title found")
                    // Handle the case where no matching title is found
                    return@addOnSuccessListener
                }
                val role = documents.first().getString("role") ?: ""
                val capitalizedRole = role.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } // Capitalize the first letter
                Log.d("EditTitleDetails", "Fetched role: $capitalizedRole")

                // Now that you have the role, set the content
                setContent {
                    EditTitleUI(role = capitalizedRole, titleName = titleName, onAddComplete = { navigateToNextScreen() })
                }
            }
            .addOnFailureListener { exception ->
                Log.e("EditTitleDetails", "Error fetching role: ${exception.localizedMessage}")
                // Handle database fetch failure
            }
    }


    private fun navigateToNextScreen() {
        val intent = Intent(this, TitlesActivity::class.java)
        startActivity(intent)
        finish()
    }
}
