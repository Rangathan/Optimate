package com.example.optimate.employeeFlow

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.optimate.R
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.example.optimate.loginAndRegister.GlobalUserData
import com.example.optimate.loginAndRegister.biWeeklyDateRanges2024
import com.example.optimate.loginAndRegister.milliSecondsToHours
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PayStub : AppCompatActivity() {

    private val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

    private val previousApprovedWorkLogs = mutableStateListOf<Map<String, Any>>()
    private var previousBiWeek = mutableStateListOf<String>()
    private val previousBiWeekTotalHour = MutableLiveData<Double>().apply { value = 0.0 }
    private val previousBiWeekIncome = MutableLiveData<Double>().apply { value = 0.0 }

    private val secPreviousApprovedWorkLogs = mutableStateListOf<Map<String, Any>>()
    private var secPreviousBiWeek = mutableStateListOf<String>()
    private val secPreviousBiWeekTotalHour = MutableLiveData<Double>().apply { value = 0.0 }
    private val secPreviousBiWeekIncome = MutableLiveData<Double>().apply { value = 0.0 }

    private val thirdPreviousApprovedWorkLogs = mutableStateListOf<Map<String, Any>>()
    private var thirdPreviousBiWeek = mutableStateListOf<String>()
    private val thirdPreviousBiWeekTotalHour = MutableLiveData<Double>().apply { value = 0.0 }
    private val thirdPreviousBiWeekIncome = MutableLiveData<Double>().apply { value = 0.0 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_stub)
        getBiWeek(biWeeklyDateRanges2024,today)
        getWorkedHoursForDateRange(previousBiWeek, previousApprovedWorkLogs)
        getWorkedHoursForDateRange(secPreviousBiWeek, secPreviousApprovedWorkLogs)
        getWorkedHoursForDateRange(thirdPreviousBiWeek, thirdPreviousApprovedWorkLogs)

        previousBiWeekIncome.observe(this, Observer { income ->
            // Assuming you want to update the pay stub when the income changes
            showPayStub(income * 0.8, income)

            val grossAmount = findViewById<TextView>(R.id.grossAmount)
            grossAmount.text = "$${String.format("%.2f", income)}"
            val netPayAmount = findViewById<TextView>(R.id.netPayAmount)
            netPayAmount.text = "$${String.format("%.2f", income?.times(0.8))}"
            val taxesAmount = findViewById<TextView>(R.id.taxesAmount)
            taxesAmount.text = "$${String.format("%.2f", income?.times(0.2))}"
        })

        previousBiWeekTotalHour.observe(this, Observer { hours ->
            val totalHours = findViewById<TextView>(R.id.totalHours)
            totalHours.text = "${String.format("%.2f", hours)}hrs"
        })

        secPreviousBiWeekIncome.observe(this, Observer { income ->
            val previousBiWeekIncome = findViewById<TextView>(R.id.previousAmount)
            previousBiWeekIncome.text = "Net pay: $${String.format("%.2f", income?.times(0.8))}"
        })

        thirdPreviousBiWeekIncome.observe(this, Observer { income ->
            val secPreviousBiWeekIncome = findViewById<TextView>(R.id.secPreviousAmount)
            secPreviousBiWeekIncome.text = "Net pay: $${String.format("%.2f", income?.times(0.8))}"
        })


        val currentBiWeekText = findViewById<TextView>(R.id.currentBiWeek)
        //change YYYYMMDD to YYYY/MM/DD
        val startDate = "${previousBiWeek[0].substring(0,4)}/${previousBiWeek[0].substring(4,6)}/${previousBiWeek[0].substring(6,8)}"
        val endDate= "${previousBiWeek[1].substring(0,4)}/${previousBiWeek[1].substring(4,6)}/${previousBiWeek[1].substring(6,8)}"
        currentBiWeekText.text = "${startDate} - ${endDate}"

        val previousBiWeekText = findViewById<TextView>(R.id.previousBiWeek)
        val startDate2 = "${secPreviousBiWeek[0].substring(0,4)}/${secPreviousBiWeek[0].substring(4,6)}/${secPreviousBiWeek[0].substring(6,8)}"
        val endDate2= "${secPreviousBiWeek[1].substring(0,4)}/${secPreviousBiWeek[1].substring(4,6)}/${secPreviousBiWeek[1].substring(6,8)}"
        previousBiWeekText.text = "${startDate2} - ${endDate2}"

        val secPreviousBiWeekText = findViewById<TextView>(R.id.secPreviousBiWeek)
        val startDate3 = "${thirdPreviousBiWeek[0].substring(0,4)}/${thirdPreviousBiWeek[0].substring(4,6)}/${thirdPreviousBiWeek[0].substring(6,8)}"
        val endDate3= "${thirdPreviousBiWeek[1].substring(0,4)}/${thirdPreviousBiWeek[1].substring(4,6)}/${thirdPreviousBiWeek[1].substring(6,8)}"
        secPreviousBiWeekText.text = "${startDate3} - ${endDate3}"

        val viewMorePayStubsBtn = findViewById<Button>(R.id.viewMorePayStubsBtn)
        val homeBtn = findViewById<ImageView>(R.id.homeBtn)

        viewMorePayStubsBtn.setOnClickListener {
            startActivity(Intent(this,ViewAllPayStubs::class.java))
        }

        homeBtn.setOnClickListener {
            startActivity(Intent(this,DynamicLandingActivity::class.java))
        }

    }

    private fun showPayStub(netIncome: Double, maxIncome: Double) {
        // Find the CircularProgressIndicator
        val donutChart = findViewById<CircularProgressIndicator>(R.id.donutChart)

        // Calculate progress percentage
        val progressPercentage = (netIncome / maxIncome) * 100

        // Set indicator color
        donutChart.setIndicatorColor(getColor(R.color.light_green))

        // Set track color
        donutChart.trackColor = getColor(R.color.light_red)

        // Create ObjectAnimator for progress animation
        val progressAnimator = ObjectAnimator.ofInt(donutChart, "progress", 0, progressPercentage.toInt())
        progressAnimator.duration = 1000 // Duration of animation in milliseconds (adjust as needed)

        // Create AnimatorSet to play the progress animation
        val animatorSet = AnimatorSet()
        animatorSet.play(progressAnimator)
        animatorSet.start()
    }

    private fun getWorkedHoursForDateRange(dateRange: List<String>, approvedWorkLogs: MutableList<Map<String, Any>>) {
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
                                            val hours = log[dateKey] as? Long ?: 0L
                                            val wage = (log["wage"] as? Number)?.toDouble() ?: 0.0
                                            // Create a new map that includes the UID, the date, and the hours
                                            val logWithUid = mapOf("date" to dateKey, "hours" to hours, "wage" to wage)
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
                countTotalHoursAndIncome(previousApprovedWorkLogs, previousBiWeekTotalHour, previousBiWeekIncome)
                countTotalHoursAndIncome(secPreviousApprovedWorkLogs, secPreviousBiWeekTotalHour, secPreviousBiWeekIncome)
                countTotalHoursAndIncome(thirdPreviousApprovedWorkLogs, thirdPreviousBiWeekTotalHour, thirdPreviousBiWeekIncome)
                Log.d("WorkLogs", "Approved work logs: ${previousApprovedWorkLogs.joinToString(separator = ", ", transform = { it.toString() })}")
                Log.d("WorkLogs", "Total hours: $previousBiWeekTotalHour")
                Log.d("WorkLogs", "Total income: $previousBiWeekIncome")
            }
            .addOnFailureListener { e ->
                // Handle the error appropriately, maybe log or show a UI indication
                // For example: Log.e("WorkLogs", "Error fetching work logs", e)
            }
    }

    private fun countTotalHoursAndIncome(workLogs: List<Map<String, Any>>, hours: MutableLiveData<Double>, income: MutableLiveData<Double>){
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
    private fun getBiWeek(biWeeklyDateRanges2024: List<List<String>>, today: String) {
        biWeeklyDateRanges2024.forEachIndexed { index, dateRange ->
            val startDate = dateRange[0]
            val endDate = dateRange[1]

            // Check for the current bi-week
            if (today in startDate..endDate) {
                previousBiWeek = if(index >0){
                    biWeeklyDateRanges2024[(index - 1)].toMutableStateList()
                }else{
                    biWeeklyDateRanges2024[0].toMutableStateList()
                }
                secPreviousBiWeek = if(index >1){
                    biWeeklyDateRanges2024[(index - 2)].toMutableStateList()
                }else{
                    biWeeklyDateRanges2024[0].toMutableStateList()
                }
                thirdPreviousBiWeek = if(index >2){
                    biWeeklyDateRanges2024[(index - 3)].toMutableStateList()
                }else{
                    biWeeklyDateRanges2024[0].toMutableStateList()
                }

            }
        }

    }

}
