package com.example.optimate.employeeFlow

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.optimate.R
import com.example.optimate.businessOwner.XmlTopBar
import com.example.optimate.loginAndRegister.GlobalUserData
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

enum class AvailabilityStatus {
    Morning,
    Evening,
    `All-Day`,
    Unavailable
}


class Availability : AppCompatActivity() {

    private var db = Firebase.firestore

    private var isEditing = false

    private val availabilityMap = mutableMapOf(
        "Monday" to mutableListOf<AvailabilityStatus>(AvailabilityStatus.Unavailable),
        "Tuesday" to mutableListOf<AvailabilityStatus>(AvailabilityStatus.Unavailable),
        "Wednesday" to mutableListOf<AvailabilityStatus>(AvailabilityStatus.Unavailable),
        "Thursday" to mutableListOf<AvailabilityStatus>(AvailabilityStatus.Unavailable),
        "Friday" to mutableListOf<AvailabilityStatus>(AvailabilityStatus.Unavailable),
        "Saturday" to mutableListOf<AvailabilityStatus>(AvailabilityStatus.Unavailable),
        "Sunday" to mutableListOf<AvailabilityStatus>(AvailabilityStatus.Unavailable)
    )

    private lateinit var toggleMondays: MaterialButtonToggleGroup
    private lateinit var toggleTuesdays: MaterialButtonToggleGroup
    private lateinit var toggleWednesdays: MaterialButtonToggleGroup
    private lateinit var toggleThursdays: MaterialButtonToggleGroup
    private lateinit var toggleFridays: MaterialButtonToggleGroup
    private lateinit var toggleSaturdays: MaterialButtonToggleGroup
    private lateinit var toggleSundays: MaterialButtonToggleGroup

    private lateinit var disableEnableMondays: MaterialSwitch
    private lateinit var disableEnableTuesdays: MaterialSwitch
    private lateinit var disableEnableWednesdays: MaterialSwitch
    private lateinit var disableEnableThursdays: MaterialSwitch
    private lateinit var disableEnableFridays: MaterialSwitch
    private lateinit var disableEnableSaturdays: MaterialSwitch
    private lateinit var disableEnableSundays: MaterialSwitch


    // Buttons for Monday
    private lateinit var mondaysM: MaterialButton
    private lateinit var mondaysE: MaterialButton
    private lateinit var mondaysA: MaterialButton

    // Buttons for Tuesday
    private lateinit var tuesdaysM: MaterialButton
    private lateinit var tuesdaysE: MaterialButton
    private lateinit var tuesdaysA: MaterialButton

    // Buttons for Wednesday
    private lateinit var wednesdaysM: MaterialButton
    private lateinit var wednesdaysE: MaterialButton
    private lateinit var wednesdaysA: MaterialButton

    // Buttons for Thursday
    private lateinit var thursdaysM: MaterialButton
    private lateinit var thursdaysE: MaterialButton
    private lateinit var thursdaysA: MaterialButton

    // Buttons for Friday
    private lateinit var fridaysM: MaterialButton
    private lateinit var fridaysE: MaterialButton
    private lateinit var fridaysA: MaterialButton

    // Buttons for Saturday
    private lateinit var saturdaysM: MaterialButton
    private lateinit var saturdaysE: MaterialButton
    private lateinit var saturdaysA: MaterialButton

    // Buttons for Sunday
    private lateinit var sundaysM: MaterialButton
    private lateinit var sundaysE: MaterialButton
    private lateinit var sundaysA: MaterialButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_availability)

        val editButton = findViewById<MaterialButton>(R.id.editButton)
        val saveIcon = ContextCompat.getDrawable(this, R.drawable.ic_save)
        val editIcon = ContextCompat.getDrawable(this, R.drawable.ic_settings)

        val topBar: XmlTopBar = findViewById(R.id.topBar)
        topBar.setTitle("Availability")

        toggleMondays = findViewById(R.id.toggleMondays)
        toggleTuesdays = findViewById(R.id.toggleTuesdays)
        toggleWednesdays = findViewById(R.id.toggleWednesdays)
        toggleThursdays = findViewById(R.id.toggleThursdays)
        toggleFridays = findViewById(R.id.toggleFridays)
        toggleSaturdays = findViewById(R.id.toggleSaturdays)
        toggleSundays = findViewById(R.id.toggleSundays)

        disableEnableMondays = findViewById(R.id.disableEnableMondays)
        disableEnableTuesdays = findViewById(R.id.disableEnableTuesdays)
        disableEnableWednesdays = findViewById(R.id.disableEnableWednesdays)
        disableEnableThursdays = findViewById(R.id.disableEnableThursdays)
        disableEnableFridays = findViewById(R.id.disableEnableFridays)
        disableEnableSaturdays = findViewById(R.id.disableEnableSaturdays)
        disableEnableSundays = findViewById(R.id.disableEnableSundays)


        // Find and initialize buttons for each day
        mondaysM = findViewById(R.id.mondaysM)
        mondaysE = findViewById(R.id.mondaysE)
        mondaysA = findViewById(R.id.mondaysA)

        tuesdaysM = findViewById(R.id.tuesdaysM)
        tuesdaysE = findViewById(R.id.tuesdaysE)
        tuesdaysA = findViewById(R.id.tuesdaysA)

        wednesdaysM = findViewById(R.id.wednesdaysM)
        wednesdaysE = findViewById(R.id.wednesdaysE)
        wednesdaysA = findViewById(R.id.wednesdaysA)

        thursdaysM = findViewById(R.id.thursdaysM)
        thursdaysE = findViewById(R.id.thursdaysE)
        thursdaysA = findViewById(R.id.thursdaysA)

        fridaysM = findViewById(R.id.fridaysM)
        fridaysE = findViewById(R.id.fridaysE)
        fridaysA = findViewById(R.id.fridaysA)

        saturdaysM = findViewById(R.id.saturdaysM)
        saturdaysE = findViewById(R.id.saturdaysE)
        saturdaysA = findViewById(R.id.saturdaysA)

        sundaysM = findViewById(R.id.sundaysM)
        sundaysE = findViewById(R.id.sundaysE)
        sundaysA = findViewById(R.id.sundaysA)


        // Set initial state
        setEditingState(editButton, editIcon)



        editButton.setOnClickListener {
            isEditing = !isEditing
            if (isEditing) {
                // Change to "Save" and switch to save icon
                editButton.text = "Save"
                editButton.icon = saveIcon
                enableButtons(true)

            } else {
                // Change to "Edit" and switch to edit icon
                editButton.text = "Edit"
                editButton.icon = editIcon
                enableButtons(false)
                updateUser(availabilityMap)

            }
        }

        // Monday
        disableEnableMondays.setOnCheckedChangeListener { buttonView, isChecked ->
            updateToggleText(isChecked, disableEnableMondays)
            toggleState(isChecked, toggleMondays)
            availabilityMap["Monday"]?.apply {
                clear()
                add(AvailabilityStatus.Unavailable)
            }
        }

// Tuesday
        disableEnableTuesdays.setOnCheckedChangeListener { buttonView, isChecked ->
            updateToggleText(isChecked, disableEnableTuesdays)
            toggleState(isChecked, toggleTuesdays)
            availabilityMap["Tuesday"]?.apply {
                clear()
                add(AvailabilityStatus.Unavailable)
            }
        }


        /*// Set OnCheckedChangeListener for each toggle to update text dynamically
                disableEnableMondays.setOnCheckedChangeListener { _, isChecked ->
                    updateToggleText(isChecked, disableEnableMondays)
                }
                disableEnableTuesdays.setOnCheckedChangeListener { _, isChecked ->
                    updateToggleText(isChecked, disableEnableTuesdays)
                }
                disableEnableWednesdays.setOnCheckedChangeListener { _, isChecked ->
                    updateToggleText(isChecked, disableEnableWednesdays)
                }
                disableEnableThursdays.setOnCheckedChangeListener { _, isChecked ->
                    updateToggleText(isChecked, disableEnableThursdays)
                }
                disableEnableFridays.setOnCheckedChangeListener { _, isChecked ->
                    updateToggleText(isChecked, disableEnableFridays)
                }
                disableEnableSaturdays.setOnCheckedChangeListener { _, isChecked ->
                    updateToggleText(isChecked, disableEnableSaturdays)
                }
                disableEnableSundays.setOnCheckedChangeListener { _, isChecked ->
                    updateToggleText(isChecked, disableEnableSundays)
                }*/

// Wednesday
        disableEnableWednesdays.setOnCheckedChangeListener { buttonView, isChecked ->
            updateToggleText(isChecked, disableEnableWednesdays)
            toggleState(isChecked, toggleWednesdays)
            availabilityMap["Wednesday"]?.apply {
                clear()
                add(AvailabilityStatus.Unavailable)
            }
        }

// Thursday
        disableEnableThursdays.setOnCheckedChangeListener { buttonView, isChecked ->
            updateToggleText(isChecked, disableEnableThursdays)
            toggleState(isChecked, toggleThursdays)
            availabilityMap["Thursday"]?.apply {
                clear()
                add(AvailabilityStatus.Unavailable)
            }
        }

// Friday
        disableEnableFridays.setOnCheckedChangeListener { buttonView, isChecked ->
            updateToggleText(isChecked, disableEnableFridays)
            toggleState(isChecked, toggleFridays)
            availabilityMap["Friday"]?.apply {
                clear()
                add(AvailabilityStatus.Unavailable)
            }
        }

// Saturday
        disableEnableSaturdays.setOnCheckedChangeListener { buttonView, isChecked ->
            updateToggleText(isChecked, disableEnableSaturdays)
            toggleState(isChecked, toggleSaturdays)
            availabilityMap["Saturday"]?.apply {
                clear()
                add(AvailabilityStatus.Unavailable)
            }
        }

// Sunday
        disableEnableSundays.setOnCheckedChangeListener { buttonView, isChecked ->
            updateToggleText(isChecked, disableEnableSundays)
            toggleState(isChecked, toggleSundays)
            availabilityMap["Sunday"]?.apply {
                clear()
                add(AvailabilityStatus.Unavailable)
            }
        }


        // Monday
        mondaysM.setOnClickListener {
            if (isEditing) {
                availabilityMap["Monday"]?.apply {
                    clear()
                    add(AvailabilityStatus.Morning)
                }
            }
        }

        mondaysE.setOnClickListener {
            if (isEditing) {
                availabilityMap["Monday"]?.apply {
                    clear()
                    add(AvailabilityStatus.Evening)
                }
            }
        }

        mondaysA.setOnClickListener {
            if (isEditing) {
                availabilityMap["Monday"]?.apply {
                    clear()
                    add(AvailabilityStatus.`All-Day`)
                }
            }
        }

// Tuesday
        tuesdaysM.setOnClickListener {
            if (isEditing) {
                availabilityMap["Tuesday"]?.apply {
                    clear()
                    add(AvailabilityStatus.Morning)
                }
            }
        }

        tuesdaysE.setOnClickListener {
            if (isEditing) {
                availabilityMap["Tuesday"]?.apply {
                    clear()
                    add(AvailabilityStatus.Evening)
                }
            }
        }

        tuesdaysA.setOnClickListener {
            if (isEditing) {
                availabilityMap["Tuesday"]?.apply {
                    clear()
                    add(AvailabilityStatus.`All-Day`)
                }
            }
        }

        // Wednesday
        wednesdaysM.setOnClickListener {
            if (isEditing) {
                availabilityMap["Wednesday"]?.apply {
                    clear()
                    add(AvailabilityStatus.Morning)
                }
            }
        }

        wednesdaysE.setOnClickListener {
            if (isEditing) {
                availabilityMap["Wednesday"]?.apply {
                    clear()
                    add(AvailabilityStatus.Evening)
                }
            }
        }

        wednesdaysA.setOnClickListener {
            if (isEditing) {
                availabilityMap["Wednesday"]?.apply {
                    clear()
                    add(AvailabilityStatus.`All-Day`)
                }
            }
        }

//Thursday
        thursdaysM.setOnClickListener {
            if (isEditing) {
                availabilityMap["Thursday"]?.apply {
                    clear()
                    add(AvailabilityStatus.Morning)
                }
            }
        }

        thursdaysE.setOnClickListener {
            if (isEditing) {
                availabilityMap["Thursday"]?.apply {
                    clear()
                    add(AvailabilityStatus.Evening)
                }
            }
        }

        thursdaysA.setOnClickListener {
            if (isEditing) {
                availabilityMap["Thursday"]?.apply {
                    clear()
                    add(AvailabilityStatus.`All-Day`)
                }
            }
        }
        // Friday
        fridaysM.setOnClickListener {
            if (isEditing) {
                availabilityMap["Friday"]?.apply {
                    clear()
                    add(AvailabilityStatus.Morning)
                }
            }
        }

        fridaysE.setOnClickListener {
            if (isEditing) {
                availabilityMap["Friday"]?.apply {
                    clear()
                    add(AvailabilityStatus.Evening)
                }
            }
        }

        fridaysA.setOnClickListener {
            if (isEditing) {
                availabilityMap["Friday"]?.apply {
                    clear()
                    add(AvailabilityStatus.`All-Day`)
                }
            }
        }

// Saturday
        saturdaysM.setOnClickListener {
            if (isEditing) {
                availabilityMap["Saturday"]?.apply {
                    clear()
                    add(AvailabilityStatus.Morning)
                }
            }
        }

        saturdaysE.setOnClickListener {
            if (isEditing) {
                availabilityMap["Saturday"]?.apply {
                    clear()
                    add(AvailabilityStatus.Evening)
                }
            }
        }

        saturdaysA.setOnClickListener {
            if (isEditing) {
                availabilityMap["Saturday"]?.apply {
                    clear()
                    add(AvailabilityStatus.`All-Day`)
                }
            }
        }
        // Sunday
        sundaysM.setOnClickListener {
            if (isEditing) {
                availabilityMap["Sunday"]?.apply {
                    clear()
                    add(AvailabilityStatus.Morning)
                }
            }
        }

        sundaysE.setOnClickListener {
            if (isEditing) {
                availabilityMap["Sunday"]?.apply {
                    clear()
                    add(AvailabilityStatus.Evening)
                }
            }
        }

        sundaysA.setOnClickListener {
            if (isEditing) {
                availabilityMap["Sunday"]?.apply {
                    clear()
                    add(AvailabilityStatus.`All-Day`)
                }
            }
        }

// Similarly, add click listeners for buttons representing other days and time slots...

        fetchAvailabilityData()


    }

    // Function to toggle the enabled state of a toggle and MaterialButtons based on the checked state of a checkbox
    private fun toggleState(isChecked: Boolean, toggle: MaterialButtonToggleGroup) {
        if (!isChecked) {
            toggle.clearChecked()
        }
        toggle.isEnabled = isChecked

    }

    private fun updateToggleText(isChecked: Boolean, toggle: MaterialSwitch) {
        toggle.text = if (isChecked) "Available" else "Not Available"
    }


    private fun setEditingState(editButton: MaterialButton, icon: Drawable?) {
        editButton.text = if (isEditing) "Save" else "Edit"
        editButton.icon = icon
        enableButtons(isEditing)
    }

    private fun enableButtons(enable: Boolean) {

        val toggleList = listOf(
            toggleMondays,
            toggleTuesdays,
            toggleWednesdays,
            toggleThursdays,
            toggleFridays,
            toggleSaturdays,
            toggleSundays
        )

        val switchList = listOf(
            disableEnableMondays,
            disableEnableTuesdays,
            disableEnableWednesdays,
            disableEnableThursdays,
            disableEnableFridays,
            disableEnableSaturdays,
            disableEnableSundays
        )

        for (i in switchList.indices) {
            toggleList[i].isEnabled = enable && switchList[i].isChecked
            switchList[i].isEnabled = enable
        }
    }

    private fun fetchAvailabilityData() {
        db.collection("availability")
            .whereEqualTo("UID", GlobalUserData.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Handle case where no availability data is found
                    return@addOnSuccessListener
                }

                // At least one document found
                val document = documents.first()
                val availabilityData = document["availability"] as? Map<String, List<String>>

                availabilityData?.forEach { (day, statusList) ->
                    // Clear existing status list for the day
                    availabilityMap[day]?.clear()

                    // Update availability status for the day
                    statusList.forEach { status ->
                        availabilityMap[day]?.add(AvailabilityStatus.valueOf(status))
                    }

                    // Update UI for the day based on availability status
                    updateUIToggleButtons(day, availabilityMap[day])

                    // Update toggle text based on checked state
                    when (day) {
                        "Monday" -> updateToggleText(
                            disableEnableMondays.isChecked,
                            disableEnableMondays
                        )

                        "Tuesday" -> updateToggleText(
                            disableEnableTuesdays.isChecked,
                            disableEnableTuesdays
                        )

                        "Wednesday" -> updateToggleText(
                            disableEnableWednesdays.isChecked,
                            disableEnableWednesdays
                        )

                        "Thursday" -> updateToggleText(
                            disableEnableThursdays.isChecked,
                            disableEnableThursdays
                        )

                        "Friday" -> updateToggleText(
                            disableEnableFridays.isChecked,
                            disableEnableFridays
                        )

                        "Saturday" -> updateToggleText(
                            disableEnableSaturdays.isChecked,
                            disableEnableSaturdays
                        )

                        "Sunday" -> updateToggleText(
                            disableEnableSundays.isChecked,
                            disableEnableSundays
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                // Handle failure to fetch availability data
                Log.e("AvailabilityActivity", "Error fetching availability data", e)
            }
    }


    private fun updateUIToggleButtons(day: String, statusList: List<AvailabilityStatus>?) {
        // Find the corresponding buttons for the day
        val buttons = when (day) {
            "Monday" -> listOf(mondaysM, mondaysE, mondaysA, disableEnableMondays)
            "Tuesday" -> listOf(tuesdaysM, tuesdaysE,tuesdaysA, disableEnableTuesdays)
            "Wednesday" -> listOf(wednesdaysM, wednesdaysE, wednesdaysA, disableEnableWednesdays)
            "Thursday" -> listOf(thursdaysM, thursdaysE, thursdaysA, disableEnableThursdays)
            "Friday" -> listOf(fridaysM, fridaysE, fridaysA, disableEnableFridays)
            "Saturday" -> listOf(saturdaysM, saturdaysE, saturdaysA, disableEnableSaturdays)
            "Sunday" -> listOf(sundaysM, sundaysE, sundaysA, disableEnableSundays)
            else -> return // Handle unexpected day
        }

        // Enable the buttons if in editing mode
        buttons.forEach { it.isEnabled = true }

        // Set checked state for the buttons based on the fetched availability status and editing mode
        statusList?.forEach { status ->
            when (status) {
                AvailabilityStatus.Morning -> if (buttons.size > 0) buttons[0].isChecked = true
                AvailabilityStatus.Evening -> if (buttons.size > 1) buttons[1].isChecked = true
                AvailabilityStatus.`All-Day` -> if (buttons.size > 2) buttons[2].isChecked = true
                // Ensure there are enough buttons before trying to access the fourth one
                AvailabilityStatus.Unavailable -> if (buttons.size > 3) buttons[3].isChecked = false
            }
        }


        // Disable the buttons if not in editing mode
        buttons.forEach { it.isEnabled = isEditing }
    }

    private fun updateUser(availabilityMap: MutableMap<String, MutableList<AvailabilityStatus>>) {
        // Query the availability collection for documents with the specified UID
        db.collection("availability")
            .whereEqualTo("UID", GlobalUserData.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // No matching document found for the provided UID
                    Log.d("EditAccountActivity", "No matching account found. Creating new record.")
                    // Create a new record with the provided UID and availability data
                    val newAvailability = hashMapOf(
                        "UID" to GlobalUserData.uid,
                        "BID" to GlobalUserData.bid,
                        "name" to GlobalUserData.name,
                        "availability" to availabilityMap
                    )
                    db.collection("availability")
                        .add(newAvailability)
                        .addOnSuccessListener { documentReference ->
                            Log.d(
                                "EditAccountActivity",
                                "New record created with ID: ${documentReference.id}"
                            )
                            // You may perform additional actions here if needed
                        }
                        .addOnFailureListener { e ->
                            Log.e("EditAccountActivity", "Error creating new record", e)
                        }
                    return@addOnSuccessListener
                }

                // If at least one document is found, proceed with updating the existing record
                val account = documents.first()
                account.reference.update("availability", availabilityMap)
                    .addOnSuccessListener {
                        Log.d("EditAccountActivity", "Account updated successfully")
                        // You may perform additional actions here if needed
                    }
                    .addOnFailureListener { e ->
                        Log.e("EditAccountActivity", "Error updating account", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("EditAccountActivity", "Error fetching account", e)
            }
    }


}
