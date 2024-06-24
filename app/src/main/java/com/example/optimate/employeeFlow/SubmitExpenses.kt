package com.example.optimate.employeeFlow
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.optimate.R
import com.example.optimate.businessOwner.XmlTopBar
import com.example.optimate.employeeFlow.PayStub
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.example.optimate.loginAndRegister.GlobalUserData
import com.example.optimate.loginAndRegister.addRevenueOrExpenseToDB
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.text.SimpleDateFormat
import java.util.*

class SubmitExpenses : AppCompatActivity() {

    private lateinit var expenseAmountEditText: TextInputEditText
    private lateinit var outlinedExpenseAmountLayout: TextInputLayout
    private lateinit var receiptPhotoEditText: TextInputEditText
    private lateinit var outlinedReceiptPhotoLayout: TextInputLayout
    private lateinit var reasonFieldEditText: TextInputEditText
    private lateinit var outlinedReasonFieldLayout: TextInputLayout
    private lateinit var outlinedExpenseDateLayout: TextInputLayout
    private lateinit var expenseDateEditText: TextInputEditText
    private val db = Firebase.firestore

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            selectedImageUri = data?.data
            selectedImageUri?.let {
                val imageName = getImageName(it)
                receiptPhotoEditText.setText(imageName)
            }
        }
    }

    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit_expenses)

        expenseAmountEditText = findViewById(R.id.expenseAmount)
        outlinedExpenseAmountLayout = findViewById(R.id.outlinedExpenseAmount)
        receiptPhotoEditText = findViewById(R.id.receiptPhoto)
        outlinedReceiptPhotoLayout = findViewById(R.id.outlinedReceiptPhoto)
        reasonFieldEditText = findViewById(R.id.reasonField)
        outlinedReasonFieldLayout = findViewById(R.id.outlinedReasonField)
        outlinedExpenseDateLayout = findViewById(R.id.outlinedExpenseDate)
        expenseDateEditText = findViewById(R.id.expenseDate)

        val topBar: XmlTopBar = findViewById(R.id.topBar)
        topBar.setTitle("Submit An Expense")

        val viewExpensesBtn = findViewById<MaterialButton>(R.id.viewExpensesBtn)
        viewExpensesBtn.setOnClickListener {
            startActivity(Intent(this, ViewExpenses::class.java))
        }

        val sendBtn = findViewById<MaterialButton>(R.id.sendExpenseReq)
        sendBtn.setOnClickListener {
            val amountStr = expenseAmountEditText.text.toString()
            val description = reasonFieldEditText.text.toString()
            val date = expenseDateEditText.text.toString()
            val amountValue = amountStr.toDoubleOrNull()

            if (date.isEmpty() || amountValue == null || description.isEmpty() || selectedImageUri == null) {
                Toast.makeText(this, "Please fill out the fields", Toast.LENGTH_SHORT).show()
            } else if (amountValue < 0.0) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            } else {
                // Upload image to Firebase Storage
                val imageName = receiptPhotoEditText.text.toString()
                val selectedImageUri = selectedImageUri
                if (selectedImageUri != null) {
                    val storageRef = Firebase.storage.reference.child("images/$imageName")
                    val uploadTask = storageRef.putFile(selectedImageUri)

                    uploadTask.addOnSuccessListener { taskSnapshot ->
                        // Image uploaded successfully, get download URL
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            // URL of the uploaded image
                            val imageUrl = uri.toString()

                            // Call function to save expense request to Firestore
                            addRevenueOrExpenseToDB("expense", date, amountValue, description, false)
                            saveExpensesRequestToFirestore(date, description, amountStr, imageUrl)

                            Toast.makeText(this, "Your request has been Sent for approval", Toast.LENGTH_SHORT).show()
                            Handler(Looper.getMainLooper()).postDelayed({
                                finish()
                                startActivity(Intent(this, DynamicLandingActivity::class.java))
                            }, 1000) // 1000 milliseconds delay (1 second)
                        }.addOnFailureListener { e ->
                            // Failed to get URL
                            Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener { e ->
                        // Failed to upload image
                        Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val calendar = Calendar.getInstance()

        // DatePicker dialog
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                calendar.set(Calendar.HOUR_OF_DAY, 12)
                updateLabel(calendar)
            }

        // Click listener for the end icon
        outlinedExpenseDateLayout.setEndIconOnClickListener {
            DatePickerDialog(
                this@SubmitExpenses, dateSetListener, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()

        }
        // Set onFocusChangeListeners to handle hints
        expenseAmountEditText.onFocusChangeListener = onFocusChangeListener("$0,000.00")
        reasonFieldEditText.onFocusChangeListener = onFocusChangeListener("Write your comment here")

        // Set click listeners for picking a photo
        receiptPhotoEditText.setOnClickListener {
            // Call the method to pick a photo
            pickPhoto()
        }

        outlinedReceiptPhotoLayout.setEndIconOnClickListener {
            // Call the method to pick a photo
            pickPhoto()
        }
    }

    private fun updateLabel(calendar: Calendar) {
        val sdf = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        expenseDateEditText.setText(sdf.format(calendar.time))
    }

    private fun onFocusChangeListener(hint: String): View.OnFocusChangeListener {
        return View.OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                (view as? TextInputEditText)?.hint = hint
            } else {
                (view as? TextInputEditText)?.hint = ""
            }
        }
    }

    private fun pickPhoto() {
        val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickMediaLauncher.launch(pickPhotoIntent)
    }

    private fun getImageName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        var name = ""
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    name = cursor.getString(displayNameIndex)
                    // Check if the name length exceeds the limit
                    val maxLength = 32 // Adjust the maximum length as needed
                    if (name.length > maxLength) {
                        // Truncate the name
                        name = name.substring(0, maxLength) + "..."
                    }
                }
            }
        }
        return name
    }

    private fun saveExpensesRequestToFirestore(datetoDb: String, reason: String, amount: String, receiptPhoto: String ) {
        val sdf = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
        val date = sdf.parse(datetoDb)
        val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)

        val submitExpense = hashMapOf(
            "dateOfRequest" to Date(),
            "uid" to GlobalUserData.uid,
            "bid" to GlobalUserData.bid,
            "name" to GlobalUserData.name,
            "expenseDate" to formattedDate,
            "amount" to amount,
            "status" to "pending",
            "receiptPhoto" to receiptPhoto,
            "reason" to reason
        )

        db.collection("expenseRequest")
            .add(submitExpense)
            .addOnSuccessListener { documentReference ->
                Log.d("EditSubmitRequest", "New record created with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("EditAccountActivity", "Error creating new record", e)
                Toast.makeText(this, "Error in Sending", Toast.LENGTH_SHORT).show()
                // Handle the error, for example, show an error message to the user
            }
    }
}
