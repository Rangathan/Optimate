package com.example.optimate.businessOwner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import com.example.optimate.loginAndRegister.GlobalUserData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


class FinancesDetailActivity: AppCompatActivity(){
    private val db = Firebase.firestore
    private val bid = GlobalUserData.bid
    private val financesList = mutableStateListOf<Finances>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val finances = financesList
            finances.sortByDescending  { finance ->
                SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }.parse(finance.date)
            }
            FinancesDetailScreen(finances = finances)
        }
    }
    override fun onResume() {
        super.onResume()
        fetchFinances()
    }

    private fun fetchFinances() {
        financesList.clear()
        fetchExpenseOrRevenue("Revenues")
        fetchExpenseOrRevenue("Expenses")


    }

    private fun fetchExpenseOrRevenue(financesType:String){
        db.collection("finances")
            .whereEqualTo("BID", bid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.documents.forEach { documentSnapshot ->
                    // Use a safe cast and check if the result is a list of maps
                    val finances = documentSnapshot.get(financesType)
                    if (finances is List<*>) {
                        finances.forEach { finance ->
                            if (finance is Map<*, *>) {
                                // Perform a safe cast for each expected value
                                val approval = finance["Approval"] as? Boolean ?: false
                                if (approval) {
                                    val amount = (finance["Amount"] as? Number)?.toString() ?: "0"
                                    val timestamp = finance["Date"] as? com.google.firebase.Timestamp
                                    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).apply {
                                        timeZone = TimeZone.getDefault()
                                    }
                                    val date = timestamp?.toDate()?.let { sdf.format(it) } ?: "Unknown"
                                    val description = finance["Description"] as? String ?: "No description"
                                    val name = finance["Name"] as? String ?: "Unknown"
                                    financesList.add(
                                        Finances(financesType, amount, date, description, name))
                                }
                            }
                        }
                    }
                }
            }
    }
}

