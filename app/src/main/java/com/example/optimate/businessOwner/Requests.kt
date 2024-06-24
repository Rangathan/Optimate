package com.example.optimate.businessOwner


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
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.optimate.R
import com.example.optimate.employeeFlow.ExpenseRequest
import com.example.optimate.employeeFlow.ExpenseRequestStatus
import com.example.optimate.employeeFlow.TimeRequest
import com.example.optimate.employeeFlow.TimeRequestStatus
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class Requests : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var currentUserBid: String
    private val cardImageStates = mutableMapOf<String, Pair<Boolean, String>>()
    private var selectedStatus: String? = null
    private lateinit var progressBar: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_requests)

        val topBar: XmlTopBar = findViewById(R.id.topBar)
        topBar.setTitle("Requests")

        val callback = object : OnBackPressedCallback(true /* default to enabled */) {
            override fun handleOnBackPressed() {

                val intent = Intent(this@Requests, DynamicLandingActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        /*val view_archive = findViewById<Button>(R.id.view_archive_btn)
        view_archive.setOnClickListener {
            // Create an Intent to start the ViewArchive activity
            val intent = Intent(this, ViewArchive::class.java)
            startActivity(intent)
        }*/

        // Initialize ProgressBar
        progressBar = findViewById(R.id.progressBar)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Check if user is authenticated
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {

            progressBar.visibility = View.VISIBLE

            // Your data fetching logic goes here

            // Simulate data fetching with a delay
            Handler(Looper.getMainLooper()).postDelayed({
                // After fetching data, hide ProgressBar
                progressBar.visibility = View.GONE

                // Populate UI with fetched data
                fetchBid(currentUser.uid)
            }, 2000) // Simulated delay of 2 seconds
            // User is authenticated, fetch BID from Firestore

        } else {
            // User is not authenticated, handle accordingly
            // For example, redirect to login screen
            Log.d(TAG, "onCreate: User not authenticated, redirecting to login screen")
        }

        val textInputLayout: TextInputLayout = findViewById(R.id.requestStatusFilter)
        val autoCompleteTextView: AutoCompleteTextView = textInputLayout.editText as AutoCompleteTextView

        autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            selectedStatus = parent.getItemAtPosition(position).toString()
            // Adjust the queries based on the selected item
            when (selectedStatus) {
                "all" -> {
                    fetchTimeOffRequests(null) // Pass null for status to fetch all requests
                    fetchExpensesRequests(null) // Pass null for status to fetch all requests
                }
                else -> {
                    fetchTimeOffRequests(selectedStatus)
                    fetchExpensesRequests(selectedStatus)
                }
            }
        }


    }



    private fun fetchBid(uid: String) {
        Log.d(TAG, "fetchBid: Fetching BID for UID: $uid")

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").whereEqualTo("UID", uid)

        userRef.get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents.first() // Get the first document
                    Log.d(TAG, "fetchBid: Document data: ${document.data}")
                    val bid = document.getString("BID")
                    if (bid != null) {
                        currentUserBid = bid
                        Log.d(TAG, "fetchBid: BID fetched successfully: $currentUserBid")
                        // Now you can use currentUserBid in other functions
                        // Call any function that requires currentUserBid here

                        fetchTimeOffRequests(selectedStatus)
                        fetchExpensesRequests(selectedStatus)

                    } else {
                        // Handle case where BID is not found in the document
                        Log.e(TAG, "fetchBid: BID not found in document")
                    }
                } else {
                    // Handle case where no documents match the query
                    Log.e(TAG, "fetchBid: No documents found for UID: $uid")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "fetchBid: Error fetching documents", exception)
                // Handle errors
            }

            .addOnFailureListener { exception ->
                Log.e(TAG, "fetchBid: Error fetching document", exception)
                // Handle errors
            }
    }

    companion object {
        const val TAG = "Requests"
    }

    private fun fetchTimeOffRequests(status: String?) {
        var query = firestore.collection("timeOffRequest")
            .whereEqualTo("bid", currentUserBid)
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
                Log.e(TAG, "Failed to fetch time off requests: ${exception.message}")
            }
    }

    private fun fetchExpensesRequests(status: String?) {
        var query = firestore.collection("expenseRequest")
            .whereEqualTo("bid", currentUserBid)
        if (status != null) {
            query = query.whereEqualTo("status", status)
        }
        query.get()
            .addOnSuccessListener { documents ->
                val expenseRequests = mutableListOf<ExpenseRequest>()
                for (document in documents) {
                    // Convert Firestore document to ExpenseRequest object
                    val expenseRequest = document.toObject(ExpenseRequest::class.java)
                    expenseRequests.add(expenseRequest)

                    // Create a unique identifier for the card
                    val cardIdentifier = expenseRequest.dateOfRequest.toDate().time.toString()

                    // Initialize visibility state and image URL for the card
                    cardImageStates[cardIdentifier] = Pair(false, expenseRequest.receiptPhoto)
                }
                // Populate the UI with expense cards
                displayExpenseRequests(expenseRequests)
            }
            .addOnFailureListener { exception ->
                // Handle failure
                Log.e("ViewRequests", "Failed to fetch requests: ${exception.message}")
            }
    }

    private fun displayExpenseRequests(expenseRequests: List<ExpenseRequest>) {
        Log.d("ViewTimeOffRequests", "Displaying time off requests. Count: ${expenseRequests.size}")



        val expenseRequestsLayout = findViewById<LinearLayout>(R.id.RequestsLayout)


        if (expenseRequests.isEmpty()) {
            Log.d("ViewTimeOffRequests", "No time off requests found.")
            val placeHolderView = findViewById<View>(R.id.placeHolder)
            placeHolderView?.visibility = View.VISIBLE
        } else {
            Log.d("ViewTimeOffRequests", "Time off requests found. Count: ${expenseRequests.size}")
            val placeHolderView = findViewById<View>(R.id.placeHolder)
            placeHolderView?.visibility = View.GONE

            expenseRequests.forEach { expenseRequest ->
                // Create a unique identifier for the card
                val cardIdentifier = expenseRequest.dateOfRequest.toDate().time.toString()

                // Initialize visibility state and image URL for the card
                cardImageStates[cardIdentifier] = Pair(false, expenseRequest.receiptPhoto)

                // Inflate the time_request_card layout
                val expenseRequestCard = layoutInflater.inflate(R.layout.view_expenses_card, null)

                // Set values to the views inside the card
                expenseRequestCard.findViewById<TextView>(R.id.empName).text = "${expenseRequest.name} - expense request"
                expenseRequestCard.findViewById<TextView>(R.id.requestDate).text =
                    expenseRequest.expenseDate
                expenseRequestCard.findViewById<TextView>(R.id.reqStatus).text =
                    "Expense request for $${expenseRequest.amount} is ${expenseRequest.status}."


                // Set up the onClickListener for the "View Image" button
                expenseRequestCard.findViewById<MaterialButton>(R.id.viewImageBtn).apply {
                    visibility = if (expenseRequest.status == ExpenseRequestStatus.PENDING) View.VISIBLE else View.GONE
                    setOnClickListener {
                        viewImage(expenseRequestCard, cardIdentifier)
                    }
                }

                expenseRequestCard.findViewById<Button>(R.id.decline).apply {
                    visibility = if (expenseRequest.status == ExpenseRequestStatus.PENDING) View.VISIBLE else View.GONE
                    setOnClickListener {
                        declineExpenseRequest(expenseRequest)
                    }
                }

                expenseRequestCard.findViewById<Button>(R.id.approve).apply {
                    visibility = if (expenseRequest.status == ExpenseRequestStatus.PENDING) View.VISIBLE else View.GONE
                    setOnClickListener {
                        approveExpenseRequest(expenseRequest)
                    }
                }

                // Set card background color based on status
                val cardView = expenseRequestCard.findViewById<MaterialCardView>(R.id.expenseRequestCard)
                val backgroundColor = when (expenseRequest.status) {
                    ExpenseRequestStatus.PENDING -> ContextCompat.getColor(this, R.color.light_yellow)
                    ExpenseRequestStatus.APPROVED -> ContextCompat.getColor(this, R.color.light_green)
                    ExpenseRequestStatus.REJECTED -> ContextCompat.getColor(this, R.color.light_red)
                    else -> ContextCompat.getColor(this, R.color.light_grey)
                }
                cardView.setCardBackgroundColor(backgroundColor)

                // Add the inflated card to the layout
                expenseRequestsLayout.addView(expenseRequestCard)
            }
        }
    }



    private fun displayTimeOffRequests(timeRequests: List<TimeRequest>) {
        Log.d("ViewTimeOffRequests", "Displaying time off requests. Count: ${timeRequests.size}")



        val requestsLayout = findViewById<LinearLayout>(R.id.RequestsLayout)
        requestsLayout.removeAllViews() // Clear previous views

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
                val timeRequestCard = layoutInflater.inflate(R.layout.requests_card, null)

                // Set values to the views inside the card
                timeRequestCard.findViewById<TextView>(R.id.empName).text = "${timeRequest.name} - ${timeRequest.reason}"
                timeRequestCard.findViewById<TextView>(R.id.requestStartDate).text =
                    timeRequest.startDate
                timeRequestCard.findViewById<TextView>(R.id.requestEndDate).text = timeRequest.endDate
                timeRequestCard.findViewById<TextView>(R.id.reqStatus).text =
                    "Time request from ${timeRequest.startTime} to ${timeRequest.endTime} is ${timeRequest.status}."

                timeRequestCard.findViewById<Button>(R.id.decline).apply {
                    visibility = if (timeRequest.status == TimeRequestStatus.PENDING) View.VISIBLE else View.GONE
                    setOnClickListener {
                        declineTimeOffRequest(timeRequest)
                    }
                }

                timeRequestCard.findViewById<Button>(R.id.approve).apply {
                    visibility = if (timeRequest.status == TimeRequestStatus.PENDING) View.VISIBLE else View.GONE
                    setOnClickListener {
                        approveTimeOffRequest(timeRequest)
                    }
                }

                // Set card background color based on status
                val cardView = timeRequestCard.findViewById<MaterialCardView>(R.id.requestCard)
                val backgroundColor = when (timeRequest.status) {
                    TimeRequestStatus.PENDING -> ContextCompat.getColor(this, R.color.light_yellow)
                    TimeRequestStatus.APPROVED -> ContextCompat.getColor(this, R.color.light_green)
                    TimeRequestStatus.REJECTED -> ContextCompat.getColor(this, R.color.light_red)
                    else -> ContextCompat.getColor(this, R.color.light_grey)
                }
                cardView.setCardBackgroundColor(backgroundColor)


                // Add the inflated card to the layout
                requestsLayout.addView(timeRequestCard)
            }
        }
    }

    private fun declineExpenseRequest(expenseRequest: ExpenseRequest) {
        // Assuming 'dateOfRequest' is the name of the field storing the timestamp in Firestore
        val dateOfRequest = expenseRequest.dateOfRequest

        firestore.collection("expenseRequest")
            .whereEqualTo("dateOfRequest", dateOfRequest)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Assuming there's only one document matching the dateOfRequest
                    val documentSnapshot = documents.documents[0]
                    val requestId = documentSnapshot.id

                    // Update the status of the found document to 'CANCELLED'
                    firestore.collection("expenseRequest")
                        .document(requestId)
                        .update("status", ExpenseRequestStatus.REJECTED)
                        .addOnSuccessListener {
                            Log.d("ViewRequests", " request cancelled successfully.")
                            // After cancelling, fetch and display the updated list of time off requests
                            fetchTimeOffRequests(selectedStatus)
                            fetchExpensesRequests(selectedStatus)
                        }
                        .addOnFailureListener { exception ->
                            Log.e(
                                "ViewRequests",
                                "Failed to cancel request: ${exception.message}"
                            )
                            // Handle failure
                        }
                } else {
                    // No document found matching the specified dateOfRequest
                    Log.e(
                        "ViewRequests",
                        "No document found matching dateOfRequest: $dateOfRequest"
                    )
                }
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "ViewRequests",
                    "Failed to query requests: ${exception.message}"
                )
                // Handle failure
            }
    }
    private fun approveExpenseRequest(expenseRequest: ExpenseRequest) {
        // Assuming 'dateOfRequest' is the name of the field storing the timestamp in Firestore
        val dateOfRequest = expenseRequest.dateOfRequest

        firestore.collection("expenseRequest")
            .whereEqualTo("dateOfRequest", dateOfRequest)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Assuming there's only one document matching the dateOfRequest
                    val documentSnapshot = documents.documents[0]
                    val requestId = documentSnapshot.id

                    // Update the status of the found document to 'CANCELLED'
                    firestore.collection("expenseRequest")
                        .document(requestId)
                        .update("status", ExpenseRequestStatus.APPROVED)
                        .addOnSuccessListener {
                            Log.d("ViewRequests", " request approved successfully.")
                            // After cancelling, fetch and display the updated list of time off requests
                            fetchTimeOffRequests(selectedStatus)
                            fetchExpensesRequests(selectedStatus)
                        }
                        .addOnFailureListener { exception ->
                            Log.e(
                                "ViewRequests",
                                "Failed to approve request: ${exception.message}"
                            )
                            // Handle failure
                        }
                } else {
                    // No document found matching the specified dateOfRequest
                    Log.e(
                        "ViewRequests",
                        "No document found matching dateOfRequest: $dateOfRequest"
                    )
                }
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "ViewRequests",
                    "Failed to query  requests: ${exception.message}"
                )
                // Handle failure
            }
    }

    private fun viewImage(expenseRequestCard: View, cardIdentifier: String) {
        val receiptImage = expenseRequestCard.findViewById<ImageView>(R.id.receiptImage)

        // Retrieve visibility state and image URL for the card
        val (isImageVisible, imageUrl) = cardImageStates[cardIdentifier] ?: return

        // Toggle visibility for the card
        if (isImageVisible) {
            receiptImage.visibility = View.GONE
            expenseRequestCard.findViewById<MaterialButton>(R.id.viewImageBtn).text = "View Image" // Change button text
        } else {
            receiptImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(imageUrl)
                .into(receiptImage)
            expenseRequestCard.findViewById<MaterialButton>(R.id.viewImageBtn).text = "Hide Image" // Change button text
        }

        // Update visibility state for the card
        cardImageStates[cardIdentifier] = Pair(!isImageVisible, imageUrl)
    }


    private fun declineTimeOffRequest(timeRequest: TimeRequest) {
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
                        .update("status", TimeRequestStatus.REJECTED)
                        .addOnSuccessListener {
                            Log.d("ViewTimeOffRequests", "Time off request cancelled successfully.")
                            // After cancelling, fetch and display the updated list of time off requests
                            fetchTimeOffRequests(selectedStatus)
                            fetchExpensesRequests(selectedStatus)
                        }
                        .addOnFailureListener { exception ->
                            Log.e(
                                "ViewTimeOffRequests",
                                "Failed to cancel time off request: ${exception.message}"
                            )
                            // Handle failure
                        }
                } else {
                    // No document found matching the specified dateOfRequest
                    Log.e(
                        "ViewTimeOffRequests",
                        "No document found matching dateOfRequest: $dateOfRequest"
                    )
                }
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "ViewTimeOffRequests",
                    "Failed to query time off requests: ${exception.message}"
                )
                // Handle failure
            }
    }

    private fun approveTimeOffRequest(timeRequest: TimeRequest) {
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
                        .update("status", TimeRequestStatus.APPROVED)
                        .addOnSuccessListener {
                            Log.d("ViewTimeOffRequests", "Time off request cancelled successfully.")
                            // After cancelling, fetch and display the updated list of time off requests
                            fetchTimeOffRequests(selectedStatus)
                            fetchExpensesRequests(selectedStatus)
                        }
                        .addOnFailureListener { exception ->
                            Log.e(
                                "ViewTimeOffRequests",
                                "Failed to cancel time off request: ${exception.message}"
                            )
                            // Handle failure
                        }
                } else {
                    // No document found matching the specified dateOfRequest
                    Log.e(
                        "ViewTimeOffRequests",
                        "No document found matching dateOfRequest: $dateOfRequest"
                    )
                }
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "ViewTimeOffRequests",
                    "Failed to query time off requests: ${exception.message}"
                )
                // Handle failure
            }
    }
}
