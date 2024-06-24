package com.example.optimate.businessOwner


import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.optimate.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ViewAvailability : AppCompatActivity() {
    private lateinit var selectedDayTextView: TextView
    private val weekdays = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private var currentDayIndex = 0
    private var businessId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_availability)

        selectedDayTextView = findViewById(R.id.selectedDay)
        updateSelectedDay()

        val previousButton = findViewById<ImageView>(R.id.iv_calendar_previous)
        val nextButton = findViewById<ImageView>(R.id.iv_calendar_next)

        previousButton.setOnClickListener {
            currentDayIndex = (currentDayIndex - 1 + weekdays.size) % weekdays.size
            updateSelectedDay()
            fetchEmployeeAvailability()
        }

        nextButton.setOnClickListener {
            currentDayIndex = (currentDayIndex + 1) % weekdays.size
            updateSelectedDay()
            fetchEmployeeAvailability()
        }

        // Fetch the business ID after the user is authorized and signed in
        fetchBusinessId()
    }

    private fun updateSelectedDay() {
        selectedDayTextView.text = weekdays[currentDayIndex]
    }

    private fun fetchBusinessId() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // User is signed in, fetch the business ID
            businessId = "d89RXe3xFjNNCEAftuslt3pGWR23ab" // Example: replace this with your logic to fetch BID
            // After fetching the business ID, you can call fetchEmployeeAvailability here if needed
            fetchEmployeeAvailability()
        } else {
            // User is not signed in, handle the case accordingly
        }
    }

   /* private fun fetchBusinessId() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            // Query the "users" collection for the document corresponding to the current user
            db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        // Retrieve the BID field from the document
                        businessId = document.getString("BID")
                        // After fetching the business ID, you can call fetchEmployeeAvailability here if needed
                        fetchEmployeeAvailability()
                    } else {
                        // Document not found, handle the case accordingly
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle errors
                }
        } else {
            // User is not signed in, handle the case accordingly
        }
    }*/

    private fun fetchEmployeeAvailability() {
        businessId?.let { bid ->
            val db = FirebaseFirestore.getInstance()
            val employeeAvailabilityLayout = findViewById<LinearLayout>(R.id.employeeAvailabilityLayout)

            // Clear existing cards
            employeeAvailabilityLayout.removeAllViews()

            // Fetch availability data for the provided BID and selected day
            db.collection("availability")
                .whereEqualTo("BID", bid)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val data = document.data
                        val name = data["name"] as String
                        val availabilityMap = data["availability"] as Map<String, List<String>>?
                        availabilityMap?.get(weekdays[currentDayIndex])?.let { availability ->
                            // Create card view
                            val cardView = layoutInflater.inflate(R.layout.employee_availability_card, null) as LinearLayout
                            val employeeNameTextView = cardView.findViewById<TextView>(R.id.employeeName1)
                            val availabilityTextView = cardView.findViewById<TextView>(R.id.availability)
                            employeeNameTextView.text = name
                            availabilityTextView.text = availability.joinToString(", ")
                            employeeAvailabilityLayout.addView(cardView)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle errors
                }
        }
    }
}

