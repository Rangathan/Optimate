package com.example.optimate.loginAndRegister

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.optimate.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import java.util.Date

class NewUserPasswordChange : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_user_password_change)

        auth = FirebaseAuth.getInstance()
        val passwordEditText= findViewById<EditText>(R.id.changePasswordText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmChangePasswordText)
        val updateButton = findViewById<Button>(R.id.ChangePassBtn)

        updateButton.setOnClickListener {

            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            // Check for non-empty fields


            // Check if passwords match
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check for password length
            if (password.length < 8 && isStrongPassword(password)) {
                Toast.makeText(
                    this,
                    "Password is not within Password Requirements.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            updatePassword(password)
        }
    }
    private fun isStrongPassword(password: String): Boolean {
        val upperCasePattern = Regex("[A-Z]")
        val lowerCasePattern = Regex("[a-z]")
        val digitPattern = Regex("[0-9]")
        val specialCharPattern = Regex("[^A-Za-z0-9]")

        return password.length >= 8 &&
                upperCasePattern.containsMatchIn(password) &&
                lowerCasePattern.containsMatchIn(password) &&
                digitPattern.containsMatchIn(password) &&
                specialCharPattern.containsMatchIn(password)
    }

    private fun updatePassword(newPassword: String){
        val user = auth.currentUser
        user?.updatePassword(newPassword)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Password updated successfully
                    Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                     updateDB()
                } else {
                    // Password update failed
                    Toast.makeText(this, "Password update failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateDB() {
        if (GlobalUserData.uid != null) {
            db.collection("users")
                .whereEqualTo("UID", GlobalUserData.uid)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Log.e("EditAccountActivity", "No matching account found")
                        return@addOnSuccessListener
                    }

                    val account = documents.first()

                    val accountStatus = AccountStatus(date = Date(), status = "Active")

                    account.reference.update("account_status", accountStatus)
                        .addOnSuccessListener {
                            GlobalUserData.account_status = accountStatus
                            Log.d("EditAccountActivity", "Account Updated successfully")
                            updateUI(GlobalUserData.uid)
                        }
                        .addOnFailureListener { e ->
                            Log.e("EditAccountActivity", "Error Updating account", e)
                        }
                }

        }
        else{
            Toast.makeText(this, "Error, No User Data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(user: String){
        if (user != null) {
            // User is signed in, show success message
            // Navigate to the Login activity
            val intent = Intent(this, DynamicLandingActivity::class.java)

            startActivity(intent)
            finish() // Finish the current activity so the user can't go back to it
        } else {
            // User is null, stay on the register page or show an error message
            Toast.makeText(this, "Update Failed.", Toast.LENGTH_SHORT).show()
        }
    }





}
