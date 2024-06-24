package com.example.optimate.employeeFlow

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import com.example.optimate.R
import java.text.SimpleDateFormat
import java.util.*
import android.content.SharedPreferences
import android.util.Log
import android.widget.ImageView
import androidx.compose.ui.platform.ComposeView
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.example.optimate.loginAndRegister.GlobalUserData
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ClockModule : AppCompatActivity() {
    private lateinit var digitalClock: TextView
    private lateinit var clockInButton: Button
    private lateinit var clockOutButton: Button
    private var isInClockInState = true
    private var isOnBreak = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var sharedPreferences: SharedPreferences
    private val db = Firebase.firestore
    private lateinit var uid: String
    private var today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    private var workLogs = mutableListOf<Map<String, Any>>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("hi", "onCreate")

        setContentView(R.layout.activity_clock_module)
        //checkTodayScheduleAndClockIn()
        loadWorkLogs()

        digitalClock = findViewById(R.id.digitalClock)
        clockInButton = findViewById(R.id.clockIn)
        clockOutButton = findViewById(R.id.clockOut)
        sharedPreferences = getSharedPreferences("ClockingState", Context.MODE_PRIVATE)

        uid = GlobalUserData.uid // Set the current user's ID

        // Load the clocking and break states for the current user
        isInClockInState = sharedPreferences.getBoolean("${uid}_isInClockInState", false)
        isOnBreak = sharedPreferences.getBoolean("${uid}_isOnBreak", false)

        //checkForExistingClockIn()
        // Update UI based on loaded states
        updateButtonStates()

        val clockedInToday = sharedPreferences.getBoolean("${uid}_isClockedIn", false) &&
                sharedPreferences.getString("${uid}_clockInDate", "") == today

        if (clockedInToday) {
            val composeView = findViewById<ComposeView>(R.id.compose_view)
            composeView.setContent {
                WorkLogsList(worklogs = workLogs)
                Log.d("ClockedIn", "WorkLogs: $workLogs")
            }
        }else{
            workLogs.clear()
            saveWorkLogs()
        }

        val homeBtn = findViewById<ImageView>(R.id.homeBtn)
        homeBtn.setOnClickListener {
            val intent = Intent(this, DynamicLandingActivity::class.java)
            startActivity(intent)
        }

        val viewHistoryBtn = findViewById<Button>(R.id.viewHistory)
        viewHistoryBtn.setOnClickListener {
            val intent = Intent(this, ViewHistory::class.java)
            startActivity(intent)
        }

        // Initialize the handler for the digital clock updates
        handler.post(object : Runnable {
            override fun run() {
                updateDigitalClock()
                handler.postDelayed(this, 1000)
            }
        })

        // Set click listeners for Clock In and Clock Out buttons
        clockInButton.setOnClickListener {
            sharedPreferences.edit().apply {
                putBoolean("${uid}_isClockedIn", true)
                putString("${uid}_clockInDate", today)
                apply()
            }
            toggleClockState(clockInButton)
        }

        clockOutButton.setOnClickListener {
            toggleClockState(clockOutButton)
        }
    }

    private fun updateButtonStates() {
        val clockedOutToday = sharedPreferences.getBoolean("${uid}_isClockedOut", false) &&
                sharedPreferences.getString("${uid}_clockOutDate", "") == today

        if (clockedOutToday) {
            // User has clocked out today, disable the clock-in button
            clockInButton.isEnabled = false
            clockOutButton.isEnabled = false
            clockInButton.backgroundTintList = getColorStateList(R.color.light_grey)
            clockOutButton.backgroundTintList = getColorStateList(R.color.light_grey)
            clockInButton.setTextColor(getColor(R.color.grey))
            clockOutButton.setTextColor(getColor(R.color.grey))
            clockInButton.text = getString(R.string.already_clocked)
            clockOutButton.text = getString(R.string.already_clocked)
        } else {
            if (isOnBreak) {
                // UI for break state
                clockInButton.text = getString(R.string.end_break)
                clockInButton.backgroundTintList = getColorStateList(R.color.light_yellow)
                clockOutButton.isEnabled = false
            } else if (!isInClockInState) {
                // UI for clocked out state
                clockInButton.text = getString(R.string.clock_in)
                clockInButton.backgroundTintList = getColorStateList(R.color.light_green)
                clockOutButton.text = getString(R.string.clock_out)
                clockOutButton.isEnabled = false
                clockOutButton.backgroundTintList = getColorStateList(R.color.light_grey)
                clockOutButton.setTextColor(getColor(R.color.grey))
            } else {
                // UI for clocked in state
                clockInButton.text = getString(R.string.clock_out)
                clockInButton.backgroundTintList = getColorStateList(R.color.light_red)
                clockOutButton.text = getString(R.string.start_break)
                clockOutButton.isEnabled = true
                clockOutButton.backgroundTintList = getColorStateList(R.color.light_yellow)
            }
        }
    }

    private fun toggleClockState(clickedButton: Button) {
        when {
            clickedButton == clockInButton && clockInButton.text == getString(R.string.clock_in) -> {
                // Clock In state
                isInClockInState = true
                isOnBreak = false // Ensure not on break when clocking in
                saveWorkLogToDB("clockIn"){}

            }
            clickedButton == clockInButton && clockInButton.text == getString(R.string.clock_out) -> {
                // Clock Out state
                isInClockInState = false
                isOnBreak = false // Ensure not on break when clocking out
                saveWorkLogToDB("clockOut"){
                    calculateTotalHoursWorked()
                }
                sharedPreferences.edit().apply {
                    putBoolean("${uid}_isClockedOut", true)
                    putString("${uid}_clockOutDate", today)
                    apply()
                }
            }
            clickedButton == clockInButton && clockInButton.text == getString(R.string.start_break) -> {
                // Start Break state
                isOnBreak = true
                saveWorkLogToDB("breakStart"){}


            }
            clickedButton == clockInButton && clockInButton.text == getString(R.string.end_break) -> {
                // End Break state
                isOnBreak = false
                saveWorkLogToDB("breakEnd"){}
            }
            clickedButton == clockOutButton && clockOutButton.text == getString(R.string.start_break) -> {
                // Start Break state for Clock Out button
                isOnBreak = true
                saveWorkLogToDB("breakStart"){}
            }
        }

        // Save the clocking and break states
        sharedPreferences.edit().apply {
            putBoolean("${uid}_isInClockInState", isInClockInState)
            putBoolean("${uid}_isOnBreak", isOnBreak)
            apply()
        }

        // After toggling, update the button states
        updateButtonStates()
    }

    private fun saveWorkLogToDB(type: String, onComplete: () -> Unit) {
        val bid = GlobalUserData.bid
        val uid = GlobalUserData.uid
        // Format the current Date and Time into a readable string
        val dateTimeFormat = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault())
        val formattedDateTime = dateTimeFormat.format(Date())

        val entry = hashMapOf(
            type to formattedDateTime
        )
        workLogs.add(entry)
        saveWorkLogs()
        // Get the current date
        val currentDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        val clockedInToday = sharedPreferences.getBoolean("${uid}_isClockedIn", false) &&
                sharedPreferences.getString("${uid}_clockInDate", "") == today

        if (clockedInToday) {
            val composeView = findViewById<ComposeView>(R.id.compose_view)
            composeView.setContent {
                WorkLogsList(worklogs = workLogs)
                Log.d("ClockedIn", "WorkLogs: $workLogs")
            }
        }

        db.collection("workLogs")
            .whereEqualTo("uid", uid)
            .whereEqualTo("bid", bid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // No documents for today, create a new work log
                    val workLog = hashMapOf(
                        "uid" to uid,
                        "bid" to bid,
                        currentDate to listOf(entry)
                    )
                    db.collection("workLogs")
                        .add(workLog)
                        .addOnSuccessListener { documentReference ->
                            Log.d("WorkLog", "DocumentSnapshot added with ID: ${documentReference.id}")
                            //checkForExistingClockIn()
                            onComplete()
                        }
                        .addOnFailureListener { e ->
                            Log.w("WorkLog", "Error adding document", e)
                        }
                } else {
                    // Update existing document with the new entry
                    val documentSnapshot = documents.documents[0]
                    val existingData = documentSnapshot.get(currentDate) as? List<*>

                    val updatedData = existingData?.toMutableList() ?: mutableListOf()
                    updatedData.add(entry)

                    documentSnapshot.reference.update(mapOf(
                        currentDate to updatedData
                    )).addOnCompleteListener {
                        //checkForExistingClockIn()
                        onComplete()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("WorkLog", "Error getting documents.", e)
            }
    }
    private fun saveTotalHoursToDB(date:String, totalHours: Long){
        val bid = GlobalUserData.bid
        val uid =GlobalUserData.uid

        val entry = hashMapOf(
            date to totalHours,
            "approved" to false,
            "wage" to GlobalUserData.wage,

        )

        db.collection("totalHours")
            .whereEqualTo("bid", bid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // No documents for today, create a new work log
                    val hours = hashMapOf(
                        "bid" to bid,
                        uid to listOf(entry),
                    )
                    db.collection("totalHours")
                        .add(hours)
                        .addOnSuccessListener { documentReference ->
                            Log.d("hours", "DocumentSnapshot added with ID: ${documentReference.id}")
                            // checkForExistingClockIn()
                        }
                        .addOnFailureListener { e ->
                            Log.w("hours", "Error adding document", e)
                        }
                } else {
                    // Update existing document with the new entry
                    val documentSnapshot = documents.documents[0]
                    val existingData = documentSnapshot.get(uid) as? List<*>

                    val updatedData = existingData?.toMutableList() ?: mutableListOf()
                    updatedData.add(entry)


                    documentSnapshot.reference.update(mapOf(
                        uid to updatedData
                    )).addOnCompleteListener {
                        //checkForExistingClockIn()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("hours", "Error getting documents.", e)
            }
    }

    // a function to calculate the total hours worked for the day
    //(clockIn - clockOut) - (breakStart + breakEnd)
    private fun calculateTotalHoursWorked() {
        val bid = GlobalUserData.bid
        val uid = GlobalUserData.uid
        val currentDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault())

        db.collection("workLogs")
            .whereEqualTo("uid", uid)
            .whereEqualTo("bid", bid)
            .get()
            .addOnSuccessListener { documents ->
                var totalDurationInMillis = 0L
                val clockInTimes = mutableListOf<Long>()
                val clockOutTimes = mutableListOf<Long>()
                val breakStartTimes = mutableListOf<Long>()
                val breakEndTimes = mutableListOf<Long>()

                documents.forEach { document ->
                    val existingData = document.get(currentDate) as? List<*>
                    existingData?.forEach { entry ->
                        if (entry is Map<*, *>) {
                            entry["clockIn"]?.let { clockInString ->
                                clockInTimes.add(dateFormat.parse(clockInString as String)?.time ?: 0)
                            }
                            entry["clockOut"]?.let { clockOutString ->
                                clockOutTimes.add(dateFormat.parse(clockOutString as String)?.time ?: 0)
                            }
                            entry["breakStart"]?.let { breakStartString ->
                                breakStartTimes.add(dateFormat.parse(breakStartString as String)?.time ?: 0)
                            }
                            entry["breakEnd"]?.let { breakEndString ->
                                breakEndTimes.add(dateFormat.parse(breakEndString as String)?.time ?: 0)
                            }
                        }
                    }
                }

                clockInTimes.forEachIndexed { index, clockInTime ->
                    if (index < clockOutTimes.size) {
                        var workDuration = clockOutTimes[index] - clockInTime

                        // Subtract any breaks that occurred during this period
                        breakStartTimes.forEachIndexed { breakIndex, breakStartTime ->
                            if (breakIndex < breakEndTimes.size && breakStartTime >= clockInTime && breakEndTimes[breakIndex] <= clockOutTimes[index]) {
                                workDuration -= (breakEndTimes[breakIndex] - breakStartTime)
                            }
                        }

                        totalDurationInMillis += workDuration
                    }
                }

                Log.d("calculateTotalHoursWorked", "Total Duration in Millis: $totalDurationInMillis")
                saveTotalHoursToDB(currentDate, totalDurationInMillis)
            }
            .addOnFailureListener { e ->
                Log.w("WorkLog", "Error getting documents.", e)
            }
    }

    private fun saveWorkLogs() {
        val sharedPreferences = getSharedPreferences("ClockingState", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val gson = Gson()
        val json = gson.toJson(workLogs) // Convert workLogs to JSON

        editor.putString("workLogs", json)
        editor.apply() // Save the JSON string
    }

    private fun loadWorkLogs() {
        val sharedPreferences = getSharedPreferences("ClockingState", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("workLogs", null)

        val gson = Gson()
        if (json != null) {
            val type = object : TypeToken<MutableList<Map<String, Any>>>() {}.type
            workLogs = gson.fromJson(json, type) // Convert JSON string back to workLogs
        }
    }

    private fun updateDigitalClock() {
        // Get the current time
        val currentTime = System.currentTimeMillis()

        // Format the time as HH:mm:ss
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formattedTime = dateFormat.format(Date(currentTime))

        // Update the TextView with the formatted time
        digitalClock.text = formattedTime
    }

   /* private fun checkTodayScheduleAndClockIn() {
        val currentDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())
        val currentTime = Calendar.getInstance()
        Log.d("Time", "Current Time: $currentTime")

        db.collection("schedule")
            .whereEqualTo("day", currentDate)
            .whereEqualTo("BID", GlobalUserData.bid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    updateClockTextForNoSchedule()
                } else {
                    var isEmployeeScheduledToday = false
                    var scheduleStartTime: Date? = null

                    for (document in documents) {
                        val employees = document.data["employees"] as? List<*>
                        val startTimeString = document.data["startTime"] as? String
                        val format = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault())
                        scheduleStartTime = startTimeString?.let {
                            format.parse("$currentDate $it")
                        }
                        Log.d("Time", "Scheduled Time: $scheduleStartTime")

                        if (employees?.contains(GlobalUserData.name) == true && scheduleStartTime != null) {
                            // The current employee's uid is in the schedule
                            isEmployeeScheduledToday = true
                            break
                        }
                    }

                    if (isEmployeeScheduledToday) {
                        val scheduleCalendar = Calendar.getInstance()
                        scheduleCalendar.time = scheduleStartTime!!

                        // Subtract one hour from the schedule start time for the comparison
                        scheduleCalendar.add(Calendar.HOUR_OF_DAY, -1)

                        if (currentTime.after(scheduleCalendar)) {
                            // If the current time is after (or exactly one hour before) the schedule start time minus one hour, enable clock in
                            clockInButton.isEnabled = true
                        } else {
                            updateClockTextForComeBackLater()
                        }
                    } else {
                        updateClockTextForNoSchedule()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("Schedule", "Error getting documents: ", e)
                // Handle the error appropriately
            }
    }*/

    private fun updateClockTextForNoSchedule() {
        // Update the digital clock's text
        clockInButton.text = getString(R.string.not_scheduled_today)
        clockInButton.backgroundTintList = getColorStateList(R.color.light_grey)
        clockInButton.setTextColor(getColor(R.color.grey))
        // Disable clock-in button or take other appropriate actions
        clockInButton.isEnabled = false
    }

    private fun updateClockTextForComeBackLater() {
        // Update the digital clock's text
        clockInButton.text = getString(R.string.come_back_later)
        clockInButton.backgroundTintList = getColorStateList(R.color.light_grey)
        clockInButton.setTextColor(getColor(R.color.grey))
        // Disable clock-in button or take other appropriate actions
        clockInButton.isEnabled = false
    }

    override fun onResume() {
        super.onResume()
        Log.d("hi", "onResume")
        today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val clockedInToday = sharedPreferences.getBoolean("${uid}_isClockedIn", false) &&
                sharedPreferences.getString("${uid}_clockInDate", "") == today

        if (clockedInToday) {
            val composeView = findViewById<ComposeView>(R.id.compose_view)
            composeView.setContent {
                WorkLogsList(worklogs = workLogs)
                Log.d("ClockedIn", "WorkLogs: $workLogs")
            }
        }else{
            workLogs.clear()
            saveWorkLogs()
        }
        updateButtonStates()
        //checkTodayScheduleAndClockIn()
    }

    override fun onDestroy() {
        // Remove the callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}


