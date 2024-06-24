package com.example.optimate.employeeFlow

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import com.example.optimate.R
import com.example.optimate.loginAndRegister.GlobalUserData
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.TimeZone

class ViewHistory : AppCompatActivity() {
    private val db = Firebase.firestore
    private val workLogsList = mutableStateListOf<Map<String, Any>>()
    private val filteredWorkLogsList = mutableStateListOf<Map<String, Any>>()
    private var startDate = ""
    private var endDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWorkLogs()

        setContent{
            ViewHistoryScreen(showDateRangePicker = { showDateRangePicker(it) }, workLogs = filteredWorkLogsList)
        }
    }
    private fun showDateRangePicker(updateDateRange: (String, String) -> Unit) {
        // Create the date range picker builder
        val builder = MaterialDatePicker.Builder.dateRangePicker().apply {
            setTheme(R.style.CustomDatePickerStyle)
        }
        builder.setTitleText("Select dates to view history")

        // Get the current month
        val currentMonth = Calendar.getInstance()

        // Set up the constraints to open the picker at the current month
        val constraintsBuilder = CalendarConstraints.Builder()
        constraintsBuilder.setOpenAt(currentMonth.timeInMillis) // Open picker at the current month

        builder.setCalendarConstraints(constraintsBuilder.build())

        // Create the date picker
        val picker = builder.build()

        // Add the event when the date is confirmed
        picker.addOnPositiveButtonClickListener { selection ->
            val start = convertTimestampToDate(selection.first)
            val end = convertTimestampToDate(selection.second)
            updateDateRange(start, end)
            onDateRangeSelected(start, end)
        }

        // Show the date picker
        picker.show(supportFragmentManager, picker.toString())
    }

    private fun convertTimestampToDate(timestamp: Long): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")) // Set to UTC
        calendar.timeInMillis = timestamp
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1 // January is 0
        val year = calendar.get(Calendar.YEAR)
        val formattedDay = if (day <= 9) "0$day" else "$day"
        val formattedMonth = if (month <= 9) "0$month" else "$month"

        return "$formattedMonth/$formattedDay/$year"
    }
    private fun getWorkLogs() {
        val bid = GlobalUserData.bid
        val uid = GlobalUserData.uid
        db.collection("workLogs")
            .whereEqualTo("uid", uid)
            .whereEqualTo("bid", bid)
            .get()
            .addOnSuccessListener { documents ->
                workLogsList.clear() // Clear the current list to avoid duplicates
                for (document in documents) {
                    val data = document.data.toMutableMap()
                    data.remove("uid")
                    data.remove("bid")
                    workLogsList.add(data)
                }
                filterWorkLogsByDateRange() // Filter the logs after fetching
            }
            .addOnFailureListener { exception ->
                Log.w("WorkLogs", "Error getting documents: ", exception)
            }
    }

    private fun filterWorkLogsByDateRange() {
        // Perform filtering only if both dates are set
        if (startDate.isNotBlank() && endDate.isNotBlank()) {
            // Clear the existing filtered list
            filteredWorkLogsList.clear()

            val formatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
            } else {
                TODO("VERSION.SDK_INT < O")
            }
            val startLocalDate = LocalDate.parse(startDate, formatter)
            val endLocalDate = LocalDate.parse(endDate, formatter)

            // Manually filter workLogsList and update filteredWorkLogsList
            for (workLog in workLogsList) {
                // Create a new map for the filtered work log entries
                val filteredWorkLog = mutableMapOf<String, Any>()

                for (key in workLog.keys) {
                    val workLogDate = LocalDate.parse(key, DateTimeFormatter.ofPattern("yyyyMMdd"))

                    // Check if the workLogDate is within the range
                    if ((workLogDate.isEqual(startLocalDate) || workLogDate.isAfter(startLocalDate)) &&
                        (workLogDate.isEqual(endLocalDate) || workLogDate.isBefore(endLocalDate))) {
                        // If it is within the range, add this entry to the filteredWorkLog
                        filteredWorkLog[key] = workLog[key]!!
                    }
                }

                // If the filteredWorkLog is not empty, add it to the filteredWorkLogsList
                if (filteredWorkLog.isNotEmpty()) {
                    filteredWorkLogsList.add(filteredWorkLog)

                }
            }
        }
    }

    private fun onDateRangeSelected(start: String, end: String) {
        startDate = start
        endDate = end
        filterWorkLogsByDateRange()
    }
}