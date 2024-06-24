package com.example.optimate.employeeFlow

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.text.TextPaint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.MutableLiveData
import com.example.optimate.R
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.example.optimate.loginAndRegister.GlobalUserData
import com.example.optimate.loginAndRegister.biWeeklyDateRanges2024
import com.example.optimate.loginAndRegister.getBusinessNameAndAddress
import com.example.optimate.loginAndRegister.milliSecondsToHours
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ViewAllPayStubs : AppCompatActivity() {
    private val yearlyTotalHour = MutableLiveData<Double>().apply { value = 0.0 }
    private val yearlyIncome = MutableLiveData<Double>().apply { value = 0.0 }
    private val yearlyWorkLogs = mutableStateListOf<Map<String, Any>>()
    private lateinit var validBiWeeklyDateRanges: List<List<String>>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_all_pay_stubs)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val dateRange = listOf("20240101", today)
        validBiWeeklyDateRanges = biWeeklyDateRanges2024.filter { it[0] <= today }
        validBiWeeklyDateRanges = validBiWeeklyDateRanges.dropLast(1)
        val previousBiWeeklyDateRanges = validBiWeeklyDateRanges.last()
        Log.d("ViewAllPayStubs", "previousBiWeeklyDateRanges: $previousBiWeeklyDateRanges")
        getWorkedHoursForDateRange(
            listOf("20240101", previousBiWeeklyDateRanges[1]),
            yearlyWorkLogs,
            yearlyTotalHour,
            yearlyIncome
        )


        val totalGrossPayAnnual = findViewById<TextView>(R.id.totalGrossPayAnnual)
        val totalNetPayAnnual = findViewById<TextView>(R.id.netPayTotalAnnual)
        val totalTaxesAnnual = findViewById<TextView>(R.id.taxesTotalAnnual)
        val expenses = findViewById<TextView>(R.id.expenses)
        val claimed = findViewById<TextView>(R.id.claimed)

        getExpensesByDateRange(dateRange) {totalExpenses,totalClaimed ->
            // Set total expenses and total claimed to TextViews
            expenses.text = "$${String.format("%.2f", totalExpenses)}"
            claimed.text = "$${String.format("%.2f", totalClaimed)}"
        }

        yearlyIncome.observe(this) { income ->
            totalGrossPayAnnual.text = "$${String.format("%.2f", income)}"
            totalNetPayAnnual.text = "$${String.format("%.2f", income?.times(0.8))}"
            totalTaxesAnnual.text = "$${String.format("%.2f", income?.times(0.2))}"
        }

        val homeBtn = findViewById<ImageView>(R.id.homeBtn)
        homeBtn.setOnClickListener {
            startActivity(Intent(this, DynamicLandingActivity::class.java))
        }

        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("MMMM dd, YYYY", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate)
        val todaysDate: TextView = findViewById(R.id.todaysDate)
        todaysDate.text = "As of $formattedDate:"



        val composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            ViewAllPayStubsList(validBiWeeklyDateRanges, this@ViewAllPayStubs)
        }
    }

    private fun getWorkedHoursForDateRange(
        dateRange: List<String>,
        approvedWorkLogs: MutableList<Map<String, Any>>,
        hours: MutableLiveData<Double>,
        income: MutableLiveData<Double>,
    ) {
        val db = Firebase.firestore
        val startDate = SimpleDateFormat("yyyyMMdd").parse(dateRange[0])
        val endDate = SimpleDateFormat("yyyyMMdd").parse(dateRange[1])

        db.collection("totalHours")
            .whereEqualTo("bid", GlobalUserData.bid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.documents.forEach { documentSnapshot ->
                    val workLogs = documentSnapshot.data as Map<String, Any>?
                    workLogs?.let {
                        val userLogs = it[GlobalUserData.uid] as? List<Map<String, Any>>
                        userLogs?.forEach { log ->
                            val approved = log["approved"] as Boolean?
                            if (approved == true) {
                                log.keys.filter { it.matches(Regex("\\d{8}")) }.forEach { dateKey ->
                                    try {
                                        val logDate = SimpleDateFormat("yyyyMMdd").parse(dateKey)
                                        if (logDate in startDate..endDate) {
                                            val hours = log[dateKey] as? Long
                                                ?: 0L // Assuming hours are stored as Long
                                            val wage = (log["wage"] as? Number)?.toDouble() ?: 0.0
                                            // Create a new map that includes the UID, the date, and the hours
                                            val logWithUid = mapOf(
                                                "date" to dateKey,
                                                "hours" to hours,
                                                "wage" to wage
                                            )
                                            approvedWorkLogs.add(logWithUid)
                                        }
                                    } catch (e: Exception) {
                                        // Handle date parsing exception
                                    }
                                }
                            }
                        }
                    }
                }
                //call countTotalHoursAndIncome to calculate the total hours and income
                countTotalHoursAndIncome(approvedWorkLogs, hours, income)

            }
            .addOnFailureListener { e ->
                Log.e("ViewAllPayStubs", "Error getting documents: ", e)
            }
    }

    private fun countTotalHoursAndIncome(
        workLogs: List<Map<String, Any>>,
        hours: MutableLiveData<Double>,
        income: MutableLiveData<Double>
    ) {
        var totalHours = 0.0
        var totalIncome = 0.0
        workLogs.forEach { log ->
            val totalHour = log["hours"] as Long
            val wage = log["wage"] as Double
            val theIncome = milliSecondsToHours(totalHour) * wage
            totalHours += milliSecondsToHours(totalHour)
            totalIncome += theIncome

        }
        hours.postValue(String.format("%.2f", totalHours).toDouble())
        income.postValue(String.format("%.2f", totalIncome).toDouble())
    }

}
