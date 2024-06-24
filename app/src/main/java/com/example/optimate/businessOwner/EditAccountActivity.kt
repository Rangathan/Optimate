package com.example.optimate.businessOwner
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.example.optimate.R
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.example.optimate.loginAndRegister.GlobalUserData
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.util.Date

class EditAccountActivity : AppCompatActivity() {
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account)

        val titlesDropDown = findViewById<AutoCompleteTextView>(R.id.title)
        val titlesList = arrayListOf<String>()
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, titlesList), Filterable {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        return FilterResults().apply {
                            values = titlesList
                            count = titlesList.size
                        }
                    }

                    override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                        if (results.count > 0) {
                            notifyDataSetChanged()
                        } else {
                            notifyDataSetInvalidated()
                        }
                    }
                }
            }
        }
        titlesDropDown.setAdapter(adapter)

        val name = findViewById<TextInputEditText>(R.id.employeeName)
        val email = findViewById<TextInputEditText>(R.id.employeeEmail)
        val password = findViewById<TextInputEditText>(R.id.employeePassword)
        val wage = findViewById<TextInputEditText>(R.id.amount)
        val submitBtn = findViewById<Button>(R.id.submitBtn)
        val homeBtn = findViewById<ImageView>(R.id.homeBtn)
        val deleteBtn = findViewById<ComposeView>(R.id.compose_view)
        val accountUid = intent.getStringExtra("account_uid")

        // Fetch and update titles list
        fetchTitlesAndUpdateAdapter(titlesList, adapter) {
            // After fetching the titles, set the default value if UID is not null
            accountUid?.let {
                fetchDetailsByUid(it, name, titlesDropDown, email, wage, password, titlesList)
            } ?: Log.d("EditAccountActivity", "Account UID is null")
        }

        homeBtn.setOnClickListener {
            val intent = Intent(this, DynamicLandingActivity::class.java)
            startActivity(intent)
            finish()
        }

        submitBtn.setOnClickListener {
            val nameText = name.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val wageText = wage.text.toString().trim()
            val titleText = titlesDropDown.text.toString().trim()
            if(nameText.isEmpty() || emailText.isEmpty() || passwordText.isEmpty() || wageText.isEmpty() || titleText.isEmpty()) {
                Toast.makeText(this, "Please fill all the fields correctly.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateUserToDB(nameText, titleText, emailText, wageText, passwordText, accountUid!!)
        }

        deleteBtn.setContent {
            var showDialog by remember { mutableStateOf(false) }

            if (showDialog) {
                DeleteAccountDialog(
                    onConfirm = {
                        showDialog = false
                        deleteUser(accountUid!!)
                    },
                    onDismiss = {
                        showDialog = false
                    }
                )
            }

            DeleteAccount(
                onClick = {
                    showDialog = true
                }
            )
        }
    }
    private fun fetchDetailsByUid(
        uid: String,
        name: TextInputEditText,
        title: AutoCompleteTextView,
        email: TextInputEditText,
        wage: TextInputEditText,
        password: TextInputEditText,
        titlesList: List<String>
    ) {
        db.collection("users")
            .whereEqualTo("UID", uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.isEmpty) {
                    Log.e("EditAccountActivity", "No matching account found")
                    return@addOnSuccessListener
                }
                val account = document.first()
                val accountName = account.getString("name") ?: ""
                val accountTitle = account.getString("title") ?: ""
                val accountEmail = account.getString("email") ?: ""
                val accountWage = account.getDouble("wage")?.toString() ?: ""
                val accountPassword = account.getString("initial_password") ?: ""

                name.setText(accountName)
                email.setText(accountEmail)
                email.isEnabled = false
                wage.setText(accountWage)
                password.setText(accountPassword)
                password.isEnabled = false

                if (titlesList.contains(accountTitle)) {
                    title.setText(accountTitle, false)
                } else {
                    Log.e("EditAccountActivity", "Title not found in titles list")
                }
            }
    }

    private fun fetchTitlesAndUpdateAdapter(
        titlesList: ArrayList<String>,
        adapter: ArrayAdapter<String>,
        onTitlesFetched: () -> Unit // Callback to execute after titles are fetched
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
                Log.d("EditAccountActivity", "Fetched titles: $titlesList")
                adapter.notifyDataSetChanged()
                onTitlesFetched() // Call the callback
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error getting documents: ", exception)
            }
    }
    private fun updateUserToDB(
        name: String,
        title: String,
        email: String,
        wage: String,
        password: String,
        accountUid: String
    ) {
        val user = hashMapOf(
            "name" to name,
            "title" to title,
            "email" to email,
            "wage" to wage.toDouble(),
            "initial_password" to password
        )

        db.collection("users")
            .whereEqualTo("UID", accountUid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e("EditAccountActivity", "No matching account found")
                    return@addOnSuccessListener
                }
                val account = documents.first()
                account.reference.update(user as Map<String, Any>)
                    .addOnSuccessListener {
                        Log.d("EditAccountActivity", "Account updated successfully")
                        navigateToAccountsActivity()
                    }
                    .addOnFailureListener { e ->
                        Log.e("EditAccountActivity", "Error updating account", e)
                    }
            }
    }
    private fun navigateToAccountsActivity() {
        val intent = Intent(this, AccountsActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun deleteUser(uid: String) {

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
                val accountStatus = AccountStatus(date = Date(), status = "Deleted")
                account.reference.update("account_status", accountStatus)
                    .addOnSuccessListener {
                        Log.d("EditAccountActivity", "Account deleted successfully")
                        navigateToAccountsActivity()
                    }
                    .addOnFailureListener { e ->
                        Log.e("EditAccountActivity", "Error deleting account", e)
                    }
            }
    }

}
