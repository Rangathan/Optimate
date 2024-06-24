package com.example.optimate.loginAndRegister
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.optimate.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import java.util.Date
import android.content.Context
import android.content.SharedPreferences
import android.widget.CheckBox
import androidx.activity.OnBackPressedCallback


class Login : AppCompatActivity(){
    private lateinit var auth: FirebaseAuth
    private var registerBtn: Button? = null
    private lateinit var forgotPasswordClk: TextView
    private lateinit var loginBtn: Button
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private val db = Firebase.firestore
    private lateinit var rememberPasswordCheckBox: CheckBox
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        val callback = object : OnBackPressedCallback(true /* default to enabled */) {
            override fun handleOnBackPressed() {
                // Start the same activity again
                val intent = Intent(this@Login, Login::class.java)
                startActivity(intent)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)


        // Get references to the EditTexts and Buttons in the layout
        emailEditText = findViewById(R.id.loginEmail)
        passwordEditText = findViewById(R.id.loginPassword)
        loginBtn = findViewById(R.id.loginBtn)
        registerBtn = findViewById(R.id.registerBtn)
        forgotPasswordClk = findViewById(R.id.forgotPassword)
        rememberPasswordCheckBox = findViewById(R.id.rememberPassword)

        // Retrieve SharedPreferences
        sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)

        // Check if remember password was checked previously
        if (sharedPreferences.getBoolean("rememberPassword", false)) {
            emailEditText.setText(sharedPreferences.getString("email", ""))
            passwordEditText.setText(sharedPreferences.getString("password", ""))
            rememberPasswordCheckBox.isChecked = true
        }






        // Set up the login button click listener
        loginBtn.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Check for non-empty fields
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sign in with Firebase
            signInWithEmail(email, password)
            GlobalUserData.password = password
            if (rememberPasswordCheckBox.isChecked) {
                // Save email and password to SharedPreferences if remember password is checked
                val editor = sharedPreferences.edit()
                editor.putString("email", email)
                editor.putString("password", password)
                editor.putBoolean("rememberPassword", true)
                editor.apply()
            } else {
                // Clear email and password from SharedPreferences if remember password is unchecked
                val editor = sharedPreferences.edit()
                editor.remove("email")
                editor.remove("password")
                editor.remove("rememberPassword")
                editor.apply()
            }
        }

        // Set up the register button click listener
        registerBtn?.setOnClickListener {
            // Go to the Register activity
            startActivity(Intent(this, Register::class.java))
        }
        forgotPasswordClk.setOnClickListener {
            startActivity(Intent(this, PasswordReset::class.java))
        }

        // Add a TextWatcher to the email EditText
        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Check for spaces and remove them
                if (s != null && s.contains(" ")) {
                    val trimmedText = s.toString().replace(" ", "")
                    emailEditText.setText(trimmedText)
                    emailEditText.setSelection(trimmedText.length)
                }
            }
        })

    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = auth.currentUser
                    getUserData(user)

                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Go to your main activity or another activity after login
            if(GlobalUserData.account_status.status == "Active") {
                val intent = Intent(this, DynamicLandingActivity::class.java)
                intent.putExtra("USER_UID", user.uid)
                startActivity(intent)

                finish()
            }else if(GlobalUserData.role == "businessOwner" && GlobalUserData.account_status.status == "Pending"){
                val intent = Intent(this, ModuleChoosingMain::class.java)
                intent.putExtra("USER_UID", user.uid)
                startActivity(intent)
                finish()
            }else if(GlobalUserData.account_status.status == "Created"){
                val intent = Intent(this, NewUserPasswordChange::class.java)
                startActivity(intent)
                finish()
            }else if(GlobalUserData.account_status.status == "Deleted") {
                auth.signOut()
                Toast.makeText(this, "This Account has been Deleted, Please contact support", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Stay on the login page or show error
        }
    }
    private fun getUserData(user: FirebaseUser?) {
        if (user != null) {
            val uid = user.uid
            val docRef = db.collection("users").whereEqualTo("UID", uid)
            docRef.get()
                .addOnSuccessListener { querySnapshot ->

                    for (document in querySnapshot.documents) {
                        // Access data for each document
                        GlobalUserData.name = (document.getString("name") ?: "").toString()
                        GlobalUserData.uid = (document.getString("UID") ?: "").toString()
                        GlobalUserData.bid = (document.getString("BID") ?: "").toString()
                        GlobalUserData.email = (document.getString("email") ?: "").toString()
                        GlobalUserData.address = (document.getString("address") ?: "").toString()
                        GlobalUserData.role = (document.getString("role") ?: "").toString()
                        GlobalUserData.title = (document.getString("title") ?: "").toString()
                        GlobalUserData.phone = (document.getString("phone") ?: "").toString()
                        GlobalUserData.wage = (document.getDouble("wage") ?: 0.0).toFloat()
                        GlobalUserData.profilePic = (document.getString("profilePic") ?: "").toString()

                        GlobalUserData.deviceToken = (document.getString("deviceToken") ?: "").toString()
                        val accountStatusObject = document.get("account_status") as? Map<String, Any>
                        if (accountStatusObject != null) {
                            val date = accountStatusObject["date"] as? Date ?: Date()
                            val status = accountStatusObject["status"] as? String ?: ""
                            GlobalUserData.account_status = AccountStatus(date, status)
                        } else {
                            // Handle case where account_status is not found in the document
                            GlobalUserData.account_status = AccountStatus(Date(), "")
                        }
                        GlobalUserData.modules = document.get("modules") as? List<String> ?: emptyList()
                        GlobalUserData.first_time = (document.getBoolean("first_time") ?: false)
                    }
                    updateUI(user)

                }
                .addOnFailureListener {e ->
                    Toast.makeText(baseContext, "Get Failed $e",
                        Toast.LENGTH_SHORT).show()
                }

        }

    }

}


