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
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.example.optimate.R
import com.example.optimate.businessOwner.XmlTopBar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.example.optimate.loginAndRegister.ModuleChoosingMain
import com.google.android.material.textfield.TextInputLayout

class ExpenseRequestStatus(val status: String, val date: String) {
    companion object {
        const val PENDING = "pending"
        const val APPROVED = "approved"
        const val REJECTED = "rejected"
        const val CANCELLED = "cancelled"
    }
}

data class ExpenseRequest(
    val reason: String = "",
    val bid: String = "",
    val dateOfRequest: Timestamp = Timestamp.now(),
    val expenseDate: String = "",
    val amount: String = "",
    val receiptPhoto: String = "",
    val name: String = "",
    val status: String = "",
    val uid: String = ""
)

class ViewExpenses : AppCompatActivity() {



    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUserUid: String
    private val cardImageStates = mutableMapOf<String, Pair<Boolean, String>>()
    private var selectedStatus: String? = null
    private lateinit var progressBar: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_expenses)

        val callback = object : OnBackPressedCallback(true /* default to enabled */) {
            override fun handleOnBackPressed() {

                val intent = Intent(this@ViewExpenses, DynamicLandingActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)


        val topBar: XmlTopBar = findViewById(R.id.topBar)
        topBar.setTitle("Expenses history")

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize ProgressBar
        progressBar = findViewById(R.id.progressBar)




        // Get current user's UID
        currentUserUid = auth.currentUser?.uid ?: ""

        Log.d("ViewRequests", "Current user UID: $currentUserUid")

        // Fetch time off requests and compare UIDs
        progressBar.visibility = View.VISIBLE

        // Your data fetching logic goes here

        // Simulate data fetching with a delay
        Handler(Looper.getMainLooper()).postDelayed({
            // After fetching data, hide ProgressBar
            progressBar.visibility = View.GONE

            // Populate UI with fetched data
            fetchExpensesRequestsAndCompareUIDs(selectedStatus)
        }, 2000)

        val textInputLayout: TextInputLayout = findViewById(R.id.requestStatusFilter)
        val autoCompleteTextView: AutoCompleteTextView = textInputLayout.editText as AutoCompleteTextView

        autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            selectedStatus = parent.getItemAtPosition(position).toString()
            // Adjust the queries based on the selected item
            when (selectedStatus) {
                "all" -> {
                    fetchExpensesRequestsAndCompareUIDs(null) // Pass null for status to fetch all requests
                     // Pass null for status to fetch all requests
                }
                else -> {
                    fetchExpensesRequestsAndCompareUIDs(selectedStatus)

                }
            }
        }

    }
    private fun fetchExpensesRequestsAndCompareUIDs(status: String?) {
        var query = firestore.collection("expenseRequest")
            .whereEqualTo("uid", currentUserUid)
            .whereNotEqualTo("status", ExpenseRequestStatus.CANCELLED)
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

    private fun cancelExpenseRequest(expenseRequest: ExpenseRequest) {
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
                        .update("status", ExpenseRequestStatus.CANCELLED)
                        .addOnSuccessListener {
                            Log.d("ViewRequests", "Request cancelled successfully.")
                            // After cancelling, fetch and display the updated list of time off requests
                            fetchExpensesRequestsAndCompareUIDs(selectedStatus)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("ViewRequests", "Failed to cancel  request: ${exception.message}")
                            // Handle failure
                        }
                } else {
                    // No document found matching the specified dateOfRequest
                    Log.e("ViewRequests", "No document found matching dateOfRequest: $dateOfRequest")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ViewRequests", "Failed to query requests: ${exception.message}")
                // Handle failure
            }
    }


    private fun displayExpenseRequests(expenseRequests: List<ExpenseRequest>) {
        Log.d("ViewTimeOffRequests", "Displaying time off requests. Count: ${expenseRequests.size}")



        val expenseRequestsLayout = findViewById<LinearLayout>(R.id.expenseRequestsLayout)
        expenseRequestsLayout.removeAllViews() // Clear previous views

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
                val expenseRequestCard = layoutInflater.inflate(R.layout.expenses_card, null)

                // Set values to the views inside the card
                expenseRequestCard.findViewById<TextView>(R.id.expenseReqDate).text =
                    expenseRequest.expenseDate
                expenseRequestCard.findViewById<TextView>(R.id.expenseRequestStatus).text =
                    "Your expense request for $${expenseRequest.amount} is ${expenseRequest.status}."

                expenseRequestCard.findViewById<MaterialButton>(R.id.cancelReqBtn).setOnClickListener {
                    cancelExpenseRequest(expenseRequest)
                }

                // Set up the onClickListener for the "View Image" button
                expenseRequestCard.findViewById<MaterialButton>(R.id.viewImageBtn).setOnClickListener {
                    viewImage(expenseRequestCard, cardIdentifier)
                }

                // Set card background color based on status
                val cardView = expenseRequestCard.findViewById<MaterialCardView>(R.id.expenseCard)
                val backgroundColor = when (expenseRequest.status) {
                    ExpenseRequestStatus.PENDING -> ContextCompat.getColor(this, R.color.light_yellow)
                    ExpenseRequestStatus.APPROVED -> ContextCompat.getColor(this, R.color.light_green)
                    ExpenseRequestStatus.REJECTED -> ContextCompat.getColor(this, R.color.light_red)
                    else -> Color.TRANSPARENT
                }
                cardView.setCardBackgroundColor(backgroundColor)

                // Set button visibility based on status
                val cancelButton = expenseRequestCard.findViewById<MaterialButton>(R.id.cancelReqBtn)
                when (expenseRequest.status) {
                    ExpenseRequestStatus.REJECTED -> {
                        cancelButton.visibility = View.GONE
                    }
                    else -> {
                        cancelButton.visibility = View.VISIBLE
                    }
                }

                // Add the inflated card to the layout
                expenseRequestsLayout.addView(expenseRequestCard)
            }
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


}