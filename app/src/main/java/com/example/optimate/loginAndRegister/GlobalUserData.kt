package com.example.optimate.loginAndRegister

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


data class AccountStatus(var date: Date, var status: String)
object GlobalUserData {
    var access: List<String> = emptyList()
    var uid: String = ""
    var bid: String = ""
    var address: String = ""
    var name: String = ""
    var email: String = ""
    var title: String = ""
    var role: String = ""
    var wage: Float = 0F
    var password: String = ""
    var phone: String = ""
    var first_time: Boolean = false
    var profilePic: String = ""
    var deviceToken: String = ""
    lateinit var account_status: AccountStatus
    lateinit var modules: List<String>

}

fun uidToName(uid: String, callback: (String) -> Unit) {
    val db = Firebase.firestore

    db.collection("users")
        .whereEqualTo("UID", uid)
        .get()
        .addOnSuccessListener { querySnapshot ->
            querySnapshot.documents.forEach { documentSnapshot ->
                val user = documentSnapshot.data
                if (user != null) {
                    val name = user["name"] as String
                    callback(name)
                }
            }
        }
        .addOnFailureListener { e ->
            Log.w("payRequestsScreen", "Error getting user name", e)
            callback("") // Handle the failure case by providing an empty name
        }
}


fun milliSecondsToHours(milliSeconds: Long): Double {
    //in 2 decimal places
    return java.lang.String.format("%.2f", milliSeconds / 3600000.0).toDouble()
}

fun getWage(uid: String, callback: (Double) -> Unit) {
    val db = Firebase.firestore

    db.collection("users")
        .whereEqualTo("UID", uid)
        .get()
        .addOnSuccessListener { querySnapshot ->
            querySnapshot.documents.forEach { documentSnapshot ->
                val user = documentSnapshot.data
                if (user != null) {
                    val wage = user["wage"] as Double
                    callback(wage)
                }
            }
        }
        .addOnFailureListener { e ->
            Log.w("ExpensesRequestsScreen", "Error getting user name", e)
        }
}

fun addRevenueOrExpenseToDB(type: String, dateStr: String, amount: Double, description: String, approved: Boolean) {
    val db = Firebase.firestore
    val bid = GlobalUserData.bid

    // Convert the String date to a Date object
    val sdf = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }
    val date = sdf.parse(dateStr)

    val entry = hashMapOf(
        "UID" to GlobalUserData.uid,
        "Date" to date,
        "Amount" to amount,
        "Description" to description,
        "Approval" to approved,
        "Name" to GlobalUserData.name,
        "Uploaded Date" to Timestamp(Date())
    )

    // Reference to the document in the finances collection
    val docRef = db.collection("finances").document(bid)

    // Check if the BID exists in the finances collection
    docRef.get().addOnSuccessListener { document ->
        if (document.exists()) {
            // If the document exists, append the new entry to the appropriate array
            val fieldName = if (type == "Revenue") "Revenues" else "Expenses"
            docRef.update(fieldName, com.google.firebase.firestore.FieldValue.arrayUnion(entry))
                .addOnSuccessListener {
                    Log.d("AddRevenueOrExpenseActivity", "$type added successfully")
                }
                .addOnFailureListener { e ->
                    Log.w("AddRevenueOrExpenseActivity", "Error adding $type", e)
                }
        } else {
            // If the document does not exist, create a new document with the BID and initialize the Revenues and Expenses arrays
            val initData = hashMapOf(
                "BID" to bid,
                "Revenues" to if (type == "Revenue") listOf(entry) else emptyList<Any>(),
                "Expenses" to if (type == "Expense") listOf(entry) else emptyList<Any>()
            )
            docRef.set(initData)
                .addOnSuccessListener {
                    Log.d("AddRevenueOrExpenseActivity", "New finances document with $type added successfully")
                }
                .addOnFailureListener { e ->
                    Log.w("AddRevenueOrExpenseActivity", "Error creating new finances document", e)
                }
        }
    }.addOnFailureListener { e ->
        Log.w("AddRevenueOrExpenseActivity", "Error checking document existence", e)
    }
}

fun getBusinessNameAndAddress(bid: String, callback: (String, String) -> Unit) {
    val db = Firebase.firestore

    db.collection("users")
        .whereEqualTo("BID", bid)
        .get()
        .addOnSuccessListener { querySnapshot ->
            //find the "role" field is "businessOwner", then its "name" field is the business name
            querySnapshot.documents.forEach { documentSnapshot ->
                val user = documentSnapshot.data
                if (user != null) {
                    val role = user["role"] as String
                    if (role == "businessOwner") {
                        val name = user["name"] as String
                        val address = user["address"] as String
                        callback(name, address)
                    }
                }
            }
        }
        .addOnFailureListener { e ->
            Log.w("ExpensesRequestsScreen", "Error getting business name", e)
        }
}

val biWeeklyDateRanges2024 = listOf(
    listOf("20240101", "20240115"), listOf("20240116", "20240131"),
    listOf("20240201", "20240215"), listOf("20240216", "20240229"),
    listOf("20240301", "20240315"), listOf("20240316", "20240331"),
    listOf("20240401", "20240415"), listOf("20240416", "20240430"),
    listOf("20240501", "20240515"), listOf("20240516", "20240531"),
    listOf("20240601", "20240615"), listOf("20240616", "20240630"),
    listOf("20240701", "20240715"), listOf("20240716", "20240731"),
    listOf("20240801", "20240815"), listOf("20240816", "20240831"),
    listOf("20240901", "20240915"), listOf("20240916", "20240930"),
    listOf("20241001", "20241015"), listOf("20241016", "20241031"),
    listOf("20241101", "20241115"), listOf("20241116", "20241130"),
    listOf("20241201", "20241215"), listOf("20241216", "20241231")
)