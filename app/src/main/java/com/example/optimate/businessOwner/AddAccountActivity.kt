package com.example.optimate.businessOwner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.optimate.R
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.example.optimate.loginAndRegister.GlobalUserData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.util.Date

class AddAccountActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_account)

        val titlesDropDown = findViewById<AutoCompleteTextView>(R.id.title)
        val name = findViewById<EditText>(R.id.employeeName)
        val email = findViewById<EditText>(R.id.employeeEmail)
        val password = findViewById<EditText>(R.id.employeePassword)
        val wage = findViewById<EditText>(R.id.amount)
        val submitBtn = findViewById<Button>(R.id.submitBtn)
        val homeBtn = findViewById<ImageView>(R.id.homeBtn)

        homeBtn.setOnClickListener {
            val intent = Intent(this, DynamicLandingActivity::class.java)
            startActivity(intent)
            finish()
        }

        val titlesList = arrayListOf<String>()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, titlesList)
        titlesDropDown.setAdapter(adapter)

        fetchTitlesAndUpdateAdapter(titlesList, adapter)

        submitBtn.setOnClickListener {
            // Get values from EditTexts and AutoCompleteTextView
            val nameText = name.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val wageText = wage.text.toString().trim()
            val titleText = titlesDropDown.text.toString().trim()

            // Convert wageText to Float
            val wageValue = wageText.toFloatOrNull()

            // Validate the inputs
            if (nameText.isEmpty() || emailText.isEmpty() || passwordText.isEmpty() || wageValue == null || titleText.isEmpty()) {
                Toast.makeText(this, "Please fill all the fields correctly.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(!isStrongPassword(passwordText)){
                Toast.makeText(this, "Password does not meet requirements", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Assuming role needs to be fetched based on title
            fetchTitleToRole(titleText) { role ->
                if (role != null) {
                    registerEmployee(nameText, emailText, passwordText, wageValue, titleText, role)
                } else {
                    Toast.makeText(this, "Failed to fetch role for the title. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
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

    private fun fetchTitlesAndUpdateAdapter(
        titlesList: ArrayList<String>,
        adapter: ArrayAdapter<String>
    ) {
        val db = FirebaseFirestore.getInstance()
        val bid = GlobalUserData.bid

        db.collection("titles")
            .whereEqualTo("bid", bid)
            .get()
            .addOnSuccessListener { documents ->
                titlesList.clear()
                for (document in documents) {
                    val title = document.getString("title")
                    title?.let { titlesList.add(it) }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error getting documents: ", exception)
            }
    }

    private fun registerEmployee(name: String, email: String, password: String, wage: Float, title: String, role: String) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    auth.signOut()

                    // Re-sign in the business account
                    signInBusinessOwner { isSuccessful ->
                        if (isSuccessful) {
                            addEmployeeToDatabase(uid, name, email, password, wage, title, role)
                            Log.d("registerEmployee", "createUserWithEmail:success")
                            Toast.makeText(this, "Employee added successfully.", Toast.LENGTH_SHORT).show()
                            navigateToAccountsActivity()
                        } else {
                            Log.e("registerEmployee", "Failed to sign in business owner after adding employee.")
                        }
                    }
                } else {
                    Log.w("registerEmployee", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this, "Employee registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun addEmployeeToDatabase(uid: String?, name: String, email: String, password: String, wage: Float, title: String, role: String) {
        val bid = GlobalUserData.bid
        data class AccountStatus(val date: Date, val status: String)
        val accountStatus = AccountStatus(date = Date(), status = "Created")


        val user = hashMapOf(
            "UID" to uid,
            "email" to email,
            "name" to name,
            "title" to title,
            "role" to role,
            "BID" to bid,
            "wage" to wage,
            "initial_password" to password,
            "account_status" to accountStatus,
            "modules" to GlobalUserData.modules
        )
        Log.d("Hison", "addEmployeeToDatabase: $user")
        //add user to users collection
        db.collection("users")
            .add(user)
            .addOnSuccessListener {
                Log.d("Hison", "Success adding user")
            }
            .addOnFailureListener { e ->
                Log.w("Hison", "Error adding document", e)
            }
    }

    private fun signInBusinessOwner(completion: (Boolean) -> Unit) {
        val email = GlobalUserData.email
        val password = GlobalUserData.password

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "BusinessOwnerSignIn:success")
                    completion(true)
                } else {
                    Log.w("FirebaseAuth", "BusinessOwnerSignIn:failure", task.exception)
                    completion(false)
                }
            }
    }


    private fun fetchTitleToRole(title: String, callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("titles")
            .whereEqualTo("title", title)
            .whereEqualTo("bid", GlobalUserData.bid)
            .get()
            .addOnSuccessListener { documents ->
                val role = documents.firstOrNull()?.getString("role")
                callback(role)
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error getting documents: ", exception)
                callback(null)
            }
    }
    private fun navigateToAccountsActivity() {
        val intent = Intent(this, AccountsActivity::class.java)
        startActivity(intent)
        finish()
    }

}



