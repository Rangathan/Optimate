package com.example.optimate.businessOwner

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.example.optimate.loginAndRegister.GlobalUserData
import com.example.optimate.loginAndRegister.ModuleChoosingMain
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class FinancesActivity : AppCompatActivity() {

    data class Amount(val type: String, val amount: String)
    data class AmountWithDate(val type: String, val amount: String, val date: String)

    private val db = Firebase.firestore
    private val bid = GlobalUserData.bid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Use remember with mutableStateOf to initialize the state
            val totalRevenues = remember { mutableDoubleStateOf(0.0) }
            val totalExpenses = remember { mutableDoubleStateOf(0.0) }
            val amountWithDate = remember { mutableStateListOf<AmountWithDate>() }

            // Update the UI with the latest values
            FinancesScreen(
                revenues = totalRevenues.doubleValue,
                expenses = totalExpenses.doubleValue,
                amountWithDate = amountWithDate

            )
        }
        val callback = object : OnBackPressedCallback(true /* default to enabled */) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@FinancesActivity, DynamicLandingActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onResume() {
        super.onResume()
        fetchFinances()
    }

    private fun fetchFinances() {
        db.collection("finances")
            .whereEqualTo("BID", bid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val financesList = mutableListOf<Amount>()
                val financesListWithDate = mutableListOf<AmountWithDate>()

                querySnapshot.documents.forEach { documentSnapshot ->
                    listOf("Revenues", "Expenses").forEach { financesType ->
                        val finances = documentSnapshot.get(financesType)
                        if (finances is List<*>) {
                            finances.forEach { finance ->
                                if (finance is Map<*, *> && finance["Approval"] as? Boolean == true) {
                                    val amount = (finance["Amount"] as? Number)?.toString() ?: "0"
                                    val timestamp = finance["Date"] as? com.google.firebase.Timestamp
                                    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).apply {
                                        timeZone = TimeZone.getDefault()
                                    }
                                    val date = timestamp?.toDate()?.let { sdf.format(it) } ?: "Unknown"
                                    val amounts = Amount(financesType, amount)
                                    val amountsWithDate = AmountWithDate(financesType, amount, date)
                                    financesList.add(amounts)
                                    financesListWithDate.add(amountsWithDate)

                                }
                            }
                        }
                    }
                }

                val totalExpenses = financesList.filter { it.type == "Expenses" }.sumOf { it.amount.toDouble() }
                val totalRevenues = financesList.filter { it.type == "Revenues" }.sumOf { it.amount.toDouble() }

                // Update your UI here with the fetched data
                updateUI(totalRevenues, totalExpenses, financesListWithDate)
            }
    }

    private fun updateUI(monthlyRevenues: Double, monthlyExpenses: Double, amountWithDate: List<AmountWithDate>) {
        // You need to use the main thread to update the UI
        runOnUiThread {
            setContent {
                FinancesScreen(
                    revenues = monthlyRevenues,
                    expenses = monthlyExpenses,
                    amountWithDate = amountWithDate

                )
            }
        }
    }
}
