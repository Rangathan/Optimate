package com.example.optimate.employeeFlow


import android.content.Intent
import retrofit2.converter.gson.GsonConverterFactory
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.optimate.R
import com.example.optimate.businessOwner.XmlTopBar
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.example.optimate.loginAndRegister.FcmApi
import com.example.optimate.loginAndRegister.GlobalUserData

import com.example.optimate.loginAndRegister.SendMessageDTO
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class RequestTimeOff : AppCompatActivity() {

    private var db = Firebase.firestore
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://optimateserver.onrender.com ") // Update with your server URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_time_off)
        var startTime = ""
        var endTime = ""
        var startDatetoDb = ""
        var endDatetoDb = ""
        var reason = ""

        val allDaySwitch: MaterialSwitch = findViewById(R.id.allDaySwitch)


        val topBar: XmlTopBar = findViewById(R.id.topBar)
        topBar.setTitle("Request Time Off")

        val cancelBtn = findViewById<Button>(R.id.cancelButton)
        cancelBtn.setOnClickListener {
            val intent = Intent(this, ScheduleModule::class.java)
            startActivity(intent)
            finish()
        }






        // Create MaterialDatePicker instances for start and end dates
        val startDatePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select start date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        val endDatePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select end date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        // Find TextInputLayouts and TextInputEditTexts
        val outlinedStartDate = findViewById<TextInputLayout>(R.id.outlinedStartDate)
        val outlinedEndDate = findViewById<TextInputLayout>(R.id.outlinedEndDate)
        val startDateEditText = findViewById<TextView>(R.id.startDate)
        val endDateEditText = findViewById<TextView>(R.id.endDate)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val outlinedStartTime = findViewById<TextInputLayout>(R.id.outlinedStartTime)
        val outlinedEndTime = findViewById<TextInputLayout>(R.id.outlinedEndTime)
        val textInputLayoutReason = findViewById<TextInputLayout>(R.id.textInputLayout)


        // Set click listeners to open date pickers
        outlinedStartDate.setEndIconOnClickListener {
            startDatePicker.show(supportFragmentManager, "START_DATE_PICKER_TAG")
        }
        outlinedEndDate.setEndIconOnClickListener {
            endDatePicker.show(supportFragmentManager, "END_DATE_PICKER_TAG")
        }

        startDateEditText.setOnClickListener {
            outlinedStartDate.error = null
            startDatePicker.show(supportFragmentManager, "START_DATE_PICKER_TAG")}

        endDateEditText.setOnClickListener {
            outlinedEndDate.error = null
            endDatePicker.show(supportFragmentManager, "END_DATE_PICKER_TAG")
            }




        // Set positive button click listeners to handle date selection
        startDatePicker.addOnPositiveButtonClickListener { startTimestamp ->
            val startDate = Date(startTimestamp)
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH)
            val formattedDate = dateFormat.format(startDate)
            val endDate = endDatePicker.selection?.let { Date(it) }
            startDatetoDb = formattedDate

            if (endDate != null && startDate.after(endDate)) {
                // If an end date is selected and it's before the start date, show an error
                outlinedEndDate.error = getString(R.string.end_date_before_start_date_error)
                endDateEditText.text = null // Clear text when error occurs
            } else {
                outlinedEndDate.error = null // Clear error if no issue with the end date
            }

            startDateEditText.text = startDatePicker.headerText
            outlinedStartDate.error = null // Clear any previous errors for start date
        }



        endDatePicker.addOnPositiveButtonClickListener { endTimestamp ->
            val endDate = Date(endTimestamp)
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH)
            val formattedDate = dateFormat.format(endDate)
            val startDate = startDatePicker.selection?.let { Date(it) }

            endDatetoDb = formattedDate

            if (startDate != null && endDate.before(startDate)) {
                // If a start date is selected and the end date is before it, show an error
                outlinedEndDate.error = getString(R.string.end_date_before_start_date_error)
                endDateEditText.text = null // Clear text when error occurs
            } else {
                outlinedEndDate.error = null // Clear error if no issue with the end date
            }

            endDateEditText.text = endDatePicker.headerText
            outlinedStartDate.error = null // Clear any previous errors for start date
        }

// Create MaterialDatePicker instances for start and end dates
        val startTimePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(12)
            .setMinute(10)
            .setTitleText("Select Appointment time")
            .build()
        val endTimePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(12)
            .setMinute(10)
            .setTitleText("Select Appointment time")
            .build()

        // Find TextInputLayouts and TextInputEditTexts
        val startTimeEditText = findViewById<TextView>(R.id.startTime)
        val endTimeEditText = findViewById<TextView>(R.id.endTime)

        val startTimeTextInputLayout = findViewById<TextInputLayout>(R.id.outlinedStartTime)

        val endTimeTextInputLayout = findViewById<TextInputLayout>(R.id.outlinedEndTime)



        allDaySwitch.setOnCheckedChangeListener { _, isChecked ->
            // Enable/disable the time fields based on the checked state of the switch
            startTimeTextInputLayout.isEnabled = !isChecked
            startTimeEditText.isEnabled = !isChecked
            endTimeTextInputLayout.isEnabled = !isChecked
            endTimeEditText.isEnabled = !isChecked

            // Clear the text when switching to all day
            if (isChecked) {
                outlinedEndTime.error = null
                outlinedStartTime.error = null
                startTimeEditText.text = null
                endTimeEditText.text = null
            }
        }

        // Set OnClickListener for the Start Time EditText
        startTimeEditText.setOnClickListener{
            outlinedStartTime.error = null
            startTimePicker.show(supportFragmentManager, "START_Time_PICKER_TAG")
        }

// Set OnClickListener for the End Time EditText
        endTimeEditText.setOnClickListener{
            outlinedEndTime.error = null
            endTimePicker.show(supportFragmentManager, "END_Time_PICKER_TAG")
        }

// Add a listener to handle positive button click for Start Time
        startTimePicker.addOnPositiveButtonClickListener{
            val hour = if (startTimePicker.hour < 10) "0${startTimePicker.hour}" else startTimePicker.hour
            val minute = if (startTimePicker.minute < 10) "0${startTimePicker.minute}" else startTimePicker.minute
            val timeString = "$hour:$minute"
            startTimeEditText.text = timeString
            startTime =timeString
        }

// Add a listener to handle positive button click for End Time
        endTimePicker.addOnPositiveButtonClickListener{
            val hour = if (endTimePicker.hour < 10) "0${endTimePicker.hour}" else endTimePicker.hour
            val minute = if (endTimePicker.minute < 10) "0${endTimePicker.minute}" else endTimePicker.minute
            val timeString = "$hour:$minute"
            endTimeEditText.text = timeString
            endTime = timeString
        }


        val textInputLayout: TextInputLayout = findViewById(R.id.textInputLayout)
        val autoCompleteTextView: AutoCompleteTextView = textInputLayout.editText as AutoCompleteTextView

// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.reasons_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the AutoCompleteTextView
            autoCompleteTextView.setAdapter(adapter)
        }

        autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            reason = parent.getItemAtPosition(position).toString()
            textInputLayoutReason.error = null
            // Now the selected item is stored in the 'reason' variable
        }



        sendButton.setOnClickListener{

            // Reset error messages to null
            findViewById<TextInputLayout>(R.id.outlinedStartDate).error = null
            findViewById<TextInputLayout>(R.id.outlinedEndDate).error = null
            findViewById<TextInputLayout>(R.id.outlinedStartTime).error = null
            findViewById<TextInputLayout>(R.id.outlinedEndTime).error = null
            findViewById<TextInputLayout>(R.id.textInputLayout).error = null

            // Check if all-day switch is checked
            if (allDaySwitch.isChecked) {
                // If all-day switch is checked, set start time to "00:00" and end time to "23:59"
                startTime = "00:00"
                endTime = "23:59"
            } else {
                // If all-day switch is not checked, ensure both start and end times are selected
                if (startTime.isEmpty()) {
                    // Show error for start time field
                    findViewById<TextInputLayout>(R.id.outlinedStartTime).error = "Please select a start time"
                }
                if (endTime.isEmpty()) {
                    // Show error for end time field
                    findViewById<TextInputLayout>(R.id.outlinedEndTime).error = "Please select an end time"
                }
            }

            // Check if start date, end date, and reason are filled
            val isAllFieldsFilled = startDatetoDb.isNotEmpty() && endDatetoDb.isNotEmpty() && reason.isNotEmpty()

            // If any field is not filled, show an error message
            if (!isAllFieldsFilled) {
                if (startDatetoDb.isEmpty()) {
                    // Show error for start date field
                    findViewById<TextInputLayout>(R.id.outlinedStartDate).error = "Please select a start date"
                }
                if (endDatetoDb.isEmpty()) {
                    // Show error for end date field
                    findViewById<TextInputLayout>(R.id.outlinedEndDate).error = "Please select an end date"
                }
                if (reason.isEmpty()) {
                    // Show error for reason field
                    findViewById<TextInputLayout>(R.id.textInputLayout).error = "Please select a reason"
                }
                return@setOnClickListener
            }

            // If all fields are filled, proceed to save the request
            lifecycleScope.launch {
                saveTimeOffRequestToFirestore(startTime, endTime, startDatetoDb, endDatetoDb, reason)
            }
        }




    }

    private suspend fun saveTimeOffRequestToFirestore(startTime: String, endTime: String, startDate: String, endDate: String, reason: String) {
        val timeOffRequest = hashMapOf(
            "dateOfRequest" to Date(),
            "startTime" to startTime,
            "endTime" to endTime,
            "uid" to GlobalUserData.uid,
            "bid" to GlobalUserData.bid,
            "name" to GlobalUserData.name,
            "startDate" to startDate,
            "endDate" to endDate,
            "status" to "pending",
            "reason" to reason
        )

        try {
            val documentReference = db.collection("timeOffRequest")
                .add(timeOffRequest)
                .await()
            Log.d("EditTimeOffRequest", "New record created with ID: ${documentReference.id}")
            Toast.makeText(this, "Your request has been Sent for approval", Toast.LENGTH_SHORT).show()

            sendNotificationToManagers() // Call the suspend function within a coroutine scope
            startActivity(Intent(this, ScheduleModule::class.java))
        } catch (e: Exception) {
            Log.e("EditAccountActivity", "Error creating new record", e)
            Toast.makeText(this, "Error in Sending", Toast.LENGTH_SHORT).show()
            // Handle the error, for example, show an error message to the user
        }
    }


    private suspend fun getManagerTokens(): List<String> = suspendCoroutine { continuation ->
        val docRef = db.collection("users")
            .whereEqualTo("BID", GlobalUserData.bid)
            .whereEqualTo("role", "Manager")

        docRef.get()
            .addOnSuccessListener { querySnapshot ->
                val managerTokens = mutableListOf<String>()
                for (document in querySnapshot.documents) {
                    val Manageruid = document.getString("deviceToken")
                    if (Manageruid != null) {
                        managerTokens.add(Manageruid)
                    } else {
                        Log.e("SendNotification", "Manager's FCM token not found for document ${document.id}")
                    }
                }
                continuation.resume(managerTokens)
            }
            .addOnFailureListener { exception ->
                Log.e("SendNotification", "Error getting manager tokens", exception)
                continuation.resumeWithException(exception)
            }
    }


        suspend fun sendNotificationToManagers() {
            val managerTokens = getManagerTokens()
            val fcmApi = retrofit.create(FcmApi::class.java)

            for (managerToken in managerTokens) {
                if (managerToken != null && managerToken.isNotBlank()) {

                        val titleData = "New Time-Off Request"
                        val bodyData = "A new time-off request requires your approval."


                    try {
                        fcmApi.sendMessage(SendMessageDTO(deviceToken = managerToken, title = titleData, body = bodyData))
                        Log.e("SendNotification", "Success")
                        // Notification sent successfully
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("SendNotification", "Fail", e)
                        // Handle failure to send notification
                    }
                } else {
                    // Handle case where manager token is not found or blank
                    println("Manager's FCM token not found or blank.")
                }
            }
        }
}





