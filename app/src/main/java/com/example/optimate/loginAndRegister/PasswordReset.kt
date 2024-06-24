package com.example.optimate.loginAndRegister

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.optimate.R
import com.google.firebase.auth.FirebaseAuth

class PasswordReset : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var resetBtn: Button? = null
    private lateinit var emailEditText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_reset)
        resetBtn = findViewById(R.id.resetBtn)
        emailEditText = findViewById(R.id.resetEmail)

        resetBtn?.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty() ) {
                Toast.makeText(this, "Please enter email.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                sendPasswordReset(email)
            }


        }

    }
    private fun sendPasswordReset(email: String){
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if(task.isSuccessful ){
                    Toast.makeText(this, "An email will be sent if have an account with us", Toast.LENGTH_SHORT).show()
                    updateUI(email)
                }else{
                    Toast.makeText(this, "Please enter a valid email",Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun updateUI(email: String) {
        if (email != null) {
            // Go to your main activity or another activity after login
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        } else {
            // Stay on the login page or show error
        }
    }
}