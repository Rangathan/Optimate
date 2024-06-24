package com.example.optimate.loginAndRegister

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.example.optimate.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.firestore
import java.util.Date

class PaymentConfirm : AppCompatActivity() {
    private var db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_confirm)
        val uid = GlobalUserData.uid

        val callback = object : OnBackPressedCallback(true /* default to enabled */) {
            override fun handleOnBackPressed() {

                val intent = Intent(this@PaymentConfirm, PaymentConfirm::class.java)
                startActivity(intent)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        val paidButton = findViewById<Button>(R.id.confirmedButton)
        val paidImage = findViewById<ImageView>(R.id.imageView5)
        paidButton.setOnClickListener {
            if (uid != null){
                updateUser(uid)
            }
        }
        paidImage.setOnClickListener{
            if (uid != null){
                updateUser(uid)
            }
        }



    }
    private fun updateUser(uid: String) {

        db.collection("users")
            .whereEqualTo("UID", uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e("EditAccountActivity", "No matching account found")
                    return@addOnSuccessListener
                }

                val account = documents.first()
                data class AccountStatus(val date: Date, val status: String)
                val accountStatus = AccountStatus(date = Date(), status = "Active")
                account.reference.update("account_status", accountStatus)
                    .addOnSuccessListener {
                        Log.d("EditAccountActivity", "Account Updated successfully")
                       updateUI(uid)
                    }
                    .addOnFailureListener { e ->
                        Log.e("EditAccountActivity", "Error Updating account", e)
                    }
            }
    }
    private fun updateUI(user: String) {
        if (user != null) {
            // User is signed in, show success message
            
            GlobalUserData.uid = ""
            // Navigate to the Login activity
            val intent = Intent(this, Login::class.java)

            startActivity(intent)
            finish() // Finish the current activity so the user can't go back to it
        } else {
            // User is null, stay on the register page or show an error message
            Toast.makeText(this, "Register failed.", Toast.LENGTH_SHORT).show()
        }
    }
}


