package com.example.optimate.businessOwner

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity

class AddTitleDetailsActivity: AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val titleType = intent.getStringExtra("TITLE_TYPE")

        setContent {
            if (titleType != null) {
                when (titleType) {
                    "manager" -> SetupRoleUI(role="Manager", onAddComplete = { navigateToNextScreen() })
                    "employee" -> SetupRoleUI(role="Employee", onAddComplete = { navigateToNextScreen() })
                }
            }
        }
    }
    private fun navigateToNextScreen() {
        val intent = Intent(this, TitlesActivity::class.java)
        startActivity(intent)
        finish()
    }
}

