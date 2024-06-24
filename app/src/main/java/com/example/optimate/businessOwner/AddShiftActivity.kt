package com.example.optimate.businessOwner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import com.example.optimate.R
import com.example.optimate.loginAndRegister.GlobalUserData
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.properties.Delegates

class AddShiftActivity : AppCompatActivity() {

    private var employeeNames = mutableStateListOf<String>()

    private var db = Firebase.firestore
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var dateTextView: TextView
    private lateinit var startTimeEditText: EditText
    private lateinit var endTimeEditText: EditText
    private var selectedDate by Delegates.notNull<Long>()

    data class Shift(
        val BID: String,  // Add this property
        val day: String,
        val employees: List<String>,
        val startTime: String,
        val endTime: String
    )

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

    private val selectedEmployees = mutableListOf<String>()
    private var approvedTimeOffRequests = mutableListOf<TimeOffRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_shift)
        val saveShiftBtn = findViewById<Button>(R.id.saveShiftButton)

        val topBar: XmlTopBar = findViewById(R.id.topBar)
        topBar.setTitle("Add Shift")

        dateTextView = findViewById(R.id.dateTextView)
        selectedDate = intent.getLongExtra("SELECTED_DATE", 0)

        // Convert the selected date to a formatted string
        val formattedDate = getDateFormattedFromMillis(selectedDate)
        // Set the text of editTextDate to the selected date
        dateTextView.text = formattedDate

        startTimeEditText = findViewById(R.id.startTime)
        endTimeEditText = findViewById(R.id.endTime)
        val employeeListView: ListView = findViewById(R.id.employeeListView)

        dateTextView.setOnClickListener {
            showDatePicker()
        }

        startTimeEditText.setOnClickListener {
            showTimePicker(startTimeEditText)
        }

        endTimeEditText.setOnClickListener {
            showTimePicker(endTimeEditText)
        }

        adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_multiple_choice,
            android.R.id.text1,
            employeeNames
        )

        employeeListView.adapter = adapter
        employeeListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        // Set up listener for item selection/deselection
        employeeListView.setOnItemClickListener { _, _, position, _ ->
            val employeeName = employeeNames[position].split("\n")[0]

            // Check if the employee has an approved time off request
            val timeOffRequest = approvedTimeOffRequests.find { it.name == employeeName }
            if (timeOffRequest != null) {
                // If the employee has an approved time off, show a toast indicating their unavailability
                Toast.makeText(this, "This employee has an approved time off", Toast.LENGTH_SHORT).show()
                // Optionally, you can deselect the employee programmatically
                employeeListView.setItemChecked(position, false)
            } else {
                // If the employee doesn't have an approved time off, proceed with selection/deselection
                if (selectedEmployees.contains(employeeName)) {
                    selectedEmployees.remove(employeeName)
                } else {
                    selectedEmployees.add(employeeName)
                }
                // Update the UI based on selection/deselection
                adapter.notifyDataSetChanged()
            }
        }


        saveShiftBtn.setOnClickListener {
            saveShiftToFirebase(formattedDate, startTimeEditText, endTimeEditText)
        }

        // Now, call getEmployeesFromDB() after adapter initialization
        getEmployeesFromDB(selectedDate)
    }

    private fun saveShiftToFirebase(
        selectedDate: String,
        startTimeEditText: EditText,
        endTimeEditText: EditText
    ) {
        val startTime = startTimeEditText.text.toString()
        val endTime = endTimeEditText.text.toString()

        val shift = SchedulerActivity.Shift(
            BID = GlobalUserData.bid,
            day = selectedDate,
            employees = selectedEmployees,
            startTime = startTime,
            endTime = endTime
        )

        val shiftMap = hashMapOf(
            "BID" to GlobalUserData.bid,
            "day" to shift.day,
            "employees" to shift.employees,
            "startTime" to shift.startTime,
            "endTime" to shift.endTime
        )

        db.collection("schedule")
            .add(shiftMap)
            .addOnSuccessListener { documentReference ->
                Log.d("AddShiftActivity", "Shift added with ID: ${documentReference.id}")
                val intent = Intent(this, SchedulerActivity::class.java)
                intent.putExtra("SELECTED_DATE", shift.day)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("AddShiftActivity", "Error adding shift", e)
            }
    }

    private fun getEmployeesFromDB(selectedDate: Long) {
        db.collection("users")
            .whereEqualTo("BID", GlobalUserData.bid)
            .whereIn("account_status.status", listOf("Created", "Active"))
            .get()
            .addOnSuccessListener { documents ->
                val employeeDataList = mutableListOf<Pair<String, List<String>>>()

                for (document in documents) {
                    val name = document.getString("name") ?: "N/A"
                    val uid = document.getString("UID") ?: ""

                    // Fetch availability for the selected date
                    fetchAvailability(uid, selectedDate) { availability ->
                        val employeeData = Pair(name, availability)
                        employeeDataList.add(employeeData)

                        // Notify the adapter when all data is fetched
                        if (employeeDataList.size == documents.size()) {
                            // Fetch all approved time off requests for the selected date
                            fetchApprovedTimeOffRequests(selectedDate) { approvedTimeOffRequests ->
                                updateEmployeeListView(employeeDataList, approvedTimeOffRequests)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ScheduleMakerActivity", "Error getting employee data", exception)
                Toast.makeText(this, "Failed to retrieve employees", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchApprovedTimeOffRequests(selectedDate: Long, callback: (List<TimeOffRequest>) -> Unit) {
            // Fetch all approved time off requests
            db.collection("timeOffRequest")
                .whereEqualTo("bid", GlobalUserData.bid)
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener { documents ->
                    approvedTimeOffRequests = mutableListOf<TimeOffRequest>()
                    for (document in documents) {
                        val request = document.toObject(TimeOffRequest::class.java)
                        approvedTimeOffRequests.add(request)
                    }

                    // Check if the selected date falls within any approved time off request period
                    val matchingTimeOffRequests = approvedTimeOffRequests.filter { request ->
                        val startDateFormatted = formatDate(request.startDate!!)
                        val endDateFormatted = formatDate(request.endDate!!)
                        val selectedDateFormatted = getDateFormattedFromMillis(selectedDate)
                        selectedDateFormatted in startDateFormatted..endDateFormatted
                    }

                    callback(matchingTimeOffRequests)
                }
                .addOnFailureListener { exception ->
                    Log.e("FirestoreData", "Failed to fetch time off requests. Error: ${exception.message}")
                    callback(emptyList())
                }
    }



    private fun fetchAvailability(uid: String, selectedDate: Long, callback: (List<String>) -> Unit) {
        // Fetch availability from the "availability" collection for the specified UID and selected date
        db.collection("availability")
            .whereEqualTo("BID", GlobalUserData.bid)
            .whereEqualTo("UID", uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Use the availability from the first document (assuming there's only one)
                    val documentSnapshot = querySnapshot.documents[0]
                    val availabilityMap = documentSnapshot.get("availability") as? Map<String, List<String>>
                    val dayOfWeek = getDayOfWeekFromDate(selectedDate)
                    // Get the availability for the selected day of the week
                    val availability = availabilityMap?.get(dayOfWeek) ?: emptyList()

                    callback(availability)
                } else {
                    Log.e("ScheduleMakerActivity", "No availability documents found for UID: $uid")
                    callback(emptyList())
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ScheduleMakerActivity", "Error getting availability data", exception)
                callback(emptyList())
            }
    }

    private fun updateEmployeeListView(employeeDataList: List<Pair<String, List<String>>>, approvedTimeOffRequests: List<TimeOffRequest>) {
        // Clear existing data
        employeeNames.clear()

        // Add employee names with availability to the list
        for ((name, availability) in employeeDataList) {
            // Check if the employee has an approved time off request for the selected date
            val timeOffRequest = approvedTimeOffRequests.find { it.name == name }
            val formattedAvailability = if (timeOffRequest != null) {
                // If there's an approved time off request, set availability as "Unavailable: reason"
                "${timeOffRequest.reason}"
            } else {
                // Otherwise, format and display their availability normally
                if (availability.isNotEmpty()) {
                    availability.joinToString(", ") // Join multiple availability entries
                } else {
                    "N/A"
                }
            }

            val employeeInfo = "$name\nAvailability: $formattedAvailability"
            employeeNames.add(employeeInfo)
        }

        employeeNames.sort()
        // Notify the adapter of the changes
        adapter.notifyDataSetChanged()
    }



    private fun getDayOfWeekFromDate(dateInMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateInMillis
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Convert day of week to a string (e.g., "Monday")
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
    }

    private fun formatDate(dateString: String): String {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val date = dateFormat.parse(dateString)
        return SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date)
    }

    private fun showDatePicker() {
        // Create a MaterialDatePicker instance for date picking
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select a date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        // Set positive button click listener to handle date selection
        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            // Convert the selected date to a formatted string
            val formattedDate = getDateFormattedFromMillis(selectedDate)

            // Set the selected date to the date TextView
            dateTextView.text = formattedDate
            getEmployeesFromDB(selectedDate)
        }

        // Show the date picker
        datePicker.show(supportFragmentManager, "DATE_PICKER_TAG")
    }

    private fun showTimePicker(editText: EditText) {
        val isStartTime = editText.id == R.id.startTime

        // Create a MaterialTimePicker instance for time picking with 12-hour format
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setTitleText("Select ${if (isStartTime) "Start" else "End"} Time")
            .build()

        // Set positive button click listener to handle time selection
        timePicker.addOnPositiveButtonClickListener {
            val amPm = if (timePicker.hour < 12) "AM" else "PM"
            val hour12 = if (timePicker.hour % 12 == 0) 12 else timePicker.hour % 12
            val hour = if (hour12 < 10) "0$hour12" else hour12
            val minute = if (timePicker.minute < 10) "0${timePicker.minute}" else timePicker.minute
            val timeString = "$hour:$minute $amPm"

            // Set the selected time to the corresponding EditText
            editText.setText(timeString)
        }

        // Show the time picker
        timePicker.show(supportFragmentManager, "TIME_PICKER_TAG")
    }

    private fun getDateFormattedFromMillis(dateInMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateInMillis
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(calendar.time)
    }
}
