package com.example.optimate.businessOwner

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.example.optimate.loginAndRegister.GlobalUserData
import com.example.optimate.loginAndRegister.ModuleChoosingMain
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AccountsActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val bid = GlobalUserData.bid
    private val accountsList = mutableStateListOf<Account>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val accounts = remember { accountsList }
            AccountsScreen(accounts = accounts)
        }
        val callback = object : OnBackPressedCallback(true /* default to enabled */) {
            override fun handleOnBackPressed() {
                // Start the same activity again
                val intent = Intent(this@AccountsActivity, DynamicLandingActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    override fun onResume() {
        super.onResume()
        fetchAccounts(bid)
    }

    private fun fetchAccounts(bid: String) {
        db.collection("users")
            .whereEqualTo("BID", bid)
            .whereIn("account_status.status", listOf("Created", "Active")) // Modified line
            .get()
            .addOnSuccessListener { documents ->
                val fetchedAccounts = mutableListOf<Account>()
                for (document in documents) {
                    if(document.getString("role") != "businessOwner") {
                        val name = document.getString("name") ?: "N/A"
                        val title = document.getString("title") ?: "N/A"
                        val uid = document.getString("UID") ?: "N/A"
                        fetchedAccounts.add(Account(name, title, uid))
                    }
                }

                accountsList.clear()
                accountsList.addAll(fetchedAccounts)
            }
            .addOnFailureListener { exception ->
                // Handle the error appropriately
            }
    }

}
