package com.example.optimate.businessOwner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.optimate.R.id
import com.example.optimate.R.layout
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.example.optimate.loginAndRegister.GlobalUserData
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.properties.Delegates

class SchedulerActivity : AppCompatActivity() {

    private var db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var dynamicContentContainer: LinearLayout

    data class Shift(
        val BID: String,
        val day: String,
        val employees: List<String>?,
        val startTime: String,
        val endTime: String
    ) {
        // Add a no-argument constructor
        constructor() : this(
            // Initialize your properties here if needed
            BID = GlobalUserData.bid,
            day = "",
            employees = null,
            startTime = "",
            endTime = ""
        )
    }

    data class TimeOffRequest(
        val bid: String? = null,
        val dateOfRequest: Timestamp? = null,
        val endDate: String? = null,
        val endTime: String? = null,
        val name: String? = null,
        val reason: String? = null,
        val startDate: String? = null,
        val startTime: String? = null,
        val status: String? = null,
        val uid: String? = null
    )

    private lateinit var noShiftView: TextView
    private lateinit var calendarView: CalendarView
    private var selectedDate by Delegates.notNull<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_scheduler)

        val topBar: XmlTopBar = findViewById(id.topBar)
        topBar.setTitle("Schedule Overview")
        auth = FirebaseAuth.getInstance()
        val callback = object : OnBackPressedCallback(true /* default to enabled */) {
            override fun handleOnBackPressed() {

                val intent = Intent(this@SchedulerActivity, DynamicLandingActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
        calendarView = findViewById(id.calendarView)
        dynamicContentContainer = findViewById(id.dynamicContentContainer)
        noShiftView = findViewById(id.noShiftText)
        selectedDate = calendarView.date

        fetchAndPopulateShiftData(getDateFormattedForDatabase(selectedDate))

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            updateSelectedDate(year, month, dayOfMonth)
        }

        val scheduleDateBtn = findViewById<Button>(id.scheduleDate)

        scheduleDateBtn.setOnClickListener {
            val intent = Intent(this, AddShiftActivity::class.java)
            intent.putExtra("SELECTED_DATE", selectedDate)
            startActivity(intent)
        }
    }

    private fun fetchAndPopulateShiftData(selectedDate: String) {
        getShiftData(selectedDate) { shifts ->
            populateUI(shifts)
            fetchAndDisplayTimeOffRequests(selectedDate)
        }
    }

    private fun updateSelectedDate(year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth)
        selectedDate = calendar.timeInMillis

        dynamicContentContainer.removeAllViews()

        val formattedDate = getDateFormattedForDatabase(selectedDate)

        fetchAndPopulateShiftData(formattedDate)
    }

    private fun getDateFormattedForDatabase(dateInMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateInMillis
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun populateUI(shifts: List<Shift>) {
        if (shifts.isNotEmpty()) {
            noShiftView.visibility = INVISIBLE
        } else {
            noShiftView.visibility = VISIBLE
        }
        if (!::dynamicContentContainer.isInitialized) {
            dynamicContentContainer = findViewById(id.dynamicContentContainer)
        }

        // Create a map to group shifts by details
        val groupedShiftsMap = mutableMapOf<String, MutableList<String>>()

        // Populate the map with shifts and associated individuals
        for (shift in shifts) {
            val shiftDetails = "${shift.startTime} - ${shift.endTime}"
            val individuals = shift.employees ?: emptyList()

            if (groupedShiftsMap.containsKey(shiftDetails)) {
                groupedShiftsMap[shiftDetails]?.addAll(individuals)
            } else {
                groupedShiftsMap[shiftDetails] = individuals.toMutableList()
            }
        }

        // Sort the keys of groupedShiftsMap by start time
        val sortedShiftDetails = groupedShiftsMap.keys.sorted()

        // Iterate through the sorted keys
        for (shiftDetails in sortedShiftDetails) {
            val individuals = groupedShiftsMap[shiftDetails]
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val contentView = inflater.inflate(layout.content_schedule_maker, null)
            dynamicContentContainer.addView(contentView)
            contentView.findViewById<TextView>(id.shiftHours).text = shiftDetails
            // Create a formatted string for the individuals working on the shift
            val names = StringBuilder()
            individuals?.forEach { name ->
                names.append(name).append(" \n")
            }
            // Remove the trailing newline if there are names
            if (names.isNotEmpty()) {
                names.setLength(names.length - 1)
            }
            contentView.findViewById<TextView>(id.NameFromDb).text = names.toString()
        }
    }

    private fun fetchAndDisplayTimeOffRequests(selectedDate: String) {
        val user = auth.currentUser
        if (user != null) {
            // Fetch all approved time off requests
            db.collection("timeOffRequest")
                .whereEqualTo("bid", GlobalUserData.bid)
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener { documents ->
                    val approvedTimeOffRequests = mutableListOf<TimeOffRequest>()
                    for (document in documents) {
                        val request = document.toObject(TimeOffRequest::class.java)
                        approvedTimeOffRequests.add(request)
                    }

                    // Check if the selected date falls within any approved time off request period
                    val matchingTimeOffRequests = approvedTimeOffRequests.filter { request ->
                        val startDateFormatted = formatDate(request.startDate!!)
                        val endDateFormatted = formatDate(request.endDate!!)
                        selectedDate in startDateFormatted..endDateFormatted
                    }

                    if (matchingTimeOffRequests.isNotEmpty()) {
                        // If approved time off request(s) exist, display them
                        populateUIWithTimeOffRequests(matchingTimeOffRequests)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FirestoreData", "Failed to fetch time off requests. Error: ${exception.message}")
                }
        }
    }

    private fun populateUIWithTimeOffRequests(timeOffRequests: List<TimeOffRequest>) {
        // Group time off requests by reason
        val groupedTimeOffRequestsMap = mutableMapOf<String, MutableList<String>>()
        for (timeOffRequest in timeOffRequests) {
            val reason = timeOffRequest.reason ?: ""
            val individuals = listOf(timeOffRequest.name ?: "")

            if (groupedTimeOffRequestsMap.containsKey(reason)) {
                groupedTimeOffRequestsMap[reason]?.addAll(individuals)
            } else {
                groupedTimeOffRequestsMap[reason] = individuals.toMutableList()
            }
        }

        // Sort the keys of groupedTimeOffRequestsMap
        val sortedTimeOffReasons = groupedTimeOffRequestsMap.keys.sorted()

        // Iterate through the sorted keys and populate the UI
        for (reason in sortedTimeOffReasons) {
            val individuals = groupedTimeOffRequestsMap[reason]

            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val contentView = inflater.inflate(layout.content_schedule_maker, null)
            dynamicContentContainer.addView(contentView)

            contentView.findViewById<TextView>(id.shiftHours).text = reason

            // Create a formatted string for the individuals taking time off
            val names = StringBuilder()
            individuals?.forEach { name ->
                names.append(name).append(" \n")
            }

            // Remove the trailing newline if there are names
            if (names.isNotEmpty()) {
                names.setLength(names.length - 1)
            }

            contentView.findViewById<TextView>(id.NameFromDb).text = names.toString()
        }
    }



    private fun getDateFormattedFromMillis(dateInMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateInMillis
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun getShiftData(selectedDate: String, callback: (List<Shift>) -> Unit) {
        db.collection("schedule")
            .whereEqualTo("BID", GlobalUserData.bid)
            .whereEqualTo("day", selectedDate)
            .get()
            .addOnSuccessListener { documents ->
                val shiftsList = mutableListOf<Shift>()
                for (document in documents) {
                    try {
                        val shift = document.toObject(Shift::class.java)
                        shiftsList.add(shift)
                    } catch (e: Exception) {
                        Log.e("SchedulerActivity", "Error parsing shift data", e)
                    }
                }

                callback(shiftsList)
            }
            .addOnFailureListener { exception ->
                Log.e("SchedulerActivity", "Error getting shift data", exception)
            }
    }

    private fun formatDate(dateString: String): String {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val date = dateFormat.parse(dateString)
        return SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date)
    }
}