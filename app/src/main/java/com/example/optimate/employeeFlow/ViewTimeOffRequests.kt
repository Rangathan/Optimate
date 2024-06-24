package com.example.optimate.employeeFlow

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.optimate.R
import com.example.optimate.businessOwner.XmlTopBar
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class TimeRequestStatus(val status: String, val date: String) {
    companion object {
        const val PENDING = "pending"
        const val APPROVED = "approved"
        const val REJECTED = "rejected"
        const val CANCELLED = "cancelled"
    }
}

data class TimeRequest(
    val reason: String = "",
    val bid: String = "",
    val dateOfRequest: Timestamp = Timestamp.now(),
    val startDate: String = "",
    val endDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val name: String = "",
    val status: String = "",
    val uid: String = ""
)

class ViewTimeOffRequests : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUserUid: String
    private var selectedStatus: String? = null
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_time_off_requests)

        val topBar: XmlTopBar = findViewById(R.id.topBar)
        topBar.setTitle("Time Off Requests")

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize ProgressBar
        progressBar = findViewById(R.id.progressBar)


        // Get current user's UID
        currentUserUid = auth.currentUser?.uid ?: ""

        Log.d("ViewTimeOffRequests", "Current user UID: $currentUserUid")

        // Fetch time off requests and compare UIDs

        progressBar.visibility = View.VISIBLE

        // Your data fetching logic goes here

        // Simulate data fetching with a delay
        Handler(Looper.getMainLooper()).postDelayed({
            // After fetching data, hide ProgressBar
            progressBar.visibility = View.GONE
            // Populate UI with fetched data
            fetchTimeOffRequestsAndCompareUIDs(selectedStatus)
        }, 2000)

        val textInputLayout: TextInputLayout = findViewById(R.id.requestStatusFilter)
        val autoCompleteTextView: AutoCompleteTextView = textInputLayout.editText as AutoCompleteTextView

        autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            selectedStatus = parent.getItemAtPosition(position).toString()
            // Adjust the queries based on the selected item
            when (selectedStatus) {
                "all" -> {
                    fetchTimeOffRequestsAndCompareUIDs(null) // Pass null for status to fetch all requests

                }
                else -> {
                    fetchTimeOffRequestsAndCompareUIDs(selectedStatus)

                }
            }
        }

    }

    private fun fetchTimeOffRequestsAndCompareUIDs(status: String?) {
        // Assuming timeOffRequests is a collection reference in Firestore
        var query = firestore.collection("timeOffRequest")
            .whereEqualTo("uid", currentUserUid)
            .whereNotEqualTo("status", TimeRequestStatus.CANCELLED)
        if (status != null) {
            query = query.whereEqualTo("status", status)
        }
        query.get()
            .addOnSuccessListener { documents ->
                val timeRequests = mutableListOf<TimeRequest>()
                for (document in documents) {
                    // Convert Firestore document to TimeRequest object
                    val timeRequest = document.toObject(TimeRequest::class.java)
                    timeRequests.add(timeRequest)
                }
                displayTimeOffRequests(timeRequests)
            }
            .addOnFailureListener { exception ->
                // Handle failure
                Log.e(
                    "ViewTimeOffRequests",
                    "Failed to fetch time off requests: ${exception.message}"
                )
            }
    }

    private fun cancelTimeOffRequest(timeRequest: TimeRequest) {
        // Assuming 'dateOfRequest' is the name of the field storing the timestamp in Firestore
        val dateOfRequest = timeRequest.dateOfRequest

        firestore.collection("timeOffRequest")
            .whereEqualTo("dateOfRequest", dateOfRequest)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Assuming there's only one document matching the dateOfRequest
                    val documentSnapshot = documents.documents[0]
                    val requestId = documentSnapshot.id

                    // Update the status of the found document to 'CANCELLED'
                    firestore.collection("timeOffRequest")
                        .document(requestId)
                        .update("status", TimeRequestStatus.CANCELLED)
                        .addOnSuccessListener {
                            Log.d("ViewTimeOffRequests", "Time off request cancelled successfully.")
                            // After cancelling, fetch and display the updated list of time off requests
                            fetchTimeOffRequestsAndCompareUIDs(selectedStatus)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("ViewTimeOffRequests", "Failed to cancel time off request: ${exception.message}")
                            // Handle failure
                        }
                } else {
                    // No document found matching the specified dateOfRequest
                    Log.e("ViewTimeOffRequests", "No document found matching dateOfRequest: $dateOfRequest")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ViewTimeOffRequests", "Failed to query time off requests: ${exception.message}")
                // Handle failure
            }
    }



    private fun displayTimeOffRequests(timeRequests: List<TimeRequest>) {
        Log.d("ViewTimeOffRequests", "Displaying time off requests. Count: ${timeRequests.size}")



        val timeRequestsLayout = findViewById<LinearLayout>(R.id.timeRequestsLayout)
        timeRequestsLayout.removeAllViews() // Clear previous views

        if (timeRequests.isEmpty()) {
            Log.d("ViewTimeOffRequests", "No time off requests found.")
            val placeHolderView = findViewById<View>(R.id.placeHolder)
            placeHolderView?.visibility = View.VISIBLE
        } else {
            Log.d("ViewTimeOffRequests", "Time off requests found. Count: ${timeRequests.size}")
            val placeHolderView = findViewById<View>(R.id.placeHolder)
            placeHolderView?.visibility = View.GONE

            timeRequests.forEach { timeRequest ->
                // Inflate the time_request_card layout
                val timeRequestCard = layoutInflater.inflate(R.layout.time_request_card, null)

                // Set values to the views inside the card
                timeRequestCard.findViewById<TextView>(R.id.reqStartDate).text =
                    timeRequest.startDate
                timeRequestCard.findViewById<TextView>(R.id.reqEndDate).text = timeRequest.endDate
                timeRequestCard.findViewById<TextView>(R.id.requestStatus).text =
                    "Your time request from ${timeRequest.startTime} to ${timeRequest.endTime} is ${timeRequest.status}."

                timeRequestCard.findViewById<MaterialButton>(R.id.cancelRequestButton).setOnClickListener {
                    cancelTimeOffRequest(timeRequest)
                }



                // Set card background color based on status
                val cardView = timeRequestCard.findViewById<MaterialCardView>(R.id.timeRequestCard)
                val backgroundColor = when (timeRequest.status) {
                    TimeRequestStatus.PENDING -> ContextCompat.getColor(this, R.color.light_yellow)
                    TimeRequestStatus.APPROVED -> ContextCompat.getColor(this, R.color.light_green)
                    TimeRequestStatus.REJECTED -> ContextCompat.getColor(this, R.color.light_red)
                    else -> Color.TRANSPARENT
                }
                cardView.setCardBackgroundColor(backgroundColor)

                // Set button visibility based on status

                val cancelButton = timeRequestCard.findViewById<MaterialButton>(R.id.cancelRequestButton)
                when (timeRequest.status) {

                    TimeRequestStatus.REJECTED -> {

                        cancelButton.visibility = View.GONE
                    }
                    else -> {
                        cancelButton.visibility = View.VISIBLE
                    }
                }

                // Add the inflated card to the layout
                timeRequestsLayout.addView(timeRequestCard)
            }
        }
    }

}