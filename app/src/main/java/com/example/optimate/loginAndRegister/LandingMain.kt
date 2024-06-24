package com.example.optimate.loginAndRegister

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.optimate.R

class LandingMain : AppCompatActivity() {
    private lateinit var startBtn: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing_main)

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        // Check if the user has seen this page before
        if (sharedPreferences.getBoolean("hasSeenLandingPage", false)) {
            // If the user has seen this page before, direct them to the login page
            startActivity(Intent(this, Login::class.java))
            finish() // Finish this activity to prevent the user from coming back to it when pressing the back button
            return // Return to prevent executing further code in this method
        }

        startBtn = findViewById(R.id.startBtn)
        startBtn.setOnClickListener {
            // Set the flag indicating that the user has seen this page
            sharedPreferences.edit().putBoolean("hasSeenLandingPage", true).apply()
            // Proceed to the Login page
            startActivity(Intent(this, Login::class.java))
            finish() // Finish this activity to prevent the user from coming back to it when pressing the back button
        }
    }
}
